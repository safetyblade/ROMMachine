package com.swordfish.lemuroid.app.shared.support

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.swordfish.lemuroid.R

/** Signature surfaces for support, native recovery and setup screens. */
internal object SupportDesign {
    val midnight = 0xFF13131A.toInt()
    val panel = 0xFF20202D.toInt()
    val pink = 0xFFFF4FA3.toInt()
    val cyan = 0xFF19F5DA.toInt()
    val text = 0xFFF6F3FA.toInt()
    val muted = 0xFFC7BECF.toInt()
    private val border = 0xFF535163.toInt()

    fun Context.dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    fun column(context: Context) =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

    fun frame(
        context: Context,
        content: LinearLayout,
    ): View =
        ScrollView(context).apply {
            setBackgroundColor(midnight)
            isFillViewport = true
            isFocusableInTouchMode = true
            clipToPadding = false
            val width = context.resources.displayMetrics.widthPixels
            val side = maxOf(context.dp(16), (width - context.dp(680)) / 2)
            setPadding(side, context.dp(16), side, context.dp(28))
            addView(content, ViewGroup.LayoutParams(-1, -2))
            protectSystemBars(this, includeKeyboard = true)
            requestFocus()
        }

    fun protectSystemBars(
        view: View,
        includeKeyboard: Boolean = false,
    ) {
        val left = view.paddingLeft
        val top = view.paddingTop
        val right = view.paddingRight
        val bottom = view.paddingBottom
        val bars = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        val types = if (includeKeyboard) bars or WindowInsetsCompat.Type.ime() else bars
        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val safe = insets.getInsets(types)
            target.setPadding(left + safe.left, top + safe.top, right + safe.right, bottom + safe.bottom)
            insets
        }
    }

    fun LinearLayout.header(
        title: String,
        subtitle: String? = null,
    ) {
        card().apply {
            addView(
                ImageView(context).apply {
                    setImageResource(R.drawable.cartdeck_wordmark_native)
                    scaleType = ImageView.ScaleType.FIT_START
                    contentDescription = "CARTDECK"
                },
                LinearLayout.LayoutParams(context.dp(160), context.dp(34)),
            )
            label(title, 28, pink, bold = true)
            subtitle?.let { label(it, 14, muted) }
        }
    }

    fun LinearLayout.card(): LinearLayout =
        column(context).also {
            it.background = shape(context, panel, border)
            it.setPadding(context.dp(16), context.dp(12), context.dp(16), context.dp(12))
            addView(it, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = context.dp(12) })
        }

    fun LinearLayout.label(
        value: String,
        size: Int = 15,
        color: Int = text,
        bold: Boolean = false,
    ): TextView =
        TextView(context).also {
            it.text = value
            it.textSize = size.toFloat()
            it.setTextColor(color)
            it.setTypeface(Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", Typeface.NORMAL))
            it.setPadding(0, context.dp(4), 0, context.dp(6))
            addView(it, LinearLayout.LayoutParams(-1, -2))
        }

    fun LinearLayout.action(
        label: String,
        primary: Boolean = false,
        onClick: () -> Unit,
    ): Button =
        Button(context).also {
            it.text = label
            style(it, primary)
            it.setOnClickListener { onClick() }
            addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = context.dp(5) })
        }

    fun style(
        button: Button,
        primary: Boolean = false,
    ) {
        val context = button.context
        button.isAllCaps = false
        button.isFocusable = true
        button.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        button.minHeight = context.dp(48)
        button.setPadding(context.dp(14), context.dp(8), context.dp(14), context.dp(8))
        button.setTextColor(if (primary) midnight else text)
        button.backgroundTintList = null
        val fill = if (primary) cyan else panel
        button.background =
            StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), shape(context, fill, if (primary) pink else cyan, 3))
                addState(intArrayOf(android.R.attr.state_pressed), shape(context, fill, pink, 3))
                addState(intArrayOf(android.R.attr.state_selected), shape(context, panel, pink, 2))
                addState(intArrayOf(), shape(context, fill, if (primary) cyan else border))
            }
    }

    fun input(field: EditText) {
        val context = field.context
        field.background = shape(context, midnight, border)
        field.setPadding(context.dp(12), context.dp(12), context.dp(12), context.dp(12))
        field.setTextColor(text)
        field.setHintTextColor(muted)
        field.backgroundTintList = null
        field.highlightColor = 0x6619F5DA
    }

    fun style(dialog: AlertDialog) {
        dialog.window?.setBackgroundDrawable(shape(dialog.context, midnight, border))
        for (kind in listOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL)) {
            dialog.getButton(kind)?.let { style(it, kind == AlertDialog.BUTTON_POSITIVE) }
        }
        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(text)
        dialog.window?.decorView?.let { decor ->
            val id = dialog.context.resources.getIdentifier("alertTitle", "id", "android")
            if (id != 0) decor.findViewById<TextView>(id)?.setTextColor(pink)
        }
    }

    private fun shape(
        context: Context,
        fill: Int,
        edge: Int,
        stroke: Int = 1,
    ) = GradientDrawable().apply {
        color = ColorStateList.valueOf(fill)
        cornerRadius = context.dp(13).toFloat()
        setStroke(context.dp(stroke), edge)
    }
}
