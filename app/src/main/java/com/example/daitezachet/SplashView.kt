package com.example.daitezachet

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class SplashView(context: Context) : View(context), Runnable {

    private var thread: Thread? = null
    @Volatile private var running = false

    var onFinished: (() -> Unit)? = null

    private var totalTime = 0f
    private var cycleTime = 0f
    private var progress = 0f
    private var displayedProgress = 0f
    private var finishSent = false

    private var screenW = 0f
    private var screenH = 0f

    private val bgPaint = Paint().apply {
        color = Color.rgb(12, 70, 66)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val glowPaint = Paint().apply {
        color = Color.argb(35, 180, 255, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
        maskFilter = BlurMaskFilter(28f, BlurMaskFilter.Blur.NORMAL)
    }

    private val platformPaint = Paint().apply {
        color = Color.rgb(225, 225, 230)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val buttonBasePaint = Paint().apply {
        color = Color.rgb(145, 24, 24)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val buttonTopPaint = Paint().apply {
        color = Color.rgb(230, 58, 58)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val playerPaint = Paint().apply {
        color = Color.rgb(170, 95, 245)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val shadowPaint = Paint().apply {
        color = Color.argb(70, 0, 0, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val particlePaint = Paint().apply {
        color = Color.argb(180, 255, 255, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val barBgPaint = Paint().apply {
        color = Color.argb(80, 255, 255, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val barFillPaint = Paint().apply {
        color = Color.rgb(120, 230, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 52f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private val subTextPaint = Paint().apply {
        color = Color.argb(210, 230, 240, 255)
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    fun resume() {
        if (running) return
        running = true
        thread = Thread(this, "SplashThread").also { it.start() }
    }

    fun pause() {
        running = false
        try { thread?.join(600) } catch (_: InterruptedException) {}
        thread = null
    }

    override fun run() {
        var lastNs = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastNs = now

            update(dt)
            postInvalidate()

            val frameMs = (System.nanoTime() - now) / 1_000_000L
            val sleepMs = 16L - frameMs
            if (sleepMs > 0) Thread.sleep(sleepMs)
        }
    }

    private fun update(dt: Float) {
        totalTime += dt
        cycleTime += dt

        val cycleDuration = 0.75f
        if (cycleTime >= cycleDuration) {
            cycleTime -= cycleDuration
            progress = (progress + 0.5f).coerceAtMost(1f)
        }

        displayedProgress += (progress - displayedProgress) * (dt * 5.5f)

        if (!finishSent && progress >= 1f && abs(displayedProgress - 1f) < 0.01f) {
            finishSent = true
            post { onFinished?.invoke() }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenW = w.toFloat()
        screenH = h.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (screenW == 0f || screenH == 0f) return

        canvas.drawRect(0f, 0f, screenW, screenH, bgPaint)

        val centerX = screenW / 2f
        val centerY = screenH * 0.42f

        val titleAlpha = (180 + 75 * sin(totalTime * 2.2f)).toInt().coerceIn(100, 255)
        textPaint.alpha = titleAlpha
        canvas.drawText("Загрузка...", centerX, screenH * 0.16f, textPaint)
        canvas.drawText("Подготовка уровней", centerX, screenH * 0.22f, subTextPaint)

        canvas.drawCircle(centerX, centerY + 40f, 130f, glowPaint)

        val platformW = 300f
        val platformH = 18f
        val platformTop = centerY + 165f
        canvas.drawRoundRect(
            centerX - platformW / 2f,
            platformTop,
            centerX + platformW / 2f,
            platformTop + platformH,
            8f, 8f,
            platformPaint
        )

        val buttonW = 88f
        val buttonH = 22f
        val buttonX = centerX + 122f

        val pressPhase = ((cycleTime - 0.45f) / 0.10f).coerceIn(0f, 1f)
        val releasePhase = ((cycleTime - 0.55f) / 0.08f).coerceIn(0f, 1f)
        val buttonPress = (pressPhase - releasePhase).coerceAtLeast(0f) * 10f

        canvas.drawRoundRect(
            buttonX - buttonW / 2f,
            platformTop + 8f,
            buttonX + buttonW / 2f,
            platformTop + buttonH + 8f,
            10f, 10f,
            buttonBasePaint
        )
        canvas.drawRoundRect(
            buttonX - buttonW / 2f,
            platformTop - 2f + buttonPress,
            buttonX + buttonW / 2f,
            platformTop + buttonH - 2f + buttonPress,
            10f, 10f,
            buttonTopPaint
        )

        val bodyW = 130f
        val bodyH = 130f
        val jumpPhase = ((cycleTime - 0.25f) / 0.30f).coerceIn(0f, 1f)
        val jumpArc = sin(jumpPhase * PI).toFloat()
        val idleBob = sin(totalTime * 2.8f).toFloat() * 4f

        val bodyBottomBase = platformTop
        val bodyBottom = bodyBottomBase - jumpArc * 120f + idleBob
        val stretchY = 1f + jumpArc * 0.10f - pressPhase * 0.12f
        val stretchX = 1f - jumpArc * 0.06f + pressPhase * 0.08f

        val shiftX = jumpArc * 78f
        val left = centerX - bodyW * stretchX / 2f + shiftX
        val top = bodyBottom - bodyH * stretchY
        val right = centerX + bodyW * stretchX / 2f + shiftX
        val bottom = bodyBottom

        canvas.drawOval(left + 10f, bottom - 10f, right + 10f, bottom + 20f, shadowPaint)
        canvas.drawRect(left, top, right, bottom, playerPaint)

        drawEyes(canvas, left, top, right, bottom)
        drawParticles(canvas, buttonX, platformTop, pressPhase)
        drawBar(canvas, displayedProgress)
    }

    private fun drawEyes(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        val w = right - left
        val h = bottom - top

        val blink = when {
            cycleTime < 0.18f -> 0.10f
            cycleTime < 0.24f -> 0.55f
            cycleTime < 0.28f -> 0.18f
            else -> 0.95f
        }

        val eyePaint = Paint().apply {
            color = Color.rgb(20, 14, 28)
            style = Paint.Style.STROKE
            strokeWidth = 7f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        val eyeY = top + h * 0.46f
        val eyeW = w * 0.12f
        val eyeH = 18f * blink

        val leftCx = left + w * 0.31f
        val rightCx = left + w * 0.69f

        canvas.drawArc(RectF(leftCx - eyeW, eyeY - eyeH, leftCx + eyeW, eyeY + eyeH), 0f, 180f, false, eyePaint)
        canvas.drawArc(RectF(rightCx - eyeW, eyeY - eyeH, rightCx + eyeW, eyeY + eyeH), 0f, 180f, false, eyePaint)
    }

    private fun drawParticles(canvas: Canvas, buttonX: Float, platformTop: Float, pressPhase: Float) {
        if (pressPhase <= 0.02f) return

        for (i in 0 until 5) {
            val dx = (i - 2) * 14f
            val dy = pressPhase * (18f + i * 5f)
            canvas.drawRect(
                buttonX + dx - 3f,
                platformTop - 10f - dy,
                buttonX + dx + 3f,
                platformTop - 4f - dy,
                particlePaint
            )
        }
    }

    private fun drawBar(canvas: Canvas, progress: Float) {
        val barW = screenW * 0.56f
        val barH = 26f
        val x = screenW / 2f - barW / 2f
        val y = screenH * 0.78f

        canvas.drawRoundRect(x, y, x + barW, y + barH, 13f, 13f, barBgPaint)
        canvas.drawRoundRect(x, y, x + barW * progress, y + barH, 13f, 13f, barFillPaint)

        val segPaint = Paint().apply {
            color = Color.argb(70, 255, 255, 255)
            strokeWidth = 2f
            isAntiAlias = true
        }

        for (i in 1 until 5) {
            val sx = x + barW * i / 5f
            canvas.drawLine(sx, y + 4f, sx, y + barH - 4f, segPaint)
        }

        val percentPaint = Paint(subTextPaint).apply {
            textSize = 30f
            color = Color.WHITE
        }
        val percentText = "${(progress * 100).toInt()}%"
        canvas.drawText(percentText, screenW / 2f, y + 62f, percentPaint)
    }
}