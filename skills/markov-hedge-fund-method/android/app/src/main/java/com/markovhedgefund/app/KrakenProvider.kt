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

object KrakenProvider : DataProvider {
    override val name = "Kraken"
    override val category = "Crypto"
    override val description = "US-friendly crypto exchange. Free, no key."
    override val requiresApiKey = false
    override val apiKeyHint = "No key needed (public OHLC API)"
    override val vpnRequired = false
    override val vpnWarning = ""
    override val tickerFormat = "XXBTZUSD, XETHZUSD, SOLUSD"

    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private val SYMBOL_MAP = mapOf(
        "BTC" to "XXBTZUSD", "ETH" to "XETHZUSD", "SOL" to "SOLUSD", "ADA" to "ADAUSD",
        "DOGE" to "DOGEXXUSD", "XRP" to "XXRPZUSD", "DOT" to "DOTUSD", "AVAX" to "AVAXUSD",
        "LTC" to "XLTCZUSD", "LINK" to "LINKUSD", "UNI" to "UNIUSD", "MATIC" to "MATICUSD",
        "BTC-USD" to "XXBTZUSD", "ETH-USD" to "XETHZUSD", "BTCUSD" to "XXBTZUSD"
    )

    override suspend fun fetchClose(symbol: String, years: Int): DataProvider.PriceData = withContext(Dispatchers.IO) {
        val pair = SYMBOL_MAP[symbol.uppercase()] ?: symbol.uppercase()
        // Kraken OHLC: interval in minutes. 1440 = 1 day. Returns up to 720 candles per call.
        val url = "https://api.kraken.com/0/public/OHLC?pair=$pair&interval=1440"
        val request = Request.Builder().url(url).header("User-Agent", "MarkovHedgeFund/1.0").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw IOException("Empty response from Kraken")
        val json = JSONObject(body)
        val errors = json.optJSONArray("error")
        if (errors != null && errors.length() > 0) {
            val errMsg = errors.getString(0)
            throw IOException("Kraken error: $errMsg. Try format like XXBTZUSD (BTC), XETHZUSD (ETH)")
        }
        val result = json.getJSONObject("result")
        // The result key is the pair name
        val pairKey = result.keys().asSequence().find { it != "last" } ?: throw IOException("Unexpected Kraken response")
        val ohlcArray = result.getJSONArray(pairKey)

        val cal = java.util.Calendar.getInstance(); cal.add(java.util.Calendar.YEAR, -years)
        val cutoffTs = cal.timeInMillis / 1000L

        val dates = mutableListOf<String>(); val closes = mutableListOf<Double>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US); sdf.timeZone = TimeZone.getTimeZone("UTC")

        for (i in 0 until ohlcArray.length()) {
            val candle = ohlcArray.getJSONArray(i)
            val ts = candle.getLong(0)
            if (ts < cutoffTs) continue
            val close = candle.getString(4).toDoubleOrNull() ?: continue
            dates.add(sdf.format(ts * 1000L))
            closes.add(close)
        }
        if (closes.isEmpty()) throw IOException("No data from Kraken for $pair. Use format like XXBTZUSD.")
        DataProvider.PriceData("$pair (Kraken)", dates, closes.toDoubleArray(), dates.first(), dates.last())
    }
}