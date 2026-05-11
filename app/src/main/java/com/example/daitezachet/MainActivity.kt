package com.example.daitezachet

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.daitezachet.engine.CreditsView

class MainActivity : AppCompatActivity() {

    private val BG_COLOR    = Color.rgb(12, 12, 22)
    private val ACCENT      = Color.rgb(0, 220, 180)
    private val ACCENT_DIM  = Color.rgb(0, 130, 110)
    private val ACCENT_DARK = Color.rgb(0, 45, 38)
    private val TEXT_WHITE  = Color.rgb(230, 240, 255)
    private val TEXT_MUTED  = Color.rgb(100, 115, 145)

    private val pixelFont: Typeface by lazy {
        try { ResourcesCompat.getFont(this, R.font.pixel)!! }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    private lateinit var root: FrameLayout
    private var creditsHost: FrameLayout? = null
    private var creditsContentView: View? = null
    private var creditsView: CreditsView? = null
    private var creditsRunner: Runnable? = null
    private var creditsOverlayVisible = false
    private var creditsLastFrameMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goFullscreen()

        root = FrameLayout(this).apply {
            setBackgroundColor(BG_COLOR)
        }

        root.addView(
            GridOverlayView(this),
            FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )

        val content = buildScreen()
        root.addView(content)

        setContentView(root)

        content.alpha = 0f
        content.translationY = 32f
        content.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun goFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = BG_COLOR
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun buildScreen(): View =
        android.widget.ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                setPadding(dp(20), dp(20), dp(20), dp(20))
                gravity = Gravity.CENTER_HORIZONTAL

                addView(buildTopSpacer(), LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
                addView(buildCenterBlock(), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                addView(buildSpacer(dp(18)))
                addView(buildBottomBlock(), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            })
        }

    private fun buildTopSpacer(): View = View(this)

    private fun buildCenterBlock(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        val titleView = buildTitle()
        addView(titleView)
        startPulseAnimation(titleView)

        addView(buildSpacer(dp(8)))
        addView(buildSubtitle())
        addView(buildSpacer(dp(16)))
        addView(buildDivider())
        addView(buildSpacer(dp(18)))

        addView(buildPrimaryButton("▶  НАЧАТЬ ИГРУ") {
            val intent = Intent(this@MainActivity, GameActivity::class.java)
            intent.putExtra(GameActivity.EXTRA_START_LEVEL, 1)
            startActivity(intent)
        })

        addView(buildSpacer(dp(10)))
        addView(buildSecondaryButton("⊞  ВЫБРАТЬ УРОВЕНЬ") {
            startActivity(Intent(this@MainActivity, LevelSelectActivity::class.java))
        })

        addView(buildSpacer(dp(10)))
        addView(buildSecondaryButton("✦  ПЕРСОНАЖ") {
            startActivity(Intent(this@MainActivity, CustomizeActivity::class.java))
        })

        addView(buildSpacer(dp(10)))
        addView(buildSecondaryButton("★  РЕКОРДЫ") {
            startActivity(Intent(this@MainActivity, RecordsActivity::class.java))
        })
    }

    private fun buildBottomBlock(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

        addView(buildFooterLabel("v1.0  ·  15 уровней"))
        addView(buildSpacer(dp(8)))
        addView(buildTinyButton("ТИТРЫ") {
            showCreditsOverlay()
        })
    }

    private fun buildTitle(): TextView = TextView(this).apply {
        text = "DAITE ZACHET"
        typeface = pixelFont
        textSize = 32f
        setTextColor(TEXT_WHITE)
        gravity = Gravity.CENTER
        letterSpacing = 0.07f
        setShadowLayer(16f, 0f, 0f, Color.argb(180, 0, 220, 180))
    }

    private fun buildSubtitle(): TextView = TextView(this).apply {
        text = "одна комната — разные правила"
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textSize = 11f
        setTextColor(TEXT_MUTED)
        gravity = Gravity.CENTER
        letterSpacing = 0.05f
    }

    private fun buildDivider(): View = View(this).apply {
        background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.TRANSPARENT, ACCENT, Color.TRANSPARENT)
        )
        layoutParams = LinearLayout.LayoutParams(dp(220), dp(1)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
    }

    private fun buildPrimaryButton(label: String, onClick: () -> Unit): View =
        buildGameButton(label, Color.rgb(0, 200, 160), BG_COLOR, ACCENT, onClick)

    private fun buildSecondaryButton(label: String, onClick: () -> Unit): View =
        buildGameButton(label, Color.argb(25, 0, 220, 180), ACCENT, ACCENT_DIM, onClick)

    private fun buildGameButton(
        label: String,
        fillColor: Int,
        textColor: Int,
        border: Int,
        onClick: () -> Unit
    ): TextView {
        val bw = dp(2)

        fun makeBg(fill: Int): Drawable {
            val stroke = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 0f
                setColor(border)
            }
            val inner = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 0f
                setColor(fill)
            }
            return LayerDrawable(arrayOf(stroke, InsetDrawable(inner, bw)))
        }

        val states = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), makeBg(ACCENT_DARK))
            addState(intArrayOf(), makeBg(fillColor))
        }

        return TextView(this).apply {
            text = label
            typeface = pixelFont
            textSize = 12f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            letterSpacing = 0.06f
            background = states
            setPadding(dp(20), dp(12), dp(20), dp(12))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(dp(270), WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(70).start()
                        startShakeAnimation(v)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    }
                }
                false
            }

            setOnClickListener { onClick() }
        }
    }

    private fun buildTinyButton(label: String, onClick: () -> Unit): View {
        val bw = dp(1)

        fun makeBg(fill: Int): Drawable {
            val stroke = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 0f
                setColor(ACCENT_DIM)
            }
            val inner = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 0f
                setColor(fill)
            }
            return LayerDrawable(arrayOf(stroke, InsetDrawable(inner, bw)))
        }

        val states = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), makeBg(ACCENT_DARK))
            addState(intArrayOf(), makeBg(Color.argb(18, 0, 220, 180)))
        }

        return TextView(this).apply {
            text = label
            typeface = pixelFont
            textSize = 9f
            setTextColor(Color.argb(180, 220, 240, 255))
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
            background = states
            setPadding(dp(12), dp(6), dp(12), dp(6))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().alpha(0.8f).scaleX(0.96f).scaleY(0.96f).setDuration(70).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(100).start()
                    }
                }
                false
            }

            setOnClickListener { onClick() }
        }
    }

    private fun showCreditsOverlay() {
        if (creditsOverlayVisible) return
        creditsOverlayVisible = true
        creditsLastFrameMs = SystemClock.elapsedRealtime()

        val host = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(235, 5, 8, 16))
            alpha = 0f
            isClickable = true
            isFocusable = true
        }

        val cv = CreditsView(resources.displayMetrics.widthPixels.toFloat(), resources.displayMetrics.heightPixels.toFloat())
        cv.onDone = {
            runOnUiThread { hideCreditsOverlay() }
        }
        creditsView = cv

        val contentView = object : View(this) {
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                creditsView?.draw(canvas)
            }
        }
        creditsContentView = contentView

        val closeHint = TextView(this).apply {
            text = "Нажми в любом месте, чтобы закрыть"
            typeface = Typeface.MONOSPACE
            textSize = 10f
            setTextColor(Color.argb(140, 210, 220, 240))
            gravity = Gravity.CENTER
            letterSpacing = 0.04f
        }

        host.addView(contentView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        host.addView(
            closeHint,
            FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(26)
            }
        )

        val runner = object : Runnable {
            override fun run() {
                if (!creditsOverlayVisible) return

                val now = SystemClock.elapsedRealtime()
                val dt = ((now - creditsLastFrameMs) / 1000f).coerceIn(0f, 0.05f)
                creditsLastFrameMs = now

                creditsView?.update(dt)
                creditsContentView?.invalidate()
                host.postDelayed(this, 16L)
            }
        }
        creditsRunner = runner
        creditsHost = host

        host.setOnClickListener {
            creditsView?.onTap()
        }

        root.addView(host, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        host.animate().alpha(1f).setDuration(220).start()
        host.post(runner)
    }

    private fun hideCreditsOverlay() {
        if (!creditsOverlayVisible) return
        creditsOverlayVisible = false

        val host = creditsHost
        creditsHost = null
        creditsContentView = null
        creditsView = null
        creditsRunner = null

        host?.animate()?.alpha(0f)?.setDuration(180)?.withEndAction {
            root.removeView(host)
        }?.start()
    }

    private fun buildFooterLabel(text: String): TextView = TextView(this).apply {
        this.text = text
        typeface = Typeface.MONOSPACE
        textSize = 10f
        setTextColor(Color.argb(70, 100, 115, 145))
        gravity = Gravity.CENTER
        letterSpacing = 0.05f
    }

    private fun startPulseAnimation(view: View) {
        ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.78f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.02f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.02f, 1f)
        ).apply {
            duration = 2200
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        }.start()
    }

    private fun startShakeAnimation(view: View) {
        ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_X,
            0f, -5f, 5f, -3f, 3f, 0f
        ).apply {
            duration = 220
        }.start()
    }

    private fun buildSpacer(h: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, h)
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onPause() {
        super.onPause()
        hideCreditsOverlay()
    }

    private inner class GridOverlayView(context: android.content.Context) : View(context) {
        private val paint = Paint().apply {
            color = Color.argb(15, 0, 200, 160)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        override fun onDraw(canvas: Canvas) {
            val step = dp(40).toFloat()

            var x = 0f
            while (x < width) {
                canvas.drawLine(x, 0f, x, height.toFloat(), paint)
                x += step
            }

            var y = 0f
            while (y < height) {
                canvas.drawLine(0f, y, width.toFloat(), y, paint)
                y += step
            }
        }
    }
}