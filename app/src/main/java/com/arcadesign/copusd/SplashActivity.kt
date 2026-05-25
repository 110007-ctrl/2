package com.arcadesign.copusd

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arcadesign.copusd.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animateViews()

        lifecycleScope.launch {
            delay(2800)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun animateViews() {
        // Logo circle pop-in
        val scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in)
        binding.logoContainer.startAnimation(scaleIn)

        // Staggered fade-ins
        lifecycleScope.launch {
            delay(300)
            binding.tvAppName.visibility = View.VISIBLE
            binding.tvAppName.startAnimation(
                AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade_slide_up)
            )

            delay(200)
            binding.tvTagline.visibility = View.VISIBLE
            binding.tvTagline.startAnimation(
                AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade_slide_up)
            )

            delay(300)
            binding.divider.visibility = View.VISIBLE
            binding.divider.startAnimation(
                AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade_in_simple)
            )

            delay(150)
            binding.tvPoweredBy.visibility = View.VISIBLE
            binding.tvPoweredBy.startAnimation(
                AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade_in_simple)
            )

            delay(200)
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.startAnimation(
                AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade_in_simple)
            )
        }
    }
}
