package com.example.sipantas

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.LottieAnimationView

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private var hasNavigated = false
    private val fallbackHandler = Handler(Looper.getMainLooper())
    private val fallbackRunnable = Runnable { navigateToMain() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Tap anywhere to skip straight to next screen
        findViewById<View>(R.id.splash_root).setOnClickListener {
            navigateToMain()
        }

        try {
            val lottieView = findViewById<LottieAnimationView>(R.id.lottieAnimationView)
            lottieView.speed = 1.25f

            lottieView.addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    navigateToMain()
                }
            })

            // Fallback timeout in case animation fails to trigger onAnimationEnd
            fallbackHandler.postDelayed(fallbackRunnable, 5500)
        } catch (e: Exception) {
            // Graceful fallback if lottie fails
            fallbackHandler.postDelayed(fallbackRunnable, 2000)
        }
    }

    private fun navigateToMain() {
        if (hasNavigated) return
        hasNavigated = true
        fallbackHandler.removeCallbacks(fallbackRunnable)

        val intent = Intent(this, WelcomeActivity::class.java)
        startActivity(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                android.app.Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.fade_in,
                R.anim.fade_out
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        fallbackHandler.removeCallbacks(fallbackRunnable)
    }
}
