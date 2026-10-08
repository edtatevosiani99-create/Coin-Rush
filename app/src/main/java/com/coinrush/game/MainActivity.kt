package com.coinrush.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random

class RushItemView(context: android.content.Context, private val itemType: Int) : android.view.View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = (width.coerceAtMost(height) * 0.34f)

        when (itemType) {
            0 -> {
                paint.color = Color.rgb(255, 193, 7)
                canvas.drawCircle(cx, cy, r, paint)
                stroke.color = Color.rgb(180, 120, 0)
                canvas.drawCircle(cx, cy, r - 2f, stroke)
                paint.color = Color.rgb(255, 236, 120)
                paint.textSize = r * 0.95f
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("1", cx, cy + r * 0.33f, paint)
            }
            1 -> {
                paint.color = Color.rgb(35, 35, 35)
                canvas.drawCircle(cx, cy + r * 0.08f, r, paint)
                stroke.color = Color.rgb(10, 10, 10)
                canvas.drawCircle(cx, cy + r * 0.08f, r - 2f, stroke)
                stroke.color = Color.rgb(240, 170, 40)
                stroke.strokeWidth = 7f
                canvas.drawLine(cx + r * 0.15f, cy - r * 0.82f, cx + r * 0.48f, cy - r * 1.12f, stroke)
                paint.color = Color.rgb(255, 90, 40)
                canvas.drawCircle(cx + r * 0.53f, cy - r * 1.16f, r * 0.16f, paint)
            }
            else -> {
                stroke.color = Color.rgb(75, 75, 75)
                stroke.strokeWidth = 10f
                canvas.drawCircle(cx, cy, r, stroke)
                canvas.drawLine(cx, cy, cx, cy - r * 0.55f, stroke)
                canvas.drawLine(cx, cy, cx + r * 0.38f, cy + r * 0.25f, stroke)
                paint.color = Color.rgb(80, 80, 80)
                canvas.drawCircle(cx, cy, r * 0.12f, paint)
                paint.color = Color.rgb(120, 95, 255)
                canvas.drawCircle(cx, cy - r * 1.05f, r * 0.14f, paint)
            }
        }
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout
    private lateinit var gameArea: FrameLayout
    private lateinit var scoreText: TextView
    private lateinit var timeText: TextView
    private var score = 0
    private var bestScore = 0
    private val prefs by lazy { getSharedPreferences("coin_rush", MODE_PRIVATE) }
    private var remainingMs = 30000L
    private var timer: CountDownTimer? = null
    private val handler by lazy { android.os.Handler(mainLooper) }
    private var spawnTask: Runnable? = null
    private var gameRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(92, 176, 220)
        window.navigationBarColor = Color.rgb(92, 176, 220)
        bestScore = prefs.getInt("best_score", 0)
        buildUi()
        showStartScreen()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(191, 232, 255))
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        val top = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(142, 208, 242))
        }
        root.addView(top, LinearLayout.LayoutParams(-1, 88))

        scoreText = label("Score: 0", 20f).apply {
            background = roundedColor(Color.rgb(46, 134, 222), 18f)
            setPadding(22, 0, 22, 0)
            includeFontPadding = true
        }
        timeText = label("Time: 30", 20f).apply {
            background = roundedColor(Color.rgb(123, 97, 255), 18f)
            setPadding(22, 0, 22, 0)
            includeFontPadding = true
        }

        top.addView(scoreText, FrameLayout.LayoutParams(-2, -1).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            leftMargin = 24
        })
        top.addView(timeText, FrameLayout.LayoutParams(-2, -1).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            rightMargin = 24
        })

        gameArea = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(191, 232, 255))
        }
        root.addView(gameArea, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
        ViewCompat.requestApplyInsets(root)
    }

    private fun roundedColor(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun label(textValue: String, size: Float) = TextView(this).apply {
        text = textValue
        textSize = size
        setTextColor(Color.WHITE)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        includeFontPadding = true
    }

    private fun makeGameButton(textValue: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = GradientDrawable().apply {
                setColor(Color.rgb(35, 180, 85))
                cornerRadius = 22f
            }
            setPadding(20, 0, 20, 0)
            setOnClickListener { onClick() }
        }
    }

    private fun showStartScreen() {
        timer?.cancel()
        stopSpawning()
        gameRunning = false
        gameArea.removeAllViews()

        val screen = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(18, 18, 24))
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(45, 0, 45, 0)
        }

        box.addView(label("🪙 COIN RUSH", 32f), LinearLayout.LayoutParams(-1, 90))
        box.addView(label("Collect coins and beat the clock!", 17f), LinearLayout.LayoutParams(-1, 60))
        box.addView(label("Best score: " + bestScore, 18f), LinearLayout.LayoutParams(-1, 60))

        box.addView(makeGameButton("PLAY") { startGame() },
            LinearLayout.LayoutParams(-1, 72).apply { setMargins(0, 15, 0, 0) })

        screen.addView(box, FrameLayout.LayoutParams(-1, -2).apply { gravity = Gravity.CENTER })
        gameArea.addView(screen, FrameLayout.LayoutParams(-1, -1))
    }

    private fun startGame() {
        score = 0
        remainingMs = 30000L
        gameRunning = true
        updateHud()
        gameArea.removeAllViews()
        timer?.cancel()
        stopSpawning()

        startTimer(30000L)

        gameArea.post {
            if (!gameRunning) return@post
            repeat(3) { spawnItem() }
            startSpawning()
        }
    }

    private fun startTimer(durationMs: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(durationMs, 100L) {
            override fun onTick(ms: Long) {
                remainingMs = ms
                updateHud()
            }

            override fun onFinish() {
                remainingMs = 0
                gameRunning = false
                updateHud()
                stopSpawning()
                showGameOver()
            }
        }.start()
    }

    private fun restartTimer() {
        if (gameRunning && remainingMs > 0) {
            startTimer(remainingMs)
        }
    }

    private fun startSpawning() {
        stopSpawning()
        val task = object : Runnable {
            override fun run() {
                if (!gameRunning || remainingMs <= 0) return
                // No artificial item-count limit: new items keep appearing for
                // the whole game. Bombs and time bonuses remove themselves after 3s.
                if (gameArea.width > 0 && gameArea.height > 0) {
                    spawnItem()
                }
                handler.postDelayed(this, Random.nextLong(350L, 650L))
            }
        }
        spawnTask = task
        handler.post(task)
    }

    private fun stopSpawning() {
        spawnTask?.let { handler.removeCallbacks(it) }
        spawnTask = null
    }

    private fun spawnItem() {
        if (!gameRunning || gameArea.width <= 0 || gameArea.height <= 0) return

        val roll = Random.nextInt(100)
        val type = when {
            roll < 65 -> 0
            roll < 88 -> 1
            else -> 2
        }

        // Use a custom scalable icon view so the complete symbol is always visible.
        val size = (gameArea.width.coerceAtMost(gameArea.height) * 0.22f)
            .toInt().coerceIn(104, 136)

        val item = RushItemView(this, type).apply {
            isClickable = true
            setOnClickListener {
                if (!gameRunning) return@setOnClickListener
                when (type) {
                    0 -> score += 1
                    1 -> score = (score - 2).coerceAtLeast(0)
                    else -> {
                        remainingMs = (remainingMs + 2000L).coerceAtMost(30000L)
                        restartTimer()
                    }
                }
                updateHud()
                (parent as? FrameLayout)?.removeView(this)
            }
        }

        val maxX = (gameArea.width - size).coerceAtLeast(1)
        val maxY = (gameArea.height - size).coerceAtLeast(1)

        gameArea.addView(item, FrameLayout.LayoutParams(size, size).apply {
            leftMargin = Random.nextInt(0, maxX + 1)
            topMargin = Random.nextInt(0, maxY + 1)
        })

        // Bombs and time bonuses disappear after 3 seconds if not pressed.
        // Coins remain available until the end of the round.
        if (type == 1 || type == 2) {
            item.postDelayed({
                if (item.parent === gameArea && gameRunning) {
                    gameArea.removeView(item)
                }
            }, 3000L)
        }
    }

    private fun updateHud() {
        scoreText.text = "Score: " + score
        timeText.text = "Time: " + ((remainingMs + 999) / 1000)
    }

    private fun saveBestScore() {
        if (score > bestScore) {
            bestScore = score
            prefs.edit().putInt("best_score", bestScore).apply()
        }
    }

    private fun showGameOver() {
        saveBestScore()

        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(245, 191, 232, 255))
            isClickable = true
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 20, 40, 20)
        }

        box.addView(label("GAME OVER", 32f), LinearLayout.LayoutParams(-1, 90))
        box.addView(label("Your score: " + score, 23f), LinearLayout.LayoutParams(-1, 70))
        box.addView(label("Best score: " + bestScore, 20f), LinearLayout.LayoutParams(-1, 60))

        box.addView(makeGameButton("PLAY AGAIN") { startGame() },
            LinearLayout.LayoutParams(-1, 72).apply { setMargins(0, 18, 0, 0) })

        overlay.addView(box, FrameLayout.LayoutParams(-1, -2).apply { gravity = Gravity.CENTER })
        gameArea.removeAllViews()
        gameArea.addView(overlay, FrameLayout.LayoutParams(-1, -1))
    }

    override fun onDestroy() {
        timer?.cancel()
        stopSpawning()
        gameRunning = false
        super.onDestroy()
    }
}
