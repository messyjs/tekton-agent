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

object TwelveDataProvider : DataProvider {
    override val name = "Twelve Data"
    override val category = "Stocks & ETFs"
    override val description = "Stocks, crypto, forex. Free: 800/day, 8/min."
    override val requiresApiKey = true
    override val apiKeyHint = "Free key at twelvedata.com/pricing"
    override val vpnRequired = false
    override val vpnWarning = ""
    override val tickerFormat = "SPY, AAPL, BTC/USD"

    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    override suspend fun fetchClose(symbol: String, years: Int): DataProvider.PriceData = withContext(Dispatchers.IO) {
        val apiKey = DataSourceConfig.getApiKey("twelvedata")
            ?: throw IOException("Twelve Data requires an API key. Add it in Settings.")

        // Auto-detect interval: if crypto symbol, use 1day on crypto market
        var interval = "1day"
        var market = "stocks"
        var sym = symbol.uppercase()
        if (sym.contains("/") || sym.contains("-") || sym.endsWith("USD") || sym.endsWith("USDT") ||
            sym in setOf("BTC", "ETH", "SOL", "DOGE", "XRP", "ADA", "DOT", "AVAX", "MATIC", "LINK")) {
            market = ""
            if (!sym.contains("/") && !sym.contains("-")) sym = "${sym}/USD"
        }

        val url = "https://api.twelvedata.com/time_series?symbol=${java.net.URLEncoder.encode(sym, "UTF-8")}" +
            "&interval=$interval&outputsize=${years * 260}&apikey=$apiKey"
        val request = Request.Builder().url(url).header("User-Agent", "MarkovHedgeFund/1.0").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw IOException("Empty response from Twelve Data")
        val json = JSONObject(body)

        if (json.has("code") && json.optString("code") == "429") {
            throw IOException("Twelve Data rate limit: ${json.optString("message", "try again later")}")
        }
        if (json.has("status") && json.optString("status") == "error") {
            throw IOException("Twelve Data error: ${json.optString("message", "unknown")}")
        }

        val values = json.optJSONArray("values") ?: throw IOException("No data from Twelve Data for $symbol")
        val dates = mutableListOf<String>(); val closes = mutableListOf<Double>()
        // Twelve Data returns newest first
        for (i in values.length() - 1 downTo 0) {
            val day = values.getJSONObject(i)
            val dateStr = day.optString("datetime", "")?.substring(0, 10) ?: continue
            val closeVal = day.optDouble("close", Double.NaN)
            if (!closeVal.isNaN()) { dates.add(dateStr); closes.add(closeVal) }
        }
        if (closes.isEmpty()) throw IOException("No data from Twelve Data for $symbol")
        DataProvider.PriceData("$symbol (Twelve Data)", dates, closes.toDoubleArray(), dates.first(), dates.last())
    }
}