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

object YahooFinanceProvider : DataProvider {
    override val name = "Yahoo Finance"
    override val category = "Stocks & ETFs"
    override val description = "US stocks, ETFs, some crypto. Free, no key."
    override val requiresApiKey = false
    override val apiKeyHint = "No key needed (free, rate-limited)"
    override val vpnRequired = false
    override val vpnWarning = ""
    override val tickerFormat = "SPY, AAPL, BTC-USD"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun fetchClose(symbol: String, years: Int): DataProvider.PriceData = withContext(Dispatchers.IO) {
        val endTs = System.currentTimeMillis() / 1000
        val startTs = endTs - (years.toLong() * 365L * 24L * 3600L)
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/" +
            "${java.net.URLEncoder.encode(symbol, "UTF-8")}?period1=$startTs&period2=$endTs&interval=1d"
        val request = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            .header("Accept", "application/json").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw IOException("Empty response from Yahoo Finance for $symbol")
        if (!response.isSuccessful) throw IOException("Yahoo Finance returned ${response.code} for $symbol")
        val json = JSONObject(body)
        val result = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
        val timestamps = result.getJSONArray("timestamp")
        val closesArray = result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("close")
        val dates = mutableListOf<String>()
        val closes = mutableListOf<Double>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US); sdf.timeZone = TimeZone.getTimeZone("UTC")
        for (i in 0 until timestamps.length()) {
            val v = closesArray.optDouble(i, Double.NaN)
            if (!v.isNaN()) { dates.add(sdf.format(timestamps.getLong(i) * 1000L)); closes.add(v) }
        }
        if (closes.isEmpty()) throw IOException("No data from Yahoo Finance for $symbol")
        DataProvider.PriceData(symbol, dates, closes.toDoubleArray(), dates.first(), dates.last())
    }
}