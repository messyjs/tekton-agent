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

object CoinbaseProvider : DataProvider {
    override val name = "Coinbase"
    override val category = "Crypto"
    override val description = "US crypto exchange. Free, no key."
    override val requiresApiKey = false
    override val apiKeyHint = "No key needed (public candles API)"
    override val vpnRequired = false
    override val vpnWarning = ""
    override val tickerFormat = "BTC-USD, ETH-USD, SOL-USD"

    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    override suspend fun fetchClose(symbol: String, years: Int): DataProvider.PriceData = withContext(Dispatchers.IO) {
        val pair = if (symbol.contains("-")) symbol.uppercase() else "${symbol.uppercase()}-USD"
        val endTs = System.currentTimeMillis() / 1000L
        val startTs = endTs - (years.toLong() * 365L * 24L * 3600L)
        val url = "https://api.exchange.coinbase.com/products/$pair/candles?granularity=86400&start=$startTs&end=$endTs"
        val request = Request.Builder().url(url).header("User-Agent", "MarkovHedgeFund/1.0").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw IOException("Empty response from Coinbase")
        if (!response.isSuccessful) throw IOException("Coinbase returned ${response.code}: ${body.take(200)}")

        val arr = JSONArray(body)
        val dates = mutableListOf<String>(); val closes = mutableListOf<Double>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US); sdf.timeZone = TimeZone.getTimeZone("UTC")

        // Coinbase returns candles in reverse chronological order
        for (i in arr.length() - 1 downTo 0) {
            val candle = arr.getJSONArray(i)
            val ts = candle.getLong(0)
            val close = candle.getDouble(4) // close is index 4
            dates.add(sdf.format(ts * 1000L))
            closes.add(close)
        }
        if (closes.isEmpty()) throw IOException("No data from Coinbase for $pair. Use format like BTC-USD.")
        DataProvider.PriceData("$pair (Coinbase)", dates, closes.toDoubleArray(), dates.first(), dates.last())
    }
}