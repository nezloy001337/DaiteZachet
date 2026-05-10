package com.example.daitezachet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager

class SplashActivity : Activity() {

    private lateinit var splashView: SplashView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        splashView = SplashView(this)
        splashView.onFinished = {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        setContentView(splashView)
    }

    override fun onResume() {
        super.onResume()
        splashView.resume()
    }

    override fun onPause() {
        super.onPause()
        splashView.pause()
    }
}
