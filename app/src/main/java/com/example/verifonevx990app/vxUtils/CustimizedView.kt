package com.example.verifonevx990app.vxUtils


import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.*
import androidx.core.content.ContextCompat
import com.example.verifonevx990app.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout


class BHTextView : AppCompatTextView, View.OnTouchListener {
    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        if (hasOnClickListeners()) {
            when (p1?.action) {
                MotionEvent.ACTION_DOWN -> isSelected = true
                MotionEvent.ACTION_CANCEL -> isSelected = false
            }
        }
        return false
    }


    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        init(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(attributeSet)
    }

    constructor(context: Context) : super(context)

    private fun init(attributeSet: AttributeSet) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.BH_Font, 0, 0)
        val fontType = a.getString(R.styleable.BH_Font_fname)
        if (fontType != null) {
            val font = if (fontType == "1") {
                Typeface.createFromAsset(context.assets, "fonts/Muli-SemiBold.ttf")
            } else {
                Typeface.createFromAsset(context.assets, "fonts/Muli-Regular.ttf")
            }
            super.setTypeface(font)
        }
        a.recycle()

        setOnTouchListener(this)

    }


}


open class BHEditText : AppCompatEditText {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        init(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(attributeSet)
    }

    private fun init(attributeSet: AttributeSet) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.BH_Font, 0, 0)
        val fontType = a.getString(R.styleable.BH_Font_fname)
        if (fontType != null) {
            val font = if (fontType == "1") {
                Typeface.createFromAsset(context.assets, "fonts/Muli-SemiBold.ttf")
            } else {
                Typeface.createFromAsset(context.assets, "fonts/Muli-Regular.ttf")
            }
            super.setTypeface(font)
        }
        a.recycle()
    }

}

class AmountEditText : BHEditText {


    constructor(context: Context) : super(context) {
        implementTextListener()
    }

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        init(attributeSet)
        implementTextListener()
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(attributeSet)
        implementTextListener()
    }

    private fun init(attributeSet: AttributeSet) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.BH_Font, 0, 0)
        val fontType = a.getString(R.styleable.BH_Font_fname)
        if (fontType != null) {
            val font = if (fontType == "1") {
                Typeface.createFromAsset(context.assets, "fonts/Muli-SemiBold.ttf")
            } else {
                Typeface.createFromAsset(context.assets, "fonts/Muli-Regular.ttf")
            }
            super.setTypeface(font)
        }
        a.recycle()
    }

    private fun implementTextListener() {
        /* val tx = "%.2f".format(0f)
         setText(tx)
         requestFocus()
         setSelection(tx.length)*/
        addTextChangedListener(watcher1)
    }


    private fun doFormatting() {
        /* if (text?.isEmpty()!!) {
             val tx = "%.2f".format(0f)
             setText(tx)
             setSelection(tx.length)
         }*/
        if (text?.toString() == "0.0") {
            setText("")
        } else {
            val fl = text.toString().replace(".", "").toLong()
            val tx = "%.2f".format(fl.toDouble() / 100)
            setText(tx)
            setSelection(tx.length)
        }
        removeTextChangedListener(watcher2)
        addTextChangedListener(watcher1)
    }


    private val watcher1 = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {

        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            removeTextChangedListener(this)
            addTextChangedListener(watcher2)
            doFormatting()

        }

    }

    private val watcher2 = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

    }

}


class BHButton : AppCompatButton {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        init(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(attributeSet)
    }

    private fun init(attributeSet: AttributeSet) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.BH_Font, 0, 0)
        val fontType = a.getString(R.styleable.BH_Font_fname)
        if (fontType != null) {
            val font = if (fontType == "1") {
                Typeface.createFromAsset(context.assets, "fonts/Muli-SemiBold.ttf")
            } else {
                Typeface.createFromAsset(context.assets, "fonts/Muli-Regular.ttf")
            }
            super.setTypeface(font)
        }
        a.recycle()
    }

}


class BHRoundImageView : AppCompatImageView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    )


    private val rect: RectF by lazy { RectF(0f, 0f, this.width.toFloat(), this.height.toFloat()) }
    private val radius = 100f
    private val path = Path()


    override fun onDraw(canvas: Canvas?) {
        path.addRoundRect(rect, radius, radius, Path.Direction.CW)
        canvas?.clipPath(path)
        super.onDraw(canvas)

    }

}


class BHCheckBox : AppCompatCheckBox {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        init(attributeSet)
    }


    private fun init(attributeSet: AttributeSet) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.BH_Font, 0, 0)
        val fontType = a.getString(R.styleable.BH_Font_fname)
        if (fontType != null) {
            val font = if (fontType == "1") {
                Typeface.createFromAsset(context.assets, "fonts/Muli-SemiBold.ttf")
            } else {
                Typeface.createFromAsset(context.assets, "fonts/Muli-Regular.ttf")
            }
            super.setTypeface(font)
        }
        a.recycle()
    }

}

class BHRadioButton : AppCompatRadioButton {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        init(attributeSet)
    }


    private fun init(attributeSet: AttributeSet) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.BH_Font, 0, 0)
        val fontType = a.getString(R.styleable.BH_Font_fname)
        if (fontType != null) {
            val font = if (fontType == "1") {
                Typeface.createFromAsset(context.assets, "fonts/Muli-SemiBold.ttf")
            } else {
                Typeface.createFromAsset(context.assets, "fonts/Muli-Regular.ttf")
            }
            super.setTypeface(font)
        }
        a.recycle()
    }
}


class BHAutoCompleteTextView : AppCompatAutoCompleteTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        init(attributeSet)
    }


    private fun init(attributeSet: AttributeSet) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.BH_Font, 0, 0)
        val fontType = a.getString(R.styleable.BH_Font_fname)
        if (fontType != null) {
            val font = if (fontType == "1") {
                Typeface.createFromAsset(context.assets, "fonts/Muli-SemiBold.ttf")
            } else {
                Typeface.createFromAsset(context.assets, "fonts/Muli-Regular.ttf")
            }
            super.setTypeface(font)
        }
        a.recycle()
    }
}

class BHTextInputEditText : TextInputEditText {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        init(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(attributeSet)
    }

    private fun init(attributeSet: AttributeSet) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.BH_Font, 0, 0)
        val fontType = a.getString(R.styleable.BH_Font_fname)
        if (fontType != null) {
            val font = if (fontType == "1") {
                Typeface.createFromAsset(context.assets, "fonts/Muli-SemiBold.ttf")
            } else {
                Typeface.createFromAsset(context.assets, "fonts/Muli-Regular.ttf")
            }
            super.setTypeface(font)
        }
        a.recycle()
    }

}

class BHTextInputLayout : TextInputLayout {
    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        init(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(attributeSet)
    }

    private fun init(attributeSet: AttributeSet) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.BH_Font, 0, 0)
        val fontType = a.getString(R.styleable.BH_Font_fname)
        if (fontType != null) {
            val font = if (fontType == "1") {
                Typeface.createFromAsset(context.assets, "fonts/Muli-SemiBold.ttf")
            } else {
                Typeface.createFromAsset(context.assets, "fonts/Muli-Regular.ttf")
            }
            super.setTypeface(font)
        }
        a.recycle()
    }

}


//region==============Touch Background==========

open class TouchBackgroundD(protected val selected: Int, protected val unselected: Int) :
    View.OnTouchListener {
    protected val views = mutableListOf<View>()

    override fun onTouch(v: View, event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP) {
            v.background = ContextCompat.getDrawable(v.context, unselected)
        } else {
            v.background = ContextCompat.getDrawable(v.context, selected)
            for (e in views) {
                if (e != v) {
                    e.background = ContextCompat.getDrawable(v.context, unselected)
                }
            }
        }
        return v.onTouchEvent(event)
    }

    fun attachView(view: View) {
        views.add(view)
        view.setOnTouchListener(this)
    }

    fun detachView(view: View) {
        for (e in views) {
            if (e == view) {
                views.remove(e)
                view.setOnTouchListener(null)
                break
            }
        }
    }

}

class TouchBackgroundC(selected: Int, unselected: Int) : TouchBackgroundD(selected, unselected) {

    override fun onTouch(v: View, event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP) {
            v.setBackgroundColor(ContextCompat.getColor(v.context, unselected))
        } else {
            v.setBackgroundColor(ContextCompat.getColor(v.context, selected))
            for (e in views) {
                if (e != v) {
                    e.setBackgroundColor(ContextCompat.getColor(v.context, unselected))
                }
            }
        }
        return v.onTouchEvent(event)
    }

}

//endregion