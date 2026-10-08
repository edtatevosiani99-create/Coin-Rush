package com.coinrush.game

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout
    private lateinit var gameArea: FrameLayout
    private lateinit var scoreText: TextView
    private lateinit var timeText: TextView
    private var score = 0
    private var remainingMs = 30000L
    private var timer: CountDownTimer? = null
    private val handler by lazy { android.os.Handler(mainLooper) }
    private var spawnTask: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(18, 18, 24)
        window.navigationBarColor = Color.rgb(18, 18, 24)
        buildUi()
        showStartScreen()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(18, 18, 24))
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        val top = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(24, 24, 32))
        }
        root.addView(top, LinearLayout.LayoutParams(-1, 88))

        scoreText = label("Счёт: 0", 20f)
        timeText = label("Время: 30", 20f)

        top.addView(scoreText, FrameLayout.LayoutParams(-2, -1).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            leftMargin = 24
        })
        top.addView(timeText, FrameLayout.LayoutParams(-2, -1).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            rightMargin = 24
        })

        gameArea = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(18, 18, 24))
        }
        root.addView(gameArea, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
        ViewCompat.requestApplyInsets(root)
    }

    private fun label(textValue: String, size: Float) = TextView(this).apply {
        text = textValue
        textSize = size
        setTextColor(Color.WHITE)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
    }

    private fun showStartScreen() {
        timer?.cancel()
        stopSpawning()
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
        box.addView(label("Собирай монеты и следи за временем!", 17f), LinearLayout.LayoutParams(-1, 70))

        val startButton = Button(this).apply {
            text = "НАЧАТЬ ИГРУ"
            textSize = 19f
            setOnClickListener { startGame() }
        }
        box.addView(startButton, LinearLayout.LayoutParams(-1, 70))

        screen.addView(box, FrameLayout.LayoutParams(-1, -2).apply {
            gravity = Gravity.CENTER
        })
        gameArea.addView(screen, FrameLayout.LayoutParams(-1, -1))
    }

    private fun startGame() {
        score = 0
        remainingMs = 30000L
        updateHud()
        gameArea.removeAllViews()
        timer?.cancel()
        stopSpawning()

        timer = object : CountDownTimer(30000L, 100L) {
            override fun onTick(ms: Long) {
                remainingMs = ms
                updateHud()
            }

            override fun onFinish() {
                remainingMs = 0
                updateHud()
                stopSpawning()
                showGameOver()
            }
        }.start()

        handler.postDelayed({ startSpawning() }, 120)
    }

    private fun startSpawning() {
        stopSpawning()
        val task = object : Runnable {
            override fun run() {
                if (remainingMs <= 0) return
                spawnItem()
                handler.postDelayed(this, Random.nextLong(500L, 900L))
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
        val roll = Random.nextInt(100)
        val type = when {
            roll < 70 -> 0
            roll < 90 -> 1
            else -> 2
        }

        val size = 90
        val item = TextView(this).apply {
            text = when (type) {
                0 -> "🪙"
                1 -> "💣"
                else -> "⏱️"
            }
            textSize = 48f
            gravity = Gravity.CENTER
            isClickable = true
            setOnClickListener {
                when (type) {
                    0 -> score += 1
                    1 -> score -= 2
                    else -> remainingMs += 2000L
                }
                updateHud()
                (parent as? FrameLayout)?.removeView(this)
            }
        }

        val maxX = (gameArea.width - size).coerceAtLeast(1)
        val maxY = (gameArea.height - size).coerceAtLeast(1)

        gameArea.addView(item, FrameLayout.LayoutParams(size, size).apply {
            leftMargin = Random.nextInt(maxX)
            topMargin = Random.nextInt(maxY)
        })
    }

    private fun updateHud() {
        scoreText.text = "Счёт: " + score
        timeText.text = "Время: " + ((remainingMs + 999) / 1000)
    }

    private fun showGameOver() {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(240, 18, 18, 24))
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 0, 40, 0)
        }

        box.addView(label("ИГРА ОКОНЧЕНА", 28f), LinearLayout.LayoutParams(-1, 70))
        box.addView(label("Твой счёт: " + score, 23f), LinearLayout.LayoutParams(-1, 70))

        box.addView(Button(this).apply {
            text = "ИГРАТЬ СНОВА"
            textSize = 18f
            setOnClickListener { startGame() }
        }, LinearLayout.LayoutParams(-1, 65))

        overlay.addView(box, FrameLayout.LayoutParams(-1, -2).apply {
            gravity = Gravity.CENTER
        })

        gameArea.addView(overlay, FrameLayout.LayoutParams(-1, -1))
    }

    override fun onDestroy() {
        timer?.cancel()
        stopSpawning()
        super.onDestroy()
    }
}
