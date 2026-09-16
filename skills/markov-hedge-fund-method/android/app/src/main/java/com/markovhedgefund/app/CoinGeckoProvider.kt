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

object CoinGeckoProvider : DataProvider {
    override val name = "CoinGecko"
    override val category = "Crypto"
    override val description = "Crypto prices. Free tier, no key needed."
    override val requiresApiKey = false
    override val apiKeyHint = "Free (rate-limited). Optional Pro key at coingecko.com"
    override val vpnRequired = false
    override val vpnWarning = ""
    override val tickerFormat = "BTC, ETH, SOL, DOGE"

    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private val SYMBOL_MAP = mapOf(
        "BTC" to "bitcoin", "BTC-USD" to "bitcoin", "ETH" to "ethereum", "ETH-USD" to "ethereum",
        "SOL" to "solana", "ADA" to "cardano", "DOGE" to "dogecoin", "XRP" to "ripple",
        "DOT" to "polkadot", "AVAX" to "avalanche-2", "MATIC" to "matic-network",
        "LINK" to "chainlink", "UNI" to "uniswap", "AAVE" to "aave",
        "LTC" to "litecoin", "BCH" to "bitcoin-cash", "XLM" to "stellar"
    )

    override suspend fun fetchClose(symbol: String, years: Int): DataProvider.PriceData = withContext(Dispatchers.IO) {
        val coinId = SYMBOL_MAP[symbol.uppercase()] ?: symbol.lowercase().replace("-usd", "").replace("-btc", "")
        val days = years * 365
        val url = "https://api.coingecko.com/api/v3/coins/$coinId/market_chart?vs_currency=usd&days=$days&interval=daily"
        val request = Request.Builder().url(url).header("User-Agent", "MarkovHedgeFund/1.0").header("Accept", "application/json").build()
        val key = DataSourceConfig.getApiKey("coingecko")
        val finalRequest = if (key != null) Request.Builder().url("$url${if ("?" in url) "&" else "?"}x_cg_pro_api_key=$key").header("User-Agent", "MarkovHedgeFund/1.0").build() else request
        val response = client.newCall(finalRequest).execute()
        val body = response.body?.string() ?: throw IOException("Empty response from CoinGecko")
        if (!response.isSuccessful) throw IOException("CoinGecko returned ${response.code}: ${body.take(200)}")
        val json = JSONArray(body)
        val prices = json.getJSONArray(0)
        val dates = mutableListOf<String>(); val closes = mutableListOf<Double>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US); sdf.timeZone = TimeZone.getTimeZone("UTC")
        for (i in 0 until prices.length()) {
            val point = prices.getJSONArray(i); val price = point.optDouble(1, Double.NaN)
            if (!price.isNaN() && price > 0) { dates.add(sdf.format(point.getLong(0))); closes.add(price) }
        }
        if (closes.isEmpty()) throw IOException("No data from CoinGecko for '$symbol'. Try the coin name (e.g. bitcoin) or common symbols.")
        DataProvider.PriceData("${symbol.uppercase()} (CoinGecko)", dates, closes.toDoubleArray(), dates.first(), dates.last())
    }
}