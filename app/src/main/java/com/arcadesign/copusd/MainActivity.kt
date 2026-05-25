package com.arcadesign.copusd

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arcadesign.copusd.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentRate: Double = 0.0
    private var isUpdatingCop = false
    private var isUpdatingUsd = false
    private var isCopMode = true  // true = COP→USD, false = USD→COP

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val copFormat = DecimalFormat("#,##0.##").apply {
        groupingSize = 3
    }
    private val usdFormat = DecimalFormat("#,##0.######")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        fetchExchangeRate()
    }

    private fun setupListeners() {
        // COP input watcher
        binding.etCop.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingUsd || currentRate == 0.0) return
                isUpdatingCop = true
                val raw = s.toString().replace(",", "").replace(".", "").trim()
                if (raw.isEmpty()) {
                    isUpdatingUsd = true
                    binding.etUsd.setText("")
                    isUpdatingUsd = false
                } else {
                    try {
                        val cop = raw.toLong()
                        val usd = cop * currentRate
                        isUpdatingUsd = true
                        binding.etUsd.setText(formatUsd(usd))
                        isUpdatingUsd = false
                        updateConversionPreview(cop.toDouble(), usd)
                    } catch (e: NumberFormatException) { /* ignore */ }
                }
                isUpdatingCop = false
            }
        })

        // USD input watcher
        binding.etUsd.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingCop || currentRate == 0.0) return
                isUpdatingUsd = true
                val raw = s.toString().replace(",", "").trim()
                if (raw.isEmpty()) {
                    isUpdatingCop = true
                    binding.etCop.setText("")
                    isUpdatingCop = false
                } else {
                    try {
                        val usd = raw.toDouble()
                        val cop = usd / currentRate
                        isUpdatingCop = true
                        binding.etCop.setText(formatCop(cop.toLong()))
                        isUpdatingCop = false
                        updateConversionPreview(cop, usd)
                    } catch (e: NumberFormatException) { /* ignore */ }
                }
                isUpdatingUsd = false
            }
        })

        // Refresh button
        binding.btnRefreshRate.setOnClickListener {
            val spin = AnimationUtils.loadAnimation(this, R.anim.spin)
            binding.btnRefreshRate.startAnimation(spin)
            fetchExchangeRate()
        }

        // Clear button
        binding.btnClear.setOnClickListener {
            binding.etCop.setText("")
            binding.etUsd.setText("")
            binding.tvConversionNote.text = ""
        }

        // Quick amount buttons
        binding.btn1k.setOnClickListener   { setQuickAmount(1_000L) }
        binding.btn10k.setOnClickListener  { setQuickAmount(10_000L) }
        binding.btn50k.setOnClickListener  { setQuickAmount(50_000L) }
        binding.btn100k.setOnClickListener { setQuickAmount(100_000L) }
        binding.btn500k.setOnClickListener { setQuickAmount(500_000L) }
        binding.btn1m.setOnClickListener   { setQuickAmount(1_000_000L) }
    }

    private fun setQuickAmount(amount: Long) {
        binding.etCop.setText(formatCop(amount))
        binding.etCop.setSelection(binding.etCop.text.length)
        val anim = AnimationUtils.loadAnimation(this, R.anim.pulse)
        binding.cardConverter.startAnimation(anim)
    }

    private fun fetchExchangeRate() {
        binding.tvRateStatus.text = "Fetching live rate…"
        binding.tvRateStatus.setTextColor(getColor(R.color.blue_primary))
        binding.progressRate.visibility = View.VISIBLE
        binding.tvRate.visibility = View.INVISIBLE

        lifecycleScope.launch {
            try {
                val rate = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://api.coinbase.com/v2/exchange-rates?currency=COP")
                        .addHeader("User-Agent", "CopUsdCalculator/1.0")
                        .build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: throw Exception("Empty response")
                    val json = JSONObject(body)
                    val rates = json.getJSONObject("data").getJSONObject("rates")
                    rates.getDouble("USD")
                }

                currentRate = rate
                val rateDisplay = String.format("%.6f", rate)
                val inverseRate = String.format("%,.2f", 1.0 / rate)

                binding.progressRate.visibility = View.GONE
                binding.tvRate.visibility = View.VISIBLE
                binding.tvRate.text = "1 COP = $${rateDisplay} USD  •  1 USD = COP $inverseRate"
                binding.tvRateStatus.text = "Live rate • Coinbase"
                binding.tvRateStatus.setTextColor(getColor(R.color.green_success))

                // Timestamp
                val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                binding.tvLastUpdated.text = "Updated: ${sdf.format(Date())}"

                // Animate rate card
                binding.cardRate.startAnimation(
                    AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in_simple)
                )

                // Recalculate if values are already entered
                val copText = binding.etCop.text.toString().replace(",", "")
                if (copText.isNotEmpty()) {
                    binding.etCop.setText(binding.etCop.text)
                }

            } catch (e: Exception) {
                binding.progressRate.visibility = View.GONE
                binding.tvRate.visibility = View.VISIBLE
                binding.tvRate.text = "Could not fetch rate"
                binding.tvRateStatus.text = "⚠ No connection — check internet"
                binding.tvRateStatus.setTextColor(getColor(R.color.red_error))
            }
        }
    }

    private fun updateConversionPreview(cop: Double, usd: Double) {
        if (usd == 0.0) { binding.tvConversionNote.text = ""; return }
        binding.tvConversionNote.text = when {
            usd >= 1000 -> String.format("That's about $%.2fK USD", usd / 1000)
            usd >= 1    -> String.format("That's $%.2f USD", usd)
            else        -> String.format("Less than $1 USD (%.4f)", usd)
        }
    }

    private fun formatCop(amount: Long): String {
        return NumberFormat.getNumberInstance(Locale("es", "CO")).format(amount)
    }

    private fun formatUsd(amount: Double): String {
        return if (amount == 0.0) "" else usdFormat.format(amount)
    }
}
