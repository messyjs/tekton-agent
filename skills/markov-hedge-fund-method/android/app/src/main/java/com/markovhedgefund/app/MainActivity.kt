package com.markovhedgefund.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var tickerInput: EditText
    private lateinit var yearsInput: EditText
    private lateinit var windowInput: EditText
    private lateinit var thresholdInput: EditText
    private lateinit var analyzeButton: Button
    private lateinit var settingsButton: ImageButton
    private lateinit var sourceSpinner: Spinner
    private lateinit var sourceStatusText: TextView
    private lateinit var statusText: TextView
    private lateinit var sourceInfoText: TextView
    // Tabs
    private lateinit var tabRegime: TextView
    private lateinit var tabSetups: TextView
    private lateinit var regimeScroll: ScrollView
    private lateinit var setupsScroll: ScrollView
    // Regime tab
    private lateinit var resultsContainer: LinearLayout
    private lateinit var currentRegimeText: TextView
    private lateinit var nextProbsRow: LinearLayout
    private lateinit var matrixGrid: LinearLayout
    private lateinit var persistenceGrid: LinearLayout
    private lateinit var stationaryGrid: LinearLayout
    private lateinit var signalText: TextView
    private lateinit var signalDirection: TextView
    private lateinit var backtestGrid: LinearLayout
    // Setups tab
    private lateinit var setupsContainer: LinearLayout
    private lateinit var setupDirectionCard: LinearLayout
    private lateinit var setupDirectionText: TextView
    private lateinit var setupConvictionLabel: TextView
    private lateinit var setupHintText: TextView
    private lateinit var setupLevelsGrid: LinearLayout
    private lateinit var setupSizePctText: TextView
    private lateinit var setupSizeLabel: TextView
    private lateinit var setupSizeReason: TextView
    private lateinit var setupContextGrid: LinearLayout

    private val bullColor = Color.parseColor("#84BBA1")
    private val bearColor = Color.parseColor("#C57F86")
    private val sidewaysColor = Color.parseColor("#A4ABB7")
    private val accentColor = Color.parseColor("#3FDE7E")
    private val accentDimColor = Color.parseColor("#6BF0A6")
    private val textPrimary = Color.parseColor("#E6EDF3")
    private val textSecondary = Color.parseColor("#8B949E")
    private val textDim = Color.parseColor("#6E7681")
    private val diagBgColor = Color.parseColor("#264D3A")
    private val cardBg = Color.parseColor("#1C2128")
    private val mono: Typeface by lazy { Typeface.MONOSPACE }
    private var lastResult: MarkovRegime.RegimeResult? = null
    private var lastCloses: DoubleArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        DataSourceConfig.init(this)

        tickerInput = findViewById(R.id.tickerInput)
        yearsInput = findViewById(R.id.yearsInput)
        windowInput = findViewById(R.id.windowInput)
        thresholdInput = findViewById(R.id.thresholdInput)
        analyzeButton = findViewById(R.id.analyzeButton)
        settingsButton = findViewById(R.id.settingsButton)
        sourceSpinner = findViewById(R.id.sourceSpinner)
        sourceStatusText = findViewById(R.id.sourceStatusText)
        statusText = findViewById(R.id.statusText)
        sourceInfoText = findViewById(R.id.sourceInfoText)
        tabRegime = findViewById(R.id.tabRegime)
        tabSetups = findViewById(R.id.tabSetups)
        regimeScroll = findViewById(R.id.regimeScroll)
        setupsScroll = findViewById(R.id.setupsScroll)
        resultsContainer = findViewById(R.id.resultsContainer)
        currentRegimeText = findViewById(R.id.currentRegimeText)
        nextProbsRow = findViewById(R.id.nextProbsRow)
        matrixGrid = findViewById(R.id.matrixGrid)
        persistenceGrid = findViewById(R.id.persistenceGrid)
        stationaryGrid = findViewById(R.id.stationaryGrid)
        signalText = findViewById(R.id.signalText)
        signalDirection = findViewById(R.id.signalDirection)
        backtestGrid = findViewById(R.id.backtestGrid)
        setupsContainer = findViewById(R.id.setupsContainer)
        setupDirectionCard = findViewById(R.id.setupDirectionCard)
        setupDirectionText = findViewById(R.id.setupDirectionText)
        setupConvictionLabel = findViewById(R.id.setupConvictionLabel)
        setupHintText = findViewById(R.id.setupHintText)
        setupLevelsGrid = findViewById(R.id.setupLevelsGrid)
        setupSizePctText = findViewById(R.id.setupSizePctText)
        setupSizeLabel = findViewById(R.id.setupSizeLabel)
        setupSizeReason = findViewById(R.id.setupSizeReason)
        setupContextGrid = findViewById(R.id.setupContextGrid)

        tickerInput.setText("SPY")

        // Tabs
        tabRegime.setOnClickListener { switchTab("regime") }
        tabSetups.setOnClickListener { switchTab("setups") }

        // Source spinner
        val providerNames = DataSourceConfig.providers.map { it.name }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, providerNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sourceSpinner.adapter = spinnerAdapter
        val savedSource = DataSourceConfig.getActiveSourceName()
        sourceSpinner.setSelection(providerNames.indexOf(savedSource).coerceAtLeast(0))
        updateSourceStatus(DataSourceConfig.getActiveProvider())
        sourceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val provider = DataSourceConfig.providers[position]
                DataSourceConfig.setActiveSourceName(provider.name)
                updateSourceStatus(provider)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        settingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        analyzeButton.setOnClickListener { runAnalysis() }
        tickerInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) { runAnalysis(); true } else false
        }

        switchTab("regime")
    }

    override fun onResume() {
        super.onResume()
        val activeProvider = DataSourceConfig.getActiveProvider()
        val idx = DataSourceConfig.providers.indexOf(activeProvider).coerceAtLeast(0)
        sourceSpinner.setSelection(idx)
        updateSourceStatus(activeProvider)
    }

    private fun switchTab(tab: String) {
        if (tab == "regime") {
            tabRegime.setTextColor(accentColor); tabRegime.setBackgroundColor(Color.parseColor("#1C2128"))
            tabSetups.setTextColor(textDim); tabSetups.setBackgroundColor(Color.parseColor("#0D1117"))
            regimeScroll.visibility = View.VISIBLE; setupsScroll.visibility = View.GONE
        } else {
            tabRegime.setTextColor(textDim); tabRegime.setBackgroundColor(Color.parseColor("#0D1117"))
            tabSetups.setTextColor(accentColor); tabSetups.setBackgroundColor(Color.parseColor("#1C2128"))
            regimeScroll.visibility = View.GONE; setupsScroll.visibility = View.VISIBLE
            // Render setups if we have data
            val result = lastResult; val closes = lastCloses
            if (result != null && closes != null) renderSetups(result, closes)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateSourceStatus(provider: DataProvider) {
        val sb = StringBuilder()
        val providerId = DataSourceConfig.getProviderId(provider)
        if (provider.requiresApiKey) {
            val hasKey = !DataSourceConfig.getApiKey(providerId).isNullOrBlank()
            sb.append(if (hasKey) "Key set" else "No API key — tap gear icon")
            sourceStatusText.setTextColor(if (hasKey) accentDimColor else bearColor)
        } else {
            sb.append("Free — no key needed")
            sourceStatusText.setTextColor(accentDimColor)
        }
        if (provider.vpnRequired) {
            sb.append("  | VPN required")
            sourceStatusText.setTextColor(Color.parseColor("#E8A838"))
        }
        sourceStatusText.text = sb.toString()
        tickerInput.hint = "e.g. ${provider.tickerFormat}"
    }

    private fun runAnalysis() {
        val ticker = tickerInput.text.toString().trim().uppercase()
        if (ticker.isBlank()) { Toast.makeText(this, "Enter a ticker symbol", Toast.LENGTH_SHORT).show(); return }
        val years = yearsInput.text.toString().toIntOrNull() ?: 10
        val window = windowInput.text.toString().toIntOrNull() ?: 20
        val thresholdPct = thresholdInput.text.toString().replace("%", "").toDoubleOrNull() ?: 5.0
        val threshold = thresholdPct / 100.0
        val provider = DataSourceConfig.getActiveProvider()
        if (provider.requiresApiKey) {
            val id = DataSourceConfig.getProviderId(provider)
            if (DataSourceConfig.getApiKey(id).isNullOrBlank()) {
                Toast.makeText(this, "${provider.name} needs an API key. Tap the gear icon.", Toast.LENGTH_LONG).show(); return
            }
        }
        resultsContainer.visibility = View.GONE; setupsContainer.visibility = View.GONE
        statusText.visibility = View.VISIBLE; statusText.setTextColor(textDim)
        statusText.text = "Fetching $ticker via ${provider.name}..."
        analyzeButton.isEnabled = false
        lifecycleScope.launch {
            try {
                val data = provider.fetchClose(ticker, years)
                statusText.text = "Computing regime on ${data.closes.size} rows..."
                val result = MarkovRegime.analyze(closes = data.closes, dates = data.dates, source = data.source, window = window, threshold = threshold)
                lastResult = result; lastCloses = data.closes
                statusText.visibility = View.GONE
                renderResults(result)
                resultsContainer.visibility = View.VISIBLE
            } catch (e: Exception) {
                statusText.text = "Error: ${e.message}"; statusText.setTextColor(bearColor)
            } finally { analyzeButton.isEnabled = true }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun renderResults(r: MarkovRegime.RegimeResult) {
        val regimeColor = when (r.currentRegime) { "Bull" -> bullColor; "Bear" -> bearColor; else -> sidewaysColor }
        currentRegimeText.text = r.currentRegime.uppercase(); currentRegimeText.setTextColor(regimeColor)
        sourceInfoText.text = "${r.source}  |  ${r.rows} rows  |  ${r.dateStart} → ${r.dateEnd}"
        nextProbsRow.removeAllViews()
        nextProbsRow.addView(makeProbPill("Bull", r.nextStateProbabilities.bull, bullColor))
        nextProbsRow.addView(makeProbPill("Bear", r.nextStateProbabilities.bear, bearColor))
        nextProbsRow.addView(makeProbPill("Side", r.nextStateProbabilities.sideways, sidewaysColor))
        matrixGrid.removeAllViews()
        val hdr = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        hdr.addView(makeCell("", textDim, 10)); hdr.addView(makeCell("Bear", textSecondary, 10))
        hdr.addView(makeCell("Side", textSecondary, 10)); hdr.addView(makeCell("Bull", textSecondary, 10))
        matrixGrid.addView(hdr)
        for (i in 0..2) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            val sn = MarkovRegime.STATES[i]; val sc = when (sn) { "Bull" -> bullColor; "Bear" -> bearColor; else -> sidewaysColor }
            row.addView(makeCell(sn, sc, 10))
            for (j in 0..2) { val diag = (i == j); row.addView(makeCell(String.format("%.1f%%", r.transitionMatrix[i][j] * 100), if (diag) accentDimColor else textSecondary, 10)) }
            matrixGrid.addView(row)
        }
        persistenceGrid.removeAllViews()
        val p = r.persistenceDiagonal
        persistenceGrid.addView(makePersistRow("Bear → Bear", p.bear, bearColor))
        persistenceGrid.addView(makePersistRow("Side → Side", p.sideways, sidewaysColor))
        persistenceGrid.addView(makePersistRow("Bull → Bull", p.bull, bullColor))
        stationaryGrid.removeAllViews()
        val s = r.stationaryDistribution
        stationaryGrid.addView(makeStationaryPill("Bull", s.bull, bullColor))
        stationaryGrid.addView(makeStationaryPill("Bear", s.bear, bearColor))
        stationaryGrid.addView(makeStationaryPill("Side", s.sideways, sidewaysColor))
        signalText.text = String.format("%+.4f", r.signal)
        signalText.setTextColor(if (r.signal > 0) bullColor else if (r.signal < 0) bearColor else sidewaysColor)
        val dir = when { r.signal > 0.05 -> "LONG BIAS"; r.signal < -0.05 -> "SHORT BIAS"; else -> "NEUTRAL" }
        signalDirection.text = dir; signalDirection.setTextColor(if (r.signal > 0) bullColor else if (r.signal < 0) bearColor else sidewaysColor)
        backtestGrid.removeAllViews()
        val wf = r.walkForward
        backtestGrid.addView(makeBacktestRow("Sharpe", if (wf.sharpe.isNaN()) "N/A" else String.format("%.3f", wf.sharpe)))
        backtestGrid.addView(makeBacktestRow("Max DD", if (wf.maxDrawdown.isNaN()) "N/A" else String.format("%.1f%%", wf.maxDrawdown * 100)))
        backtestGrid.addView(makeBacktestRow("Trades", wf.nTrades.toString()))
    }

    @SuppressLint("SetTextI18n")
    private fun renderSetups(r: MarkovRegime.RegimeResult, closes: DoubleArray) {
        val setup = MarkovRegime.generateTradeSetup(closes, r)
        setupsContainer.visibility = View.VISIBLE

        // Direction card
        val dirColor = when (setup.direction) { "LONG" -> bullColor; "SHORT" -> bearColor; else -> sidewaysColor }
        setupDirectionText.text = setup.direction; setupDirectionText.setTextColor(dirColor)
        setupConvictionLabel.text = "${setup.convictionLabel.uppercase()} conviction  (${String.format("%.1f%%", setup.conviction * 100)})"
        setupConvictionLabel.setTextColor(dirColor)
        setupHintText.text = setup.compositionHint; setupHintText.setTextColor(textSecondary)

        // Levels
        setupLevelsGrid.removeAllViews()
        val df = DecimalFormat("#,##0.00")
        setupLevelsGrid.addView(makeLevelRow("Entry", df.format(setup.entryPrice), textPrimary))
        setupLevelsGrid.addView(makeLevelRow("Stop", df.format(setup.stopLoss), bearColor))
        setupLevelsGrid.addView(makeLevelRow(if (setup.direction == "SHORT") "Target 1" else "Target 1", df.format(setup.target1), bullColor))
        setupLevelsGrid.addView(makeLevelRow("Target 2", df.format(setup.target2), accentDimColor))
        setupLevelsGrid.addView(makeLevelRow("R:R 1", setup.riskReward1, textSecondary))
        setupLevelsGrid.addView(makeLevelRow("R:R 2", setup.riskReward2, textSecondary))

        // Risk distance
        val riskPct = if (setup.entryPrice != 0.0) (abs(setup.entryPrice - setup.stopLoss) / setup.entryPrice) * 100 else 0.0
        setupLevelsGrid.addView(makeLevelRow("Risk dist", String.format("%.2f%%", riskPct), Color.parseColor("#E8A838")))

        // Position size
        setupSizePctText.text = if (setup.positionSizePct == 0.0) "FLAT" else String.format("%.2f%%", setup.positionSizePct)
        setupSizePctText.setTextColor(dirColor)
        setupSizeLabel.text = setup.positionSizeLabel; setupSizeLabel.setTextColor(textSecondary)

        // Size reason with tail-risk context
        val bearPct = String.format("%.0f%%", setup.bearBaseline * 100)
        setupSizeReason.text = "Bear baseline: $bearPct of time in bear regime"; setupSizeReason.setTextColor(textDim)

        // Context grid
        setupContextGrid.removeAllViews()
        val wf = r.walkForward
        setupContextGrid.addView(makeBacktestRow("Regime", r.currentRegime))
        setupContextGrid.addView(makeBacktestRow("Signal", String.format("%+.4f", r.signal)))
        setupContextGrid.addView(makeBacktestRow("Sharpe", if (wf.sharpe.isNaN()) "N/A" else String.format("%.3f", wf.sharpe)))
        setupContextGrid.addView(makeBacktestRow("Max DD", if (wf.maxDrawdown.isNaN()) "N/A" else String.format("%.1f%%", wf.maxDrawdown * 100)))
    }

    // --- Tab helpers ---
    private fun makeProbPill(label: String, prob: Double, color: Int): View {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(24, 8, 24, 8) }
        c.addView(TextView(this).apply { text = String.format("%.1f%%", prob * 100); setTextColor(color); textSize = 16f; typeface = mono; gravity = Gravity.CENTER })
        c.addView(TextView(this).apply { text = label; setTextColor(textDim); textSize = 10f; typeface = mono; gravity = Gravity.CENTER })
        return c
    }
    private fun makeCell(text: String, textColor: Int, textSizePt: Int): TextView {
        return TextView(this).apply { this.text = text; setTextColor(textColor); textSize = textSizePt.toFloat(); typeface = mono; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setPadding(4, 4, 4, 4) }
    }
    private fun makePersistRow(label: String, value: Double, color: Int): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 4, 0, 4) }
        row.addView(TextView(this).apply { text = label; setTextColor(textSecondary); textSize = 13f; typeface = mono; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
        row.addView(TextView(this).apply { text = String.format("%.1f%%", value * 100); setTextColor(color); textSize = 14f; typeface = mono; setTypeface(mono, Typeface.BOLD) })
        return row
    }
    private fun makeStationaryPill(label: String, value: Double, @Suppress("UNUSED_PARAMETER") color: Int): View {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(16, 12, 16, 12); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setBackgroundColor(diagBgColor) }
        c.addView(TextView(this).apply { text = String.format("%.1f%%", value * 100); setTextColor(accentDimColor); textSize = 18f; typeface = mono; setTypeface(mono, Typeface.BOLD); gravity = Gravity.CENTER })
        c.addView(TextView(this).apply { text = label; setTextColor(textSecondary); textSize = 11f; typeface = mono; gravity = Gravity.CENTER })
        return c
    }
    private fun makeBacktestRow(label: String, value: String): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 4, 0, 4) }
        row.addView(TextView(this).apply { text = label; setTextColor(textSecondary); textSize = 13f; typeface = mono; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
        row.addView(TextView(this).apply { text = value; setTextColor(textPrimary); textSize = 13f; typeface = mono; setTypeface(mono, Typeface.BOLD) })
        return row
    }
    private fun makeLevelRow(label: String, value: String, color: Int): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 4, 0, 4) }
        row.addView(TextView(this).apply { text = label; setTextColor(textSecondary); textSize = 13f; typeface = mono; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
        row.addView(TextView(this).apply { text = value; setTextColor(color); textSize = 14f; typeface = mono; setTypeface(mono, Typeface.BOLD) })
        return row
    }
}