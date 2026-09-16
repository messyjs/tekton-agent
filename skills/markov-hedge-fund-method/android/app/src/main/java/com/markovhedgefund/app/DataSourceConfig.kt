package com.markovhedgefund.app

import android.content.Context
import android.content.SharedPreferences

object DataSourceConfig {

    private const val PREFS_NAME = "markov_hedge_fund_prefs"
    private const val KEY_ACTIVE_SOURCE = "active_data_source"
    private const val KEY_PREFIX = "api_key_"

    // All providers, grouped by category for the UI
    val providers: List<DataProvider> = listOf(
        // Stocks & ETFs
        YahooFinanceProvider,
        AlphaVantageProvider,
        TwelveDataProvider,
        FinnhubProvider,
        PolygonProvider,
        // Crypto
        CoinGeckoProvider,
        BinanceProvider,
        BybitProvider,
        OKXProvider,
        KrakenProvider,
        CoinbaseProvider
    )

    val categories: List<String> = listOf("Stocks & ETFs", "Crypto")

    fun providersByCategory(cat: String): List<DataProvider> = providers.filter { it.category == cat }

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getActiveSourceName(): String {
        return prefs.getString(KEY_ACTIVE_SOURCE, YahooFinanceProvider.name) ?: YahooFinanceProvider.name
    }

    fun setActiveSourceName(name: String) {
        prefs.edit().putString(KEY_ACTIVE_SOURCE, name).apply()
    }

    fun getActiveProvider(): DataProvider {
        val name = getActiveSourceName()
        return providers.find { it.name == name } ?: YahooFinanceProvider
    }

    fun getApiKey(providerId: String): String? {
        return prefs.getString(KEY_PREFIX + providerId, null)
    }

    fun setApiKey(providerId: String, key: String?) {
        if (key.isNullOrBlank()) {
            prefs.edit().remove(KEY_PREFIX + providerId).apply()
        } else {
            prefs.edit().putString(KEY_PREFIX + providerId, key.trim()).apply()
        }
    }

    fun isProviderReady(provider: DataProvider): Boolean {
        if (!provider.requiresApiKey) return true
        return !getApiKey(getProviderId(provider)).isNullOrBlank()
    }

    fun getProviderId(provider: DataProvider): String = when (provider) {
        is YahooFinanceProvider -> "yahoo"
        is AlphaVantageProvider -> "alphavantage"
        is CoinGeckoProvider -> "coingecko"
        is PolygonProvider -> "polygon"
        is BinanceProvider -> "binance"
        is BybitProvider -> "bybit"
        is OKXProvider -> "okx"
        is KrakenProvider -> "kraken"
        is CoinbaseProvider -> "coinbase"
        is TwelveDataProvider -> "twelvedata"
        is FinnhubProvider -> "finnhub"
        else -> "unknown"
    }
}