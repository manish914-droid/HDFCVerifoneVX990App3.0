package com.example.customneumorphic.internal.shape

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import com.example.customneumorphic.CornerFamily
import com.example.customneumorphic.LightSource
import com.example.customneumorphic.NeumorphShapeAppearanceModel
import com.example.customneumorphic.NeumorphShapeDrawable.NeumorphShapeDrawableState
import com.example.customneumorphic.internal.util.onCanvas
import com.example.customneumorphic.internal.util.withClipOut
import com.example.customneumorphic.internal.util.withTranslation
import kotlin.math.min
import kotlin.math.roundToInt

internal class FlatShape(
        private var drawableState: NeumorphShapeDrawableState
) : Shape {

    private var lightShadowBitmap: Bitmap? = null
    private var darkShadowBitmap: Bitmap? = null
    private val lightShadowDrawable = GradientDrawable()
    private val darkShadowDrawable = GradientDrawable()

    override fun setDrawableState(newDrawableState: NeumorphShapeDrawableState) {
        this.drawableState = newDrawableState
    }

    override fun draw(canvas: Canvas, outlinePath: Path) {
        canvas.withClipOut(outlinePath) {
            val lightSource = drawableState.lightSource
            val elevation = drawableState.shadowElevation
            val z = drawableState.shadowElevation + drawableState.translationZ
            val inset = drawableState.inset
            val left = inset.left.toFloat()
            val top = inset.top.toFloat()
            lightShadowBitmap?.let {
                val offsetX =
                        if (LightSource.isLeft(lightSource)) -elevation - z else -elevation + z
                val offsetY = if (LightSource.isTop(lightSource)) -elevation - z else -elevation + z
                drawBitmap(it, offsetX + left, offsetY + top, null)
            }
            darkShadowBitmap?.let {
                val offsetX =
                        if (LightSource.isLeft(lightSource)) -elevation + z else -elevation - z
                val offsetY = if (LightSource.isTop(lightSource)) -elevation + z else -elevation - z
                drawBitmap(it, offsetX + left, offsetY + top, null)
            }
        }
    }

    override fun updateShadowBitmap(bounds: Rect) {
        fun GradientDrawable.setCornerShape(shapeAppearanceModel: NeumorphShapeAppearanceModel) {
            when (shapeAppearanceModel.getCornerFamily()) {
                CornerFamily.OVAL -> {
                    shape = GradientDrawable.OVAL
                }
                CornerFamily.ROUNDED -> {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadii = shapeAppearanceModel.getCornerRadii(
                            min(bounds.width() / 2f, bounds.height() / 2f)
                    )
                }
            }
        }

        lightShadowDrawable.apply {
            setColor(drawableState.shadowColorLight)
            setCornerShape(drawableState.shapeAppearanceModel)
        }
        darkShadowDrawable.apply {
            setColor(drawableState.shadowColorDark)
            setCornerShape(drawableState.shapeAppearanceModel)
        }

        val w = bounds.width()
        val h = bounds.height()
        lightShadowDrawable.setSize(w, h)
        lightShadowDrawable.setBounds(0, 0, w, h)
        darkShadowDrawable.setSize(w, h)
        darkShadowDrawable.setBounds(0, 0, w, h)
        lightShadowBitmap = lightShadowDrawable.toBlurredBitmap(w, h)
        darkShadowBitmap = darkShadowDrawable.toBlurredBitmap(w, h)
    }

    private fun Drawable.toBlurredBitmap(w: Int, h: Int): Bitmap? {
        fun Bitmap.blurred(): Bitmap? {
            if (drawableState.inEditMode) {
                return this
            }
            return drawableState.blurProvider.blur(this)
        }

        val shadowElevation = drawableState.shadowElevation
        var width = (w + shadowElevation * 2).roundToInt()
        var height = (h + shadowElevation * 2).roundToInt()
        //By Manish Kumar
        //To check width and height must be greater than 0
        val updatewidth = if (width > 0) {
            width
        } else {
            "200".toInt().also { width = it }
        }
        val updateheight = if (height > 0) {
            height
        } else {
            "200".toInt().also { height = it }
        }

        return Bitmap.createBitmap(updatewidth, updateheight, Bitmap.Config.ARGB_8888)
                .onCanvas {
                    withTranslation(shadowElevation, shadowElevation) {
                        draw(this)
                    }
                }
                .blurred()
    }
}