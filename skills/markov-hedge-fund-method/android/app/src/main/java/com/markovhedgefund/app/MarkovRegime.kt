package com.markovhedgefund.app

import kotlin.math.*

object MarkovRegime {

    val STATES = listOf("Bear", "Sideways", "Bull")
    private const val BEAR = 0
    private const val SIDEWAYS = 1
    private const val BULL = 2

    data class RegimeResult(
        val source: String,
        val rows: Int,
        val dateStart: String,
        val dateEnd: String,
        val params: Params,
        val currentRegime: String,
        val nextStateProbabilities: NextProbs,
        val signal: Double,
        val transitionMatrix: Array<DoubleArray>,
        val persistenceDiagonal: Persistence,
        val stationaryDistribution: Stationary,
        val walkForward: WalkForward
    )

    data class Params(val window: Int, val threshold: Double, val minTrain: Int)
    data class NextProbs(val bear: Double, val sideways: Double, val bull: Double)
    data class Persistence(val bear: Double, val sideways: Double, val bull: Double)
    data class Stationary(val bear: Double, val sideways: Double, val bull: Double)
    data class WalkForward(val sharpe: Double, val maxDrawdown: Double, val nTrades: Int)

    // Trade setup — actionable entry/stop/target derived from the regime model
    data class TradeSetup(
        val direction: String,             // "LONG", "SHORT", or "FLAT"
        val conviction: Double,            // |signal| in [0, 1]
        val convictionLabel: String,        // "strong", "moderate", "weak"
        val entryPrice: Double,
        val stopLoss: Double,
        val target1: Double,               // 1:1 R target
        val target2: Double,               // 2:1 R target
        val riskReward1: String,            // "1:1"
        val riskReward2: String,            // "2:1"
        val positionSizePct: Double,        // suggested % of portfolio (0–100)
        val positionSizeLabel: String,      // sizing explanation
        val bearBaseline: Double,           // stationary bear % — tail-risk filter
        val compositionHint: String,        // one-line setup description
        val regime: String,
        val signal: Double
    )

    fun generateTradeSetup(
        closes: DoubleArray,
        regimeResult: RegimeResult,
        baseRiskPct: Double = 1.0        // risk 1% per trade as baseline
    ): TradeSetup {
        val lastClose = closes.last()
        val signal = regimeResult.signal
        val bearBaseline = regimeResult.stationaryDistribution.bear

        // Direction from signal
        val direction = when {
            signal > 0.05 -> "LONG"
            signal < -0.05 -> "SHORT"
            else -> "FLAT"
        }

        // Conviction from |signal|, clamped to [0, 1]
        val conviction = min(abs(signal), 1.0)
        val convictionLabel = when {
            conviction > 0.3 -> "strong"
            conviction > 0.1 -> "moderate"
            else -> "weak"
        }

        // ATR-based stop (14-day ATR)
        val atr = computeATR(closes, 14)

        // Position sizing: base risk scaled by conviction and inverse of bear baseline
        // Higher bear baseline -> smaller position (tail-risk filter)
        val tailRiskScale = 1.0 - bearBaseline  // e.g., bear=0.3 -> 0.7 scale
        val convictionScale = conviction.coerceAtLeast(0.05) // never zero
        val sizePct = baseRiskPct * convictionScale * tailRiskScale * 100  // as % of portfolio

        // Stop loss and targets
        val stopDistance = when (direction) {
            "LONG" -> atr * 1.5
            "SHORT" -> -(atr * 1.5)
            else -> 0.0
        }
        val stopLoss = lastClose - stopDistance
        val risk = abs(lastClose - stopLoss)
        val target1 = when (direction) {
            "LONG" -> lastClose + risk      // 1:1 R:R
            "SHORT" -> lastClose - risk
            else -> lastClose
        }
        val target2 = when (direction) {
            "LONG" -> lastClose + risk * 2   // 2:1 R:R
            "SHORT" -> lastClose - risk * 2
            else -> lastClose
        }

        val compositionHint = when (direction) {
            "LONG" -> "Long bias. P(bull) > P(bear). Scale by $convictionLabel conviction."
            "SHORT" -> "Short bias. P(bear) > P(bull). Scale by $convictionLabel conviction."
            else -> "No edge. P(sideways) dominant. Stand down."
        }

        val positionSizeLabel = when {
            direction == "FLAT" -> "No position — sideways regime"
            bearBaseline > 0.35 -> "Reduced size — high bear baseline (${"%.0f".format(bearBaseline * 100)}%)"
            conviction < 0.1 -> "Minimal size — weak signal"
            else -> "Full size — signal aligned, low tail risk"
        }

        return TradeSetup(
            direction = direction,
            conviction = conviction,
            convictionLabel = convictionLabel,
            entryPrice = lastClose,
            stopLoss = stopLoss,
            target1 = target1,
            target2 = target2,
            riskReward1 = "1:1",
            riskReward2 = "2:1",
            positionSizePct = if (direction == "FLAT") 0.0 else sizePct.coerceAtMost(5.0),
            positionSizeLabel = positionSizeLabel,
            bearBaseline = bearBaseline,
            compositionHint = compositionHint,
            regime = regimeResult.currentRegime,
            signal = signal
        )
    }

    private fun computeATR(closes: DoubleArray, period: Int = 14): Double {
        // Simplified ATR: use close-to-close volatility as proxy
        // (true ATR needs OHLC; we only have close)
        if (closes.size < period + 1) return closes.last() * 0.02 // 2% default
        val returns = DoubleArray(period) { (closes[closes.size - period + it] - closes[closes.size - period + it - 1]) / closes[closes.size - period + it - 1] }
        val meanAbs = returns.map { abs(it) }.average()
        return closes.last() * meanAbs // ATR in price terms
    }

    fun labelRegimes(closes: DoubleArray, window: Int = 20, threshold: Double = 0.05): IntArray {
        val n = closes.size
        val labels = IntArray(n) { SIDEWAYS }
        for (i in window until n) {
            val ret = (closes[i] - closes[i - window]) / closes[i - window]
            labels[i] = when {
                ret > threshold -> BULL
                ret < -threshold -> BEAR
                else -> SIDEWAYS
            }
        }
        return labels
    }

    fun buildTransitionMatrix(labels: IntArray, startIdx: Int = 0, endIdx: Int = labels.size): Array<DoubleArray> {
        val counts = Array(3) { DoubleArray(3) }
        for (i in startIdx until endIdx - 1) {
            if (labels[i] in 0..2 && labels[i + 1] in 0..2) {
                counts[labels[i]][labels[i + 1]] += 1.0
            }
        }
        val P = Array(3) { DoubleArray(3) }
        for (r in 0..2) {
            val rowSum = counts[r].sum()
            for (c in 0..2) {
                P[r][c] = if (rowSum > 0.0) counts[r][c] / rowSum else 1.0 / 3.0
            }
        }
        return P
    }

    fun stationaryDistribution(P: Array<DoubleArray>): DoubleArray {
        val pi = doubleArrayOf(1.0 / 3.0, 1.0 / 3.0, 1.0 / 3.0)
        for (iter in 0 until 200) {
            val newPi = DoubleArray(3)
            for (i in 0..2) {
                for (j in 0..2) {
                    newPi[i] += pi[j] * P[j][i]
                }
            }
            val sum = newPi.sum()
            for (i in 0..2) newPi[i] /= sum
            var converged = true
            for (i in 0..2) {
                if (abs(newPi[i] - pi[i]) > 1e-12) { converged = false; break }
            }
            System.arraycopy(newPi, 0, pi, 0, 3)
            if (converged) break
        }
        return pi
    }

    fun signalFromMatrix(P: Array<DoubleArray>, currentState: Int): Double {
        return P[currentState][BULL] - P[currentState][BEAR]
    }

    fun walkForwardBacktest(closes: DoubleArray, labels: IntArray, minTrain: Int = 252): WalkForward {
        val n = labels.size
        val dailyReturns = DoubleArray(n) { if (it > 0) (closes[it] - closes[it - 1]) / closes[it - 1] else 0.0 }
        val counts = Array(3) { DoubleArray(3) }
        for (i in 0 until minTrain - 1) {
            if (labels[i] in 0..2 && labels[i + 1] in 0..2) {
                counts[labels[i]][labels[i + 1]] += 1.0
            }
        }
        val strategyReturns = mutableListOf<Double>()
        for (t in minTrain until n - 1) {
            val P = Array(3) { DoubleArray(3) }
            for (r in 0..2) {
                val rowSum = counts[r].sum()
                for (c in 0..2) {
                    P[r][c] = if (rowSum > 0.0) counts[r][c] / rowSum else 1.0 / 3.0
                }
            }
            val currentState = labels[t]
            val signal = signalFromMatrix(P, currentState)
            val position = sign(signal)
            val nextReturn = dailyReturns[t + 1]
            strategyReturns.add(position * nextReturn)
            if (labels[t - 1] in 0..2 && labels[t] in 0..2) {
                counts[labels[t - 1]][labels[t]] += 1.0
            }
        }
        if (strategyReturns.isEmpty()) return WalkForward(Double.NaN, Double.NaN, 0)
        val sr = strategyReturns.toDoubleArray()
        val mean = sr.average()
        val std = sqrt(sr.map { (it - mean) * (it - mean) }.sum() / (sr.size - 1))
        val sharpe = if (std == 0.0 || std.isNaN()) Double.NaN else mean / std * sqrt(252.0)
        var equity = 1.0; var peak = 1.0; var maxDD = 0.0
        for (r in sr) {
            equity *= (1.0 + r)
            if (equity > peak) peak = equity
            val dd = (equity - peak) / peak
            if (dd < maxDD) maxDD = dd
        }
        return WalkForward(sharpe, maxDD, sr.size)
    }

    fun analyze(
        closes: DoubleArray,
        dates: List<String>,
        source: String,
        window: Int = 20,
        threshold: Double = 0.05,
        minTrain: Int = 252
    ): RegimeResult {
        val labels = labelRegimes(closes, window, threshold)
        val validStart = window
        val P = buildTransitionMatrix(labels, validStart, labels.size)
        val pi = stationaryDistribution(P)
        val lastValidIdx = labels.lastIndex
        val currentState = labels[lastValidIdx]
        val nextProbs = P[currentState]
        val signal = signalFromMatrix(P, currentState)
        val bt = walkForwardBacktest(closes, labels, minTrain)
        return RegimeResult(
            source = source,
            rows = closes.size,
            dateStart = dates.firstOrNull() ?: "",
            dateEnd = dates.lastOrNull() ?: "",
            params = Params(window, threshold, minTrain),
            currentRegime = STATES[currentState],
            nextStateProbabilities = NextProbs(nextProbs[BEAR], nextProbs[SIDEWAYS], nextProbs[BULL]),
            signal = signal,
            transitionMatrix = P,
            persistenceDiagonal = Persistence(P[BEAR][BEAR], P[SIDEWAYS][SIDEWAYS], P[BULL][BULL]),
            stationaryDistribution = Stationary(pi[BEAR], pi[SIDEWAYS], pi[BULL]),
            walkForward = bt
        )
    }
}
