package com.markovhedgefund.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private val bgPrimary = Color.parseColor("#0D1117")
    private val bgCard = Color.parseColor("#1C2128")
    private val textPrimary = Color.parseColor("#E6EDF3")
    private val textSecondary = Color.parseColor("#8B949E")
    private val textDim = Color.parseColor("#6E7681")
    private val accentColor = Color.parseColor("#3FDE7E")
    private val vpnWarningColor = Color.parseColor("#E8A838")
    private val mono: Typeface by lazy { Typeface.MONOSPACE }
    private lateinit var keyContainer: LinearLayout
    private lateinit var sourceInfoText: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataSourceConfig.init(this)

        val scrollView = ScrollView(this).apply { setBackgroundColor(bgPrimary) }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 48)
        }

        // Back button + header
        val headerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        headerRow.addView(TextView(this).apply {
            text = "⚙ SETTINGS"
            setTextColor(accentColor); textSize = 20f; typeface = mono; setTypeface(mono, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val doneBtn = Button(this).apply {
            text = "DONE"; setTextColor(bgPrimary); setBackgroundColor(accentColor)
            typeface = mono; setTypeface(mono, Typeface.BOLD); textSize = 12f
            setPadding(16, 4, 16, 4)
            setOnClickListener { finish() }
        }
        headerRow.addView(doneBtn)
        container.addView(headerRow)
        container.addView(TextView(this).apply {
            text = "Data sources, API keys & VPN notices"
            setTextColor(textDim); textSize = 12f; typeface = mono
            setPadding(0, 4, 0, 24)
        })

        // Active source spinner
        container.addView(makeSectionHeader("ACTIVE DATA SOURCE"))
        val sourceSpinner = Spinner(this)
        val providerNames = DataSourceConfig.providers.map { it.name }
        val spinnerAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, providerNames) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.setTextColor(textPrimary); tv.typeface = mono; tv.textSize = 14f
                return tv
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val tv = super.getDropDownView(position, convertView, parent) as TextView
                tv.setTextColor(textPrimary); tv.typeface = mono; tv.textSize = 13f
                tv.setPadding(16, 12, 16, 12)
                return tv
            }
        }
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sourceSpinner.adapter = spinnerAdapter
        sourceSpinner.setSelection(providerNames.indexOf(DataSourceConfig.getActiveSourceName()).coerceAtLeast(0))
        sourceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                DataSourceConfig.setActiveSourceName(providerNames[position])
                refreshSourceInfo(sourceInfoText, DataSourceConfig.providers[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        container.addView(sourceSpinner.apply { setPadding(0, 8, 0, 8) })

        // Active source info (dynamic)
        sourceInfoText = TextView(this).apply {
            setTextColor(textSecondary); textSize = 11f; typeface = mono; setPadding(0, 0, 0, 16)
        }
        container.addView(sourceInfoText)
        refreshSourceInfo(sourceInfoText, DataSourceConfig.getActiveProvider())

        // Provider cards grouped by category
        container.addView(makeSectionHeader("ALL PROVIDERS"))
        for (cat in DataSourceConfig.categories) {
            container.addView(TextView(this).apply {
                text = "── $cat ──"; setTextColor(textSecondary); textSize = 11f; typeface = mono
                setPadding(0, 12, 0, 4)
            })
            for (provider in DataSourceConfig.providersByCategory(cat)) {
                container.addView(makeProviderCard(provider))
            }
        }

        // API keys
        container.addView(makeSectionHeader("API KEYS"))
        container.addView(TextView(this).apply {
            text = "Keys are stored on-device and never sent anywhere except the selected provider's API."
            setTextColor(textDim); textSize = 10f; typeface = mono; setPadding(0, 0, 0, 8)
        })
        keyContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        container.addView(keyContainer)
        refreshKeySections()

        scrollView.addView(container)
        setContentView(scrollView)
    }

    private fun makeSectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title; setTextColor(accentColor); textSize = 11f; typeface = mono; letterSpacing = 0.1f
            setPadding(0, 8, 0, 8)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun makeProviderCard(provider: DataProvider): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 10, 12, 10)
            setBackgroundColor(bgCard)
        }
        val providerId = DataSourceConfig.getProviderId(provider)

        // Name row
        val nameRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        nameRow.addView(TextView(this).apply {
            text = if (provider.requiresApiKey) "🔑 " else if (provider.vpnRequired) "🔒 " else "● "
            setTextColor(if (provider.requiresApiKey) textDim else if (provider.vpnRequired) vpnWarningColor else accentColor)
            textSize = 12f; typeface = mono
        })
        nameRow.addView(TextView(this).apply {
            text = provider.name; setTextColor(textPrimary); textSize = 14f
            typeface = mono; setTypeface(mono, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        card.addView(nameRow)

        // Description
        card.addView(TextView(this).apply {
            text = provider.description; setTextColor(textDim); textSize = 11f; typeface = mono; setPadding(0, 2, 0, 0)
        })

        // Ticker format hint
        card.addView(TextView(this).apply {
            text = "Format: ${provider.tickerFormat}"; setTextColor(textDim); textSize = 10f; typeface = mono; setPadding(0, 1, 0, 0)
        })

        // VPN warning
        if (provider.vpnRequired) {
            card.addView(TextView(this).apply {
                text = provider.vpnWarning; setTextColor(vpnWarningColor); textSize = 11f; typeface = mono
                setTypeface(mono, Typeface.BOLD); setPadding(0, 2, 0, 0)
            })
        }

        // API key status
        if (provider.requiresApiKey) {
            val hasKey = !DataSourceConfig.getApiKey(providerId).isNullOrBlank()
            card.addView(TextView(this).apply {
                text = if (hasKey) "✓ Key configured" else "○ No API key — add below"
                setTextColor(if (hasKey) accentColor else Color.parseColor("#C57F86"))
                textSize = 11f; typeface = mono
            })
        }

        // Spacing between cards
        card.addView(TextView(this).apply { text = ""; setPadding(0, 0, 0, 4) })
        return card
    }

    @SuppressLint("SetTextI18n")
    private fun refreshSourceInfo(infoView: TextView, provider: DataProvider) {
        val providerId = DataSourceConfig.getProviderId(provider)
        val ready = DataSourceConfig.isProviderReady(provider)
        val sb = StringBuilder()
        sb.append(provider.description)
        if (provider.requiresApiKey) {
            sb.append(if (ready) " • ✓ Key set" else " • ✗ No key")
        }
        if (provider.vpnRequired) {
            sb.append(" • ⚠ VPN needed")
        }
        infoView.text = sb.toString()
    }

    @SuppressLint("SetTextI18n")
    private fun refreshKeySections() {
        keyContainer.removeAllViews()
        for (provider in DataSourceConfig.providers) {
            if (!provider.requiresApiKey) continue
            val providerId = DataSourceConfig.getProviderId(provider)
            val existingKey = DataSourceConfig.getApiKey(providerId) ?: ""

            keyContainer.addView(TextView(this).apply {
                text = "${provider.name}"
                setTextColor(textSecondary); textSize = 12f; typeface = mono; setTypeface(mono, Typeface.BOLD)
                setPadding(0, 16, 0, 4)
            })
            keyContainer.addView(TextView(this).apply {
                text = provider.apiKeyHint
                setTextColor(textDim); textSize = 10f; typeface = mono
                setPadding(0, 0, 0, 4)
            })
            val keyInput = EditText(this).apply {
                hint = "Enter ${provider.name} API key"
                setTextColor(textPrimary); setHintTextColor(textDim)
                textSize = 14f; typeface = mono
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                setPadding(12, 8, 12, 8)
                setBackgroundColor(Color.parseColor("#21262D"))
                if (existingKey.isNotEmpty()) setText(existingKey)
            }
            keyContainer.addView(keyInput)
            keyInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    DataSourceConfig.setApiKey(providerId, s?.toString())
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }
}