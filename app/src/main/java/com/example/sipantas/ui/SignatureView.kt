package com.example.sipantas.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import java.io.ByteArrayOutputStream

class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 8f
    }

    private val path = Path()
    private var extraBitmap: Bitmap? = null
    private var extraCanvas: Canvas? = null
    private var hasSignature = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            extraBitmap?.recycle()
            extraBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            extraCanvas = Canvas(extraBitmap!!).apply {
                drawColor(Color.TRANSPARENT)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        extraBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        canvas.drawPath(path, paint)
    }

    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionTouchEventX = event.x
        motionTouchEventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.reset()
                path.moveTo(motionTouchEventX, motionTouchEventY)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(motionTouchEventX, motionTouchEventY)
                extraCanvas?.drawPath(path, paint)
                hasSignature = true
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                extraCanvas?.drawPath(path, paint)
                path.reset()
                invalidate()
            }
        }
        return true
    }

    fun clear() {
        path.reset()
        extraCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        hasSignature = false
        invalidate()
    }

    fun isSigned(): Boolean = hasSignature

    fun getSignatureBase64(): String? {
        val bitmap = extraBitmap ?: return null
        if (!hasSignature) return null

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
