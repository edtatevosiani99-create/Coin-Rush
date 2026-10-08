package com.coinrush.game
import android.os.Bundle
import android.os.CountDownTimer
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
 private lateinit var gameArea: FrameLayout
 private lateinit var scoreText: TextView
 private lateinit var timeText: TextView
 private var score=0
 private var remainingMs=30000L
 private var timer: CountDownTimer?=null
 private val handler by lazy { android.os.Handler(mainLooper) }
 private var spawnTask: Runnable?=null

 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState); buildUi(); startGame()
 }
 private fun buildUi() {
  val root=FrameLayout(this).apply { setBackgroundColor(Color.rgb(18,18,24)) }
  val top=FrameLayout(this); root.addView(top,FrameLayout.LayoutParams(-1,80))
  scoreText=label("Счёт: 0",20f); timeText=label("Время: 30",20f)
  top.addView(scoreText,FrameLayout.LayoutParams(-2,-1).apply { gravity=Gravity.START or Gravity.CENTER_VERTICAL; leftMargin=24 })
  top.addView(timeText,FrameLayout.LayoutParams(-2,-1).apply { gravity=Gravity.END or Gravity.CENTER_VERTICAL; rightMargin=24 })
  gameArea=FrameLayout(this); root.addView(gameArea,FrameLayout.LayoutParams(-1,0).apply { topMargin=80 })
  setContentView(root)
 }
 private fun label(t:String,s:Float)=TextView(this).apply {
  text=t; textSize=s; setTextColor(Color.WHITE); typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER
 }
 private fun startGame() {
  score=0; remainingMs=30000L; updateHud(); gameArea.removeAllViews(); timer?.cancel()
  timer=object:CountDownTimer(30000L,100L) {
   override fun onTick(ms:Long){remainingMs=ms;updateHud()}
   override fun onFinish(){remainingMs=0;updateHud();stopSpawning();showGameOver()}
  }.start()
  startSpawning()
 }
 private fun startSpawning() {
  stopSpawning()
  val task=object:Runnable{
   override fun run(){
    if(remainingMs<=0)return
    spawnItem();handler.postDelayed(this,Random.nextLong(450L,900L))
   }
  }
  spawnTask=task;handler.post(task)
 }
 private fun stopSpawning(){spawnTask?.let{handler.removeCallbacks(it)};spawnTask=null}
 private fun spawnItem(){
  val roll=Random.nextInt(100)
  val type=when{roll<72->0;roll<90->1;else->2}
  val size=82
  val v=TextView(this).apply{
   text=when(type){0->"🪙";1->"💣";else->"⏱️"};textSize=42f;gravity=Gravity.CENTER
   setOnClickListener{
    when(type){0->score+=1;1->score-=2;else->remainingMs+=2000L}
    updateHud();(parent as? FrameLayout)?.removeView(this)
   }
  }
  val maxX=(gameArea.width-size).coerceAtLeast(1);val maxY=(gameArea.height-size).coerceAtLeast(1)
  gameArea.addView(v,FrameLayout.LayoutParams(size,size).apply{leftMargin=Random.nextInt(maxX);topMargin=Random.nextInt(maxY)})
 }
 private fun updateHud(){
  scoreText.text="Счёт: $score"
  timeText.text="Время: ${(remainingMs+999)/1000}"
 }
 private fun showGameOver(){
  val overlay=FrameLayout(this).apply{setBackgroundColor(Color.argb(235,18,18,24))}
  val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER}
  overlay.addView(box,FrameLayout.LayoutParams(-1,-2).apply{gravity=Gravity.CENTER;leftMargin=40;rightMargin=40})
  box.addView(label("ИГРА ОКОНЧЕНА",28f),LinearLayout.LayoutParams(-1,70))
  box.addView(label("Твой счёт: $score",23f),LinearLayout.LayoutParams(-1,70))
  box.addView(Button(this).apply{text="ИГРАТЬ СНОВА";textSize=18f;setOnClickListener{gameArea.removeView(overlay);startGame()}},LinearLayout.LayoutParams(-1,65))
  gameArea.addView(overlay,FrameLayout.LayoutParams(-1,-1))
 }
 override fun onDestroy(){timer?.cancel();stopSpawning();super.onDestroy()}
}