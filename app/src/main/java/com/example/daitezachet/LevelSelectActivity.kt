package com.example.daitezachet

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.daitezachet.levels.LevelRegistry

class LevelSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Размер ячейки — от высоты экрана, чтобы влезало несколько рядов в альбоме
        val dm       = resources.displayMetrics
        val cellSize = (dm.heightPixels / 3.5f).toInt()
        val cols     = (dm.widthPixels / (cellSize + 16)).coerceAtLeast(3)

        // Контейнер с заголовком
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(26, 26, 40))
            setPadding(24, 32, 24, 32)
        }

        val title = TextView(this).apply {
            text     = "Выбор уровня"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity  = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        inner.addView(title)

        val grid = GridLayout(this).apply {
            columnCount = cols
        }

        for (n in 1..LevelRegistry.count) {
            val cell = TextView(this).apply {
                text     = "$n"
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.rgb(55, 105, 180))
                gravity  = Gravity.CENTER

                layoutParams = GridLayout.LayoutParams().apply {
                    width  = cellSize
                    height = cellSize
                    setMargins(8, 8, 8, 8)
                }

                setOnClickListener {
                    startActivity(
                        Intent(this@LevelSelectActivity, GameActivity::class.java)
                            .putExtra(GameActivity.EXTRA_START_LEVEL, n)
                    )
                }
            }
            grid.addView(cell)
        }

        inner.addView(grid)

        // ScrollView позволяет листать при большом числе уровней
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(18, 18, 28))
            addView(inner)
        }

        setContentView(scroll)
    }
}
