package com.xingchen.library.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import kotlin.math.min

class CircleProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs), OnClickListener, OnLongClickListener, OnTouchListener {
    private val rectF = RectF()
    private val paint = Paint()
    private var progress = 0f
    private var strokeWidth = 10f
    var onTakePhoto: (() -> Unit)? = null
    var onCaptureEnd: (() -> Unit)? = null
    var onCaptureStart: (() -> Unit)? = null

    init {
        paint.isAntiAlias = true
        paint.strokeWidth = strokeWidth
        setOnTouchListener(this)
        setOnClickListener(this)
        setOnLongClickListener(this)
    }

    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var radiusOuter: Float = 0f
    private var radiusInner: Float = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 根据视图大小设置圆形的边界
        val diameter = min(w, h) - strokeWidth
        val left = (w - diameter) / 2f
        val top = (h - diameter) / 2f
        rectF.set(left, top, left + diameter, top + diameter)
        // 确定圆心及内外圆半径
        centerX = width / 2f
        centerY = height / 2f
        radiusOuter = (min(w, h) - strokeWidth) / 2f
        radiusInner = (min(w, h) - strokeWidth) / 2f - strokeWidth * 3
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制内圆
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, radiusInner, paint)

        // 绘制外环
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(centerX, centerY, radiusOuter, paint)

        // 绘制进度
        paint.color = Color.GREEN
        paint.style = Paint.Style.STROKE
        canvas.drawArc(rectF, -90f, progress, false, paint)
    }

    override fun onClick(v: View?) {
        onTakePhoto?.invoke()
    }

    override fun onLongClick(v: View?): Boolean {
        onCaptureStart?.invoke()
        countdownTimer.start()
        return true
    }

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP ||
            event.action == MotionEvent.ACTION_CANCEL
        ) {
            onCaptureEnd?.invoke()
            countdownTimer.cancel()
            progress = 0f
            invalidate()
        }
        return false
    }

    /**
     * 创建倒计时计时器
     */
    private val countdownTimer = object : CountDownTimer(60000, 50) {
        override fun onTick(millisUntilFinished: Long) {
            val elapsedTime = 60000 - millisUntilFinished
            progress = elapsedTime * 360 / 60000f
            invalidate()
        }

        override fun onFinish() {
            progress = 0f
            invalidate()
        }
    }
}