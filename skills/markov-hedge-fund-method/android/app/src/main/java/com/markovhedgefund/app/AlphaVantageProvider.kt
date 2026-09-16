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

object AlphaVantageProvider : DataProvider {
    override val name = "Alpha Vantage"
    override val category = "Stocks & ETFs"
    override val description = "Stocks, forex, crypto. Free: 25 req/day."
    override val requiresApiKey = true
    override val apiKeyHint = "Free key at www.alphavantage.co/support/#api-key"
    override val vpnRequired = false
    override val vpnWarning = ""
    override val tickerFormat = "SPY, AAPL, MSFT"

    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    override suspend fun fetchClose(symbol: String, years: Int): DataProvider.PriceData = withContext(Dispatchers.IO) {
        val apiKey = DataSourceConfig.getApiKey("alphavantage")
            ?: throw IOException("Alpha Vantage requires an API key. Add it in Settings.")
        val url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=${java.net.URLEncoder.encode(symbol, "UTF-8")}&outputsize=full&apikey=$apiKey"
        val response = client.newCall(Request.Builder().url(url).header("User-Agent", "MarkovHedgeFund/1.0").build()).execute()
        val body = response.body?.string() ?: throw IOException("Empty response from Alpha Vantage")
        val json = JSONObject(body)
        if (json.has("Error Message")) throw IOException("Alpha Vantage: ${json.getString("Error Message")}")
        if (json.has("Note")) throw IOException("Alpha Vantage rate limit: ${json.getString("Note")}")
        if (json.has("Information")) throw IOException("Alpha Vantage: ${json.getString("Information")}")
        val tsKey = json.keys().asSequence().find { it.contains("Time Series") } ?: throw IOException("Unexpected Alpha Vantage response")
        val ts = json.getJSONObject(tsKey)
        val cal = java.util.Calendar.getInstance(); cal.add(java.util.Calendar.YEAR, -years)
        val cutoff = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        val dates = mutableListOf<String>(); val closes = mutableListOf<Double>()
        ts.keys().asSequence().filter { it >= cutoff }.sorted().forEach { key ->
            val day = ts.getJSONObject(key); val v = day.optDouble("4. close", Double.NaN)
            if (!v.isNaN()) { dates.add(key); closes.add(v) }
        }
        if (closes.isEmpty()) throw IOException("No data from Alpha Vantage for $symbol")
        DataProvider.PriceData(symbol, dates, closes.toDoubleArray(), dates.first(), dates.last())
    }
}