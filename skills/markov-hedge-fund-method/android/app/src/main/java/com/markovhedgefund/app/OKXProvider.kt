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

object OKXProvider : DataProvider {
    override val name = "OKX"
    override val category = "Crypto"
    override val description = "Global crypto exchange. Free, no key. VPN for China."
    override val requiresApiKey = false
    override val apiKeyHint = "No key needed (public market API)"
    override val vpnRequired = true
    override val vpnWarning = "⚠ OKX blocks mainland China IPs. VPN may be needed."
    override val tickerFormat = "BTC-USDT, ETH-USDT, SOL-USDT"

    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    override suspend fun fetchClose(symbol: String, years: Int): DataProvider.PriceData = withContext(Dispatchers.IO) {
        // OKX uses format like BTC-USDT-SWAP or BTC-USDT
        val instId = if (symbol.contains("-")) symbol.uppercase() else "${symbol.uppercase()}-USDT"

        val endTs = System.currentTimeMillis()
        val startTs = endTs - (years.toLong() * 365L * 24L * 3600L * 1000L)
        val dates = mutableListOf<String>(); val closes = mutableListOf<Double>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US); sdf.timeZone = TimeZone.getTimeZone("UTC")

        // OKX candles: max 100 per call
        var after = ""
        var attempts = 0
        while (attempts < 50) {
            var url = "https://www.okx.com/api/v5/market/candles?instId=$instId&bar=1D&limit=100"
            if (after.isNotEmpty()) url += "&after=$after"
            val request = Request.Builder().url(url).header("User-Agent", "MarkovHedgeFund/1.0").header("Accept", "application/json").build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw IOException("Empty response from OKX")
            if (!response.isSuccessful) throw IOException("OKX returned ${response.code}: ${body.take(200)}")
            val json = org.json.JSONObject(body)
            val code = json.optString("code", "")
            if (code != "0" && code != "") {
                val msg = json.optString("msg", "")
                throw IOException("OKX error ($code): $msg. Use format like BTC-USDT")
            }
            val data = json.getJSONArray("data")
            if (data.length() == 0) break
            for (i in 0 until data.length()) {
                val candle = data.getJSONArray(i)
                val ts = candle.getLong(0)
                val close = candle.getString(4).toDoubleOrNull() ?: continue
                if (ts >= startTs && ts <= endTs) {
                    val dateStr = sdf.format(ts)
                    if (dateStr !in dates) { dates.add(0, dateStr); closes.add(0, close) }
                }
                after = candle.getString(0) // pagination: use earliest timestamp as "after"
            }
            attempts++
            if (data.length() < 100) break
            // Small delay to avoid rate limits
            Thread.sleep(100)
        }
        if (closes.isEmpty()) throw IOException("No data from OKX for $instId. Use format like BTC-USDT")
        DataProvider.PriceData("$instId (OKX)", dates, closes.toDoubleArray(), dates.first(), dates.last())
    }
}