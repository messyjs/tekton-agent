package com.markovhedgefund.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object PolygonProvider : DataProvider {
    override val name = "Polygon.io"
    override val category = "Stocks & ETFs"
    override val description = "Stocks, crypto, forex. Free: 5 calls/min, 2yr history."
    override val requiresApiKey = true
    override val apiKeyHint = "Free key at polygon.io"
    override val vpnRequired = false
    override val vpnWarning = ""
    override val tickerFormat = "SPY, AAPL, X:BTCUSD"

    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    override suspend fun fetchClose(symbol: String, years: Int): DataProvider.PriceData = withContext(Dispatchers.IO) {
        val apiKey = DataSourceConfig.getApiKey("polygon")
            ?: throw IOException("Polygon.io requires an API key. Add it in Settings.")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = java.util.Calendar.getInstance()
        val endDate = sdf.format(cal.time)
        cal.add(java.util.Calendar.YEAR, -years)
        val startDate = sdf.format(cal.time)
        val url = "https://api.polygon.io/v2/aggs/ticker/${java.net.URLEncoder.encode(symbol.uppercase(), "UTF-8")}/range/1/day/$startDate/$endDate?adjusted=true&sort=asc&limit=50000&apiKey=$apiKey"
        val response = client.newCall(Request.Builder().url(url).header("User-Agent", "MarkovHedgeFund/1.0").build()).execute()
        val body = response.body?.string() ?: throw IOException("Empty response from Polygon.io")
        val json = JSONObject(body)
        if (json.has("error")) throw IOException("Polygon.io: ${json.getString("error")}")
        val results = json.optJSONArray("results") ?: throw IOException("No results from Polygon.io for $symbol")
        val dates = mutableListOf<String>(); val closes = mutableListOf<Double>()
        for (i in 0 until results.length()) {
            val day = results.getJSONObject(i)
            val closeVal = day.optDouble("c", Double.NaN); val ts = day.optLong("t", 0L)
            if (!closeVal.isNaN() && ts > 0L) { dates.add(sdf.format(ts)); closes.add(closeVal) }
        }
        if (closes.isEmpty()) throw IOException("No valid data from Polygon.io for $symbol")
        DataProvider.PriceData(symbol, dates, closes.toDoubleArray(), dates.first(), dates.last())
    }
}