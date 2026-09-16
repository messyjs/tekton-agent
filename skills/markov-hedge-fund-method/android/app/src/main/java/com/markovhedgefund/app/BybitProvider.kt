package com.markovhedgefund.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object BybitProvider : DataProvider {
    override val name = "Bybit"
    override val category = "Crypto"
    override val description = "Major crypto exchange. Free, no key. VPN needed."
    override val requiresApiKey = false
    override val apiKeyHint = "No key needed (public market API)"
    override val vpnRequired = true
    override val vpnWarning = "⚠ Bybit blocks US IPs. Enable VPN before using."
    override val tickerFormat = "BTCUSDT, ETHUSDT, SOLUSDT"

    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    override suspend fun fetchClose(symbol: String, years: Int): DataProvider.PriceData = withContext(Dispatchers.IO) {
        val pair = if (!symbol.uppercase().endsWith("USDT") && !symbol.uppercase().endsWith("PERP") && !symbol.uppercase().contains("USD"))
            "${symbol.uppercase()}USDT" else symbol.uppercase()

        val endTs = System.currentTimeMillis()
        val startTs = endTs - (years.toLong() * 365L * 24L * 3600L * 1000L)
        val dates = mutableListOf<String>(); val closes = mutableListOf<Double>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US); sdf.timeZone = TimeZone.getTimeZone("UTC")

        // Bybit v5 market kline: max 200 per call, paginate
        var cursor = startTs
        var attempts = 0
        while (cursor < endTs && attempts < 60) {
            val url = "https://api.bybit.com/v5/market/kline?category=spot&symbol=$pair&interval=D&start=${cursor}&limit=200"
            val request = Request.Builder().url(url).header("User-Agent", "MarkovHedgeFund/1.0").build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw IOException("Empty response from Bybit")
            if (!response.isSuccessful) throw IOException("Bybit returned ${response.code}: ${body.take(200)}")
            val json = JSONObject(body)
            val retCode = json.optInt("retCode", -1)
            if (retCode != 0) {
                val retMsg = json.optString("retMsg", "unknown error")
                if (retMsg.contains("restricted", ignoreCase = true) || retMsg.contains("forbidden", ignoreCase = true))
                    throw IOException("Bybit is geo-blocked in your region. Enable VPN. ($retMsg)")
                throw IOException("Bybit error: $retMsg")
            }
            val list = json.getJSONObject("result").getJSONArray("list")
            if (list.length() == 0) break
            // Bybit returns most recent first, so reverse
            for (i in list.length() - 1 downTo 0) {
                val kline = list.getJSONArray(i)
                val ts = kline.getLong(0)
                val close = kline.getString(4).toDoubleOrNull() ?: continue
                if (ts in startTs..endTs && !dates.contains(sdf.format(ts))) {
                    dates.add(sdf.format(ts)); closes.add(close)
                }
                cursor = ts + 86400000L
            }
            attempts++
            if (list.length() < 200) break
        }
        if (closes.isEmpty()) throw IOException("No data from Bybit for $pair. Use format like BTCUSDT.")
        DataProvider.PriceData("$pair (Bybit)", dates, closes.toDoubleArray(), dates.first(), dates.last())
    }
}