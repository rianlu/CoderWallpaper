package com.wzl.coderwallpaper

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import java.util.*

class MainWallpaperService : WallpaperService() {

    private lateinit var mClockPaint: Paint
    private lateinit var mLogoPaint: Paint
    private var screenWidth = 0f
    private var screenHeight = 0f
    private var displayClock = true
    private var displaySecond = true
    private var autoDark = true
    private var alwaysDark = false
    private var jike_1024 = true
    private var overTime = false
    private lateinit var sp: SharedPreferences
    private var wallpaperVisible = false
    private var mHolder: SurfaceHolder? = null

    override fun onCreateEngine(): Engine? {
        return try {
            val movie = Movie.decodeStream(assets.open("wallpaper.gif"))
            WallpaperEngine(movie)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    inner class WallpaperEngine(private val movie: Movie) : Engine() {

        private val frameDuration = 20L
        private var mHandler: Handler? = null
        private val drawGIFRunnable = Runnable {
            drawGIF()
        }

        init {
            Looper.myLooper()?.let { looper ->
                mHandler = Handler(looper)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            Log.d("test", "onCreate")
            mHolder = surfaceHolder
            mClockPaint = Paint()
            mClockPaint.isAntiAlias = true
            mClockPaint.color = ContextCompat.getColor(this@MainWallpaperService, R.color.black)
            mClockPaint.textSize = sp2px(this@MainWallpaperService, 50f).toFloat()
            mClockPaint.textAlign = Paint.Align.CENTER
            mLogoPaint = Paint()
            mLogoPaint.isAntiAlias = true

            screenWidth = Resources.getSystem().displayMetrics.widthPixels.toFloat()
            screenHeight = Resources.getSystem().displayMetrics.heightPixels.toFloat()

            sp = getSharedPreferences("coder_wallpaper_settings", Context.MODE_PRIVATE)
            displayClock = sp.getBoolean("display_clock", true)
            displaySecond = sp.getBoolean("display_second", true)
            autoDark = sp.getBoolean("auto_dark", true)
            alwaysDark = sp.getBoolean("always_dark", true)
            jike_1024 = sp.getBoolean("jike_1024", true)
        }

        private fun drawGIF() {
            if (wallpaperVisible) {
                val width = movie.width().toFloat()
                val height = movie.height().toFloat()
                val pointX = (screenWidth - width) / 2
                val pointY = (screenHeight - height) / 2
                val now = SystemClock.uptimeMillis()
                // 获取画布
                val canvas = surfaceHolder.lockCanvas()
                // 保存画布
                canvas.save()
                // 绘制背景
                drawBackground(canvas)
                // 绘制时钟
                drawClock(canvas)
                // 设置画布
                movie.draw(canvas, pointX, pointY)
                // 绘制即刻 LOGO
                drawJike(canvas, pointX + width / 2, pointY + height)
                // 逐帧绘制
                val relativeMilliseconds = (now % movie.duration()).toInt()
                // 恢复画布
                canvas.restore()
                surfaceHolder.unlockCanvasAndPost(canvas)
                movie.setTime(relativeMilliseconds)
                mHandler?.removeCallbacks(drawGIFRunnable)
                mHandler?.postDelayed(drawGIFRunnable, frameDuration)
            }
        }

        private fun drawClock(canvas: Canvas) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            if (displayClock) {
                val hourString = convertToDouble(hour)
                val minuteString = convertToDouble(calendar.get(Calendar.MINUTE))
                val secondString = convertToDouble(calendar.get(Calendar.SECOND))
                val clockText = if (displaySecond) {
                    "$hourString:$minuteString:$secondString"
                } else {
                    "$hourString:$minuteString"
                }
                val clockTextRect = Rect()
                mClockPaint.getTextBounds(clockText, 0, clockText.length - 1, clockTextRect)
                val clockPointX = screenWidth / 2
                val clockPointY = screenHeight / 8
                mClockPaint.color = ContextCompat.getColor(
                    this@MainWallpaperService,
                    if (alwaysDark) {
                        if (autoDark) {
                            R.color.white
                        } else {
                            R.color.black
                        }
                    } else {
                        if (!autoDark) {
                            R.color.black
                        } else {
                            if (hour in 6..19 && autoDark) {
                                R.color.black
                            } else {
                                R.color.white
                            }
                        }
                    }
                )
                canvas.drawText(clockText, clockPointX, clockPointY, mClockPaint)
            }
        }

        private fun drawBackground(canvas: Canvas) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            // 画布颜色
            if (alwaysDark) {
                canvas.drawColor(
                    ContextCompat.getColor(
                        this@MainWallpaperService,
                        if (autoDark) {
                            R.color.black
                        } else {
                            R.color.white
                        }
                    )
                )
            } else {
                if (!autoDark) {
                    canvas.drawColor(
                        ContextCompat.getColor(this@MainWallpaperService, R.color.white)
                    )
                } else {
                    canvas.drawColor(
                        ContextCompat.getColor(
                            this@MainWallpaperService,
                            if (hour in 6..19) {
                                R.color.white
                            } else {
                                R.color.black
                            }
                        )
                    )
                }
            }
        }

        private fun drawJike(canvas: Canvas, left: Float, top: Float) {
            // 到60s刷新一次
            val jikeRefreshTime = Calendar.getInstance().get(Calendar.SECOND)
            Log.d("test", "jikeRefreshTime: $jikeRefreshTime")
            if (overTime || jikeRefreshTime == 0) {
                val beginCal: Calendar = Calendar.getInstance()
                beginCal.add(Calendar.HOUR_OF_DAY, -1)
                val endCal: Calendar = Calendar.getInstance()
                val usageStatsManager =
                    getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val stats = usageStatsManager.queryAndAggregateUsageStats(
                    beginCal.timeInMillis,
                    endCal.timeInMillis
                )
                for (stat in stats) {
                    if (stat.key == "com.ruguoapp.jike") {
                        val usageStats = stat.value
                        val totalTime = usageStats.totalTimeInForeground
                        Log.d("test", "totalTime: $totalTime")
                        // 超过了 1024 秒
                        if (totalTime >= 1024 * 1000) {
                            overTime = true
                            if (jike_1024 && wallpaperVisible) {
                                val bitmap = ResourcesCompat.getDrawable(
                                    resources,
                                    R.drawable.jike_logo,
                                    null
                                )!!.toBitmap(
                                    dip2px(this@MainWallpaperService, 24f),
                                    dip2px(this@MainWallpaperService, 24f)
                                )
                                canvas.drawBitmap(
                                    bitmap,
                                    left - bitmap.width.toFloat() / 2,
                                    top + bitmap.height.toFloat() / 2,
                                    Paint()
                                )
                            }
                        }
                        break
                    }
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Log.d("test", "onVisibilityChanged $visible")
            wallpaperVisible = visible
            if (wallpaperVisible) {
                mHandler?.post(drawGIFRunnable)
            } else {
                mHandler?.removeCallbacks(drawGIFRunnable)
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            super.onOffsetsChanged(
                xOffset,
                yOffset,
                xOffsetStep,
                yOffsetStep,
                xPixelOffset,
                yPixelOffset
            )
        }

        override fun onDestroy() {
            super.onDestroy()
            Log.d("test", "onSurfaceCreated")
            mHandler?.removeCallbacks(drawGIFRunnable)
        }

        private fun convertToDouble(number: Int): String {
            var numberString = number.toString()
            if (numberString.length == 1) {
                numberString = "0$numberString"
            }
            return numberString
        }
    }

    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    fun dip2px(context: Context, dipValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).toInt()
    }
}