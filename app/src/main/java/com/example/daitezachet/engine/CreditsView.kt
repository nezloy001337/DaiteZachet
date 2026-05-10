package com.example.daitezachet.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

class CreditsView(private val screenW: Float, private val screenH: Float) {

    var scrollY: Float = screenH
    var finished: Boolean = false
    var onDone: (() -> Unit)? = null

    private val scrollSpeed = 90f

    private val titlePaint = Paint().apply {
        color       = Color.rgb(255, 220, 80)
        textSize    = 62f
        isAntiAlias = true
        textAlign   = Paint.Align.CENTER
        typeface    = Typeface.DEFAULT_BOLD
    }
    private val namePaint = Paint().apply {
        color       = Color.WHITE
        textSize    = 42f
        isAntiAlias = true
        textAlign   = Paint.Align.CENTER
        typeface    = Typeface.DEFAULT_BOLD
    }
    private val rolePaint = Paint().apply {
        color       = Color.rgb(160, 200, 255)
        textSize    = 34f
        isAntiAlias = true
        textAlign   = Paint.Align.CENTER
    }
    private val endPaint = Paint().apply {
        color       = Color.argb(200, 255, 255, 255)
        textSize    = 46f
        isAntiAlias = true
        textAlign   = Paint.Align.CENTER
        typeface    = Typeface.DEFAULT_BOLD
    }
    private val bgPaint = Paint().apply {
        color = Color.rgb(8, 8, 18)
    }

    // Список: Pair(имя, роль)
    private val credits = listOf(
        null to "— Разработчики —",
        "Юхневич Андриан"    to "1 уровень · движок · архитектура",
        "Амштейн Анна"       to "2 уровень",
        "Клименков Антон"    to "3 уровень",
        "Селивоха Станислав" to "4 уровень",
        "Ивановская Виктория" to "5 уровень",
        "Бегун Денис"        to "6 уровень",
        "Евсеева Виталина"   to "7 уровень",
        "Прищепова Полина"   to "8 уровень",
        "Антончик Денис"     to "9 уровень",
        "Зейдель Анастасия"  to "10 уровень",
        "Марченко Дарья"     to "11 уровень",
        "Бойко Тимофей"      to "12 уровень",
        "Крупенин Кирилл"    to "13 уровень · дизайн · рекорды · персонажи",
        "Кацко Денис"        to "14 уровень · фиксы · мерж · титры",
        "Лепля Михаил"       to "15 уровень"
    )

    private val lineHeight = 90f
    private val blockH: Float get() = credits.size * lineHeight + screenH * 0.5f

    fun update(dt: Float) {
        scrollY -= scrollSpeed * dt
        if (scrollY < -blockH) {
            finished = true
            onDone?.invoke()
        }
    }

    fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, screenW, screenH, bgPaint)

        var y = scrollY

        canvas.drawText("Игра окончена!", screenW / 2f, y, titlePaint)
        y += lineHeight * 1.6f

        for ((name, role) in credits) {
            if (name == null) {
                canvas.drawText(role, screenW / 2f, y, titlePaint)
                y += lineHeight * 1.4f
            } else {
                canvas.drawText(name, screenW / 2f, y, namePaint)
                y += lineHeight * 0.72f
                canvas.drawText(role, screenW / 2f, y, rolePaint)
                y += lineHeight * 1.0f
            }
        }

        y += lineHeight
        canvas.drawText("Спасибо за игру! ♥", screenW / 2f, y, endPaint)
    }
    fun onTap() {
        finished = true
        onDone?.invoke()
    }
}