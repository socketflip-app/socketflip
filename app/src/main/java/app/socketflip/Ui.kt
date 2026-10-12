package app.socketflip

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/**
 * Keeps a screen clear of the status and navigation bars (Android 15+ draws edge to
 * edge). getInsets() is Android 11+, so Android 10 uses the older getters, which
 * still work there.
 */
fun View.padForSystemBars() = setOnApplyWindowInsetsListener { v, insets ->
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bars = insets.getInsets(WindowInsets.Type.systemBars())
        v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
    } else {
        @Suppress("DEPRECATION")
        v.setPadding(
            insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
            insets.systemWindowInsetRight, insets.systemWindowInsetBottom
        )
    }
    insets
}

/**
 * Gives a slider a name and its value in words for TalkBack, which otherwise reads
 * "seek bar, 12 percent" of the slider's range. stateDescription is Android 11+;
 * Android 10 hears both in the name.
 */
fun SeekBar.describe(name: CharSequence, state: CharSequence) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        contentDescription = name
        stateDescription = state
    } else {
        contentDescription = "$name, $state"
    }
}

/**
 * Said just before Android's VPN consent screen, whose "monitor network traffic"
 * wording is the scariest moment of the setup: what the tunnel really does, and a
 * link to the full explanation. [onContinue] opens the consent screen; [onWhy] runs
 * after the explanation page has been opened in the browser.
 */
fun Activity.explainVpnWarning(title: Int, onContinue: () -> Unit, onWhy: () -> Unit = {}) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(R.string.vpn_next_text)
        .setPositiveButton(R.string.vpn_next_continue) { _, _ -> onContinue() }
        .setNeutralButton(R.string.consent_why_safe) { _, _ ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Prefs.SAFETY_WHY_VPN_URL)))
                onWhy()
            } catch (e: ActivityNotFoundException) {
                // No browser; the dialog text already says the essentials.
            }
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}

/**
 * Text and mark colours for light and dark mode. Every one is at least 4.5:1 (WCAG AA)
 * on the card colour of its mode, which is the lower-contrast background in both modes;
 * ContrastTest checks it.
 */
object Palette {
    val LIGHT_CARD = 0xFFF1F2F5.toInt()
    val DARK_CARD = 0xFF232428.toInt()
    const val MIN_TEXT_CONTRAST = 4.5

    fun card(night: Boolean) = if (night) DARK_CARD else LIGHT_CARD
    fun warning(night: Boolean) = if (night) 0xFFEF6C00.toInt() else 0xFFB24D00.toInt()
    fun good(night: Boolean) = if (night) 0xFF66BB6A.toInt() else 0xFF2E7D32.toInt()
    fun bad(night: Boolean) = if (night) 0xFFFF8A80.toInt() else 0xFFC62828.toInt()
    fun info(night: Boolean) = if (night) 0xFF9E9E9E.toInt() else 0xFF616161.toInt()

    /** Behind the white labels of the settings preview; still mid-grey enough to show opacity. */
    val PREVIEW_PANEL = 0xFF5F5F5F.toInt()

    /**
     * The phone theme's accent colours text buttons and section titles, so it must be
     * readable on the card; some themes' accents are not, and get a fixed blue instead.
     */
    fun readableAccent(accent: Int, night: Boolean): Int =
        if (Look.contrast(accent, card(night)) >= MIN_TEXT_CONTRAST) accent
        else if (night) 0xFF90CAF9.toInt() else 0xFF1565C0.toInt()
}

/**
 * The few building blocks every screen uses: rounded cards, section titles,
 * a filled main button, quiet text buttons and captions. Plain platform views,
 * no support libraries, and colours that follow the phone's light or dark mode.
 */
class Ui(private val context: Context) {

    companion object {
        const val MAX_CONTENT_DP = 600
    }

    private val density = context.resources.displayMetrics.density
    private val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES

    val accent: Int = Palette.readableAccent(TypedValue().let {
        if (context.theme.resolveAttribute(android.R.attr.colorAccent, it, true) && it.type >= TypedValue.TYPE_FIRST_COLOR_INT)
            it.data else 0xFF1E88E5.toInt()
    }, night)
    /**
     * Text on the accent colour. Material You accents are often pale in dark mode,
     * so pick dark or light text by contrast rather than assuming white.
     */
    val onAccent: Int =
        if (Look.contrast(accent, 0xFF000000.toInt()) >= Look.contrast(accent, 0xFFFFFFFF.toInt())) 0xFF1B1B1F.toInt()
        else 0xFFFFFFFF.toInt()
    val primaryText: Int = TypedValue().let {
        if (context.theme.resolveAttribute(android.R.attr.textColorPrimary, it, true)) {
            if (it.resourceId != 0) context.getColorStateList(it.resourceId).defaultColor else it.data
        } else if (night) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
    }
    val surface: Int = Palette.card(night)
    val secondaryText: Int = if (night) 0xB3FFFFFF.toInt() else 0x99000000.toInt()
    val warning: Int = Palette.warning(night)
    val good: Int = Palette.good(night)
    val bad: Int = Palette.bad(night)
    val info: Int = Palette.info(night)

    fun dp(v: Int) = (v * density).toInt()

    /**
     * Side padding for a screen's column: 16 dp, or more on a tablet or a phone in
     * landscape, so the content is at most [MAX_CONTENT_DP] wide and centred rather than
     * stretched edge to edge. Screens are recreated on rotation, so this is kept current.
     */
    fun sidePadding(): Int = dp(maxOf(16, (context.resources.configuration.screenWidthDp - MAX_CONTENT_DP) / 2))

    /** A rounded card holding one section. */
    fun card(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(surface)
        }
        setPadding(dp(16), dp(12), dp(16), dp(12))
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(12) }
    }

    /** Small, bold, accent-coloured label at the top of a card. */
    fun sectionTitle(text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(accent)
        setTypeface(typeface, Typeface.BOLD)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setPadding(0, dp(4), 0, dp(8))
    }

    /** Big bold screen title. */
    fun title(text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(primaryText)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(4), 0, dp(4), 0)
    }

    fun caption(text: String = ""): TextView = TextView(context).apply {
        this.text = text
        setTextColor(secondaryText)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setPadding(0, dp(2), 0, dp(6))
    }

    /** The one filled, full-width button a screen leads with. */
    fun primaryButton(text: String, onClick: () -> Unit): Button = Button(context, null, 0, 0).apply {
        this.text = text
        isAllCaps = false
        setTextColor(onAccent)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(14), dp(16), dp(14))
        val shape = GradientDrawable().apply {
            cornerRadius = dp(28).toFloat()
            setColor(accent)
        }
        background = RippleDrawable(ColorStateList.valueOf(onAccent and 0x33FFFFFF), shape, null)
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(12) }
    }

    /** A quiet text button in the accent colour. */
    fun textButton(text: String, onClick: () -> Unit): Button =
        Button(context, null, android.R.attr.borderlessButtonStyle).apply {
            this.text = text
            isAllCaps = false
            setTextColor(accent)
            minWidth = 0
            minimumWidth = 0
            setPadding(dp(12), 0, dp(12), 0)
            setOnClickListener { onClick() }
        }

    /** Buttons side by side, sharing the width. */
    fun row(vararg buttons: Button): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        buttons.forEach { addView(it, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)) }
    }
}
