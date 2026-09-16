package com.markovhedgefund.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object BinanceProvider : DataProvider {
    override val name = "Binance"
    override val category = "Crypto"
    override val description = "World's largest crypto exchange. Free, no key."
    override val requiresApiKey = false
    override val apiKeyHint = "No key needed (public klines API)"
    override val vpnRequired = true
    override val vpnWarning = "⚠ Binance blocks US IPs. Enable VPN before using."
    override val tickerFormat = "BTCUSDT, ETHUSDT, SOLUSDT"

    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    override suspend fun fetchClose(symbol: String, years: Int): DataProvider.PriceData = withContext(Dispatchers.IO) {
        // Auto-append USDT if no quote currency suffix
        val pair = if (symbol.length <= 6 && !symbol.uppercase().endsWith("USDT") && !symbol.uppercase().endsWith("BUSD") && !symbol.uppercase().endsWith("BTC") && !symbol.uppercase().endsWith("ETH")) {
            "${symbol.uppercase()}USDT"
        } else symbol.uppercase()

        val endTs = System.currentTimeMillis()
        val startTs = endTs - (years.toLong() * 365L * 24L * 3600L * 1000L)
        // Binance klines: 1000 max per call, so we paginate
        val dates = mutableListOf<String>(); val closes = mutableListOf<Double>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US); sdf.timeZone = TimeZone.getTimeZone("UTC")
        var cursor = startTs
        var attempts = 0
        while (cursor < endTs && attempts < 20) {
            val url = "https://api.binance.com/api/v3/klines?symbol=$pair&interval=1d&startTime=$cursor&limit=1000"
            val request = Request.Builder().url(url).header("User-Agent", "MarkovHedgeFund/1.0").build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string()?.take(200) ?: ""
                if (errBody.contains("Forbidden") || errBody.contains("restricted") || errBody.contains("451"))
                    throw IOException("Binance is geo-blocked in your region. Enable VPN. ($errBody)")
                throw IOException("Binance returned ${response.code}: $errBody")
            }
            val body = response.body?.string() ?: throw IOException("Empty response from Binance")
            val arr = JSONArray(body)
            if (arr.length() == 0) break
            for (i in 0 until arr.length()) {
                val kline = arr.getJSONArray(i)
                val ts = kline.getLong(0)
                val close = kline.getString(4).toDoubleOrNull() ?: continue
                if (ts >= startTs && ts <= endTs) {
                    dates.add(sdf.format(ts)); closes.add(close)
                }
                cursor = ts + 86400000L
            }
            attempts++
            if (arr.length() < 1000) break
        }
        if (closes.isEmpty()) throw IOException("No data from Binance for $pair. Use format like BTCUSDT.")
        DataProvider.PriceData("$pair (Binance)", dates, closes.toDoubleArray(), dates.first(), dates.last())
    }
}