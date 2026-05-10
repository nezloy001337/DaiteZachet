package com.example.daitezachet

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.daitezachet.engine.GameView

class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView

    companion object {
        const val EXTRA_START_LEVEL = "start_level"
        private const val KEY_CURRENT_LEVEL = "current_level"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val container = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        // Восстанавливаем уровень из savedInstanceState или из Intent
        val startLevel = savedInstanceState?.getInt(KEY_CURRENT_LEVEL)
            ?: intent.getIntExtra(EXTRA_START_LEVEL, 1)

        gameView = GameView(this).apply {
            startLevelNumber = startLevel
            onGameComplete = { showCompleteScreen(container) }
            onExitLevel    = { finish() }
        }

        container.addView(gameView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(container)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Сохраняем ТЕКУЩИЙ уровень (не стартовый!)
        outState.putInt(KEY_CURRENT_LEVEL, gameView.getCurrentLevelNumber())
    }

    private fun showCompleteScreen(container: FrameLayout) {
        runOnUiThread {
            val tv = TextView(this).apply {
                text      = "Игра пройдена!\n\nВсе уровни завершены!"
                textSize  = 34f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.argb(230, 0, 60, 10))
                gravity   = Gravity.CENTER
                setPadding(40, 60, 40, 60)
                setOnClickListener { finish() }
            }
            container.addView(tv, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).also { it.gravity = Gravity.CENTER })
        }
    }

    override fun onPause()  { super.onPause();  gameView.pause() }
    override fun onResume() { super.onResume()  /* resume вызывается из surfaceChanged */ }
    override fun onDestroy() { super.onDestroy(); gameView.pause() }
}
