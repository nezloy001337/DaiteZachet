package com.example.daitezachet

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.daitezachet.levels.LevelRegistry

class LevelSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(18, 18, 28))
            setPadding(32, 48, 32, 32)
        }

        val title = TextView(this).apply {
            text     = "Выбор уровня"
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity  = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        root.addView(title)

        val grid = GridLayout(this).apply {
            columnCount = 3
            rowCount    = (LevelRegistry.count + 2) / 3
        }

        for (n in 1..LevelRegistry.count) {
            val btn = TextView(this).apply {
                text     = "$n"
                textSize = 26f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.rgb(40, 80, 140))
                gravity  = Gravity.CENTER
                setPadding(0, 0, 0, 0)

                val size   = resources.displayMetrics.widthPixels / 3 - 24
                val params = GridLayout.LayoutParams().apply {
                    width  = size
                    height = size
                    setMargins(8, 8, 8, 8)
                }
                layoutParams = params

                setOnClickListener {
                    val intent = Intent(this@LevelSelectActivity, GameActivity::class.java)
                    intent.putExtra(GameActivity.EXTRA_START_LEVEL, n)
                    startActivity(intent)
                }
            }
            grid.addView(btn)
        }

        root.addView(grid)
        setContentView(root)
    }
}
