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

object FinnhubProvider : DataProvider {
    override val name = "Finnhub"
    override val category = "Stocks & ETFs"
    override val description = "Stocks, forex, crypto. Free: 60 calls/min."
    override val requiresApiKey = true
    override val apiKeyHint = "Free key at finnhub.io/register"
    override val vpnRequired = false
    override val vpnWarning = ""
    override val tickerFormat = "SPY, AAPL, BINANCE:BTCUSDT"

    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    override suspend fun fetchClose(symbol: String, years: Int): DataProvider.PriceData = withContext(Dispatchers.IO) {
        val apiKey = DataSourceConfig.getApiKey("finnhub")
            ?: throw IOException("Finnhub requires an API key. Add it in Settings.")

        val endTs = System.currentTimeMillis() / 1000L
        val startTs = endTs - (years.toLong() * 365L * 24L * 3600L)

        val url = "https://finnhub.io/api/v1/stock/candle?symbol=${java.net.URLEncoder.encode(symbol.uppercase(), "UTF-8")}" +
            "&resolution=D&from=$startTs&to=$endTs&token=$apiKey"
        val request = Request.Builder().url(url).header("User-Agent", "MarkovHedgeFund/1.0").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw IOException("Empty response from Finnhub")
        val json = JSONObject(body)

        val status = json.optString("s", "")
        if (status == "no_data") throw IOException("No data from Finnhub for $symbol. Check the symbol format.")
        if (!response.isSuccessful) throw IOException("Finnhub returned ${response.code}")

        val timestamps = json.optJSONArray("t") ?: throw IOException("No timestamp data from Finnhub")
        val closesArr = json.optJSONArray("c") ?: throw IOException("No close data from Finnhub")

        val dates = mutableListOf<String>(); val closes = mutableListOf<Double>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US); sdf.timeZone = TimeZone.getTimeZone("UTC")

        for (i in 0 until timestamps.length()) {
            val ts = timestamps.getLong(i)
            val close = closesArr.optDouble(i, Double.NaN)
            if (!close.isNaN() && close > 0) {
                dates.add(sdf.format(ts * 1000L))
                closes.add(close)
            }
        }
        if (closes.isEmpty()) throw IOException("No data from Finnhub for $symbol")
        DataProvider.PriceData(symbol, dates, closes.toDoubleArray(), dates.first(), dates.last())
    }
}