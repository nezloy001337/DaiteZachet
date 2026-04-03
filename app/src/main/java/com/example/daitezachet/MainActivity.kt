package com.example.daitezachet

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER
            setBackgroundColor(Color.rgb(18, 18, 28))
            setPadding(60, 0, 60, 0)
        }

        val title = TextView(this).apply {
            text      = "DaiteZachet"
            textSize  = 46f
            typeface  = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity   = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val subtitle = TextView(this).apply {
            text     = "Одна комната — разные правила"
            textSize = 18f
            setTextColor(Color.rgb(150, 150, 170))
            gravity  = Gravity.CENTER
            setPadding(0, 0, 0, 80)
        }

        val startBtn = Button(this).apply {
            text     = "Начать игру"
            textSize = 22f
            setOnClickListener {
                val intent = Intent(this@MainActivity, GameActivity::class.java)
                intent.putExtra(GameActivity.EXTRA_START_LEVEL, 1)
                startActivity(intent)
            }
        }

        val selectBtn = Button(this).apply {
            text     = "Выбрать уровень"
            textSize = 20f
            setPadding(0, 16, 0, 0)
            setOnClickListener {
                startActivity(Intent(this@MainActivity, LevelSelectActivity::class.java))
            }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(startBtn)
        root.addView(selectBtn)
        setContentView(root)
    }
}
