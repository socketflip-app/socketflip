package app.socketflip

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

/**
 * The "Add target app" list: every launchable app with its icon, a search box, and
 * apps that call themselves games (Android's own app category) grouped first.
 * Icons load as rows scroll into view, so opening the list stays quick.
 */
class AppPicker(private val activity: Activity, private val onPick: (String) -> Unit) {

    private class App(val pkg: String, val label: String, val game: Boolean, var showPkg: Boolean = false)

    private val ui = Ui(activity)
    private val pm = activity.packageManager
    private val icons = HashMap<String, Drawable?>()
    private val all: List<App>
    // Either a group heading (String) or an App.
    private var rows: List<Any> = emptyList()

    init {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val existing = Prefs.targets(activity)
        all = pm.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.applicationInfo }
            .filter { it.packageName != activity.packageName && it.packageName !in existing }
            .distinctBy { it.packageName }
            .map { App(it.packageName, Prefs.label(activity, it.packageName), it.category == ApplicationInfo.CATEGORY_GAME) }
            .sortedWith(compareBy({ !it.game }, { it.label.lowercase() }))
        // Two apps with the same name (clones, dual apps) are told apart by package.
        all.groupBy { it.label.lowercase() }.values.filter { it.size > 1 }.flatten().forEach { it.showPkg = true }
    }

    fun show() {
        val adapter = Adapter()
        val list = ListView(activity).apply {
            this.adapter = adapter
            divider = null
        }
        val empty = TextView(activity).apply {
            text = activity.getString(R.string.picker_empty)
            setPadding(ui.dp(24), ui.dp(16), ui.dp(24), ui.dp(16))
            visibility = View.GONE
        }
        val search = EditText(activity).apply {
            hint = activity.getString(R.string.picker_search)
            isSingleLine = true
        }
        fun filter(query: String) {
            val q = query.trim().lowercase()
            val hits = all.filter { q.isEmpty() || q in it.label.lowercase() || q in it.pkg.lowercase() }
            val games = hits.filter { it.game }
            val others = hits.filterNot { it.game }
            rows = if (games.isEmpty() || others.isEmpty()) hits
            else listOf(activity.getString(R.string.picker_games)) + games +
                activity.getString(R.string.picker_other) + others
            adapter.notifyDataSetChanged()
            empty.text = activity.getString(if (all.isEmpty()) R.string.picker_empty else R.string.picker_no_match)
            empty.visibility = if (hits.isEmpty()) View.VISIBLE else View.GONE
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = filter(s?.toString().orEmpty())
        })
        filter("")

        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(20), ui.dp(8), ui.dp(20), 0)
            if (all.isNotEmpty()) addView(search, MATCH_PARENT, WRAP_CONTENT)
            addView(empty, MATCH_PARENT, WRAP_CONTENT)
            // MATCH_PARENT, not WRAP_CONTENT: measuring to wrap would build every row and load every icon.
            addView(list, MATCH_PARENT, MATCH_PARENT)
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.targets_add)
            .setView(layout)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        list.setOnItemClickListener { _, _, position, _ ->
            (rows[position] as? App)?.let {
                dialog.dismiss()
                onPick(it.pkg)
            }
        }
        dialog.show()
    }

    private inner class Adapter : BaseAdapter() {
        override fun getCount() = rows.size
        override fun getItem(position: Int) = rows[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getViewTypeCount() = 2
        override fun getItemViewType(position: Int) = if (rows[position] is App) 0 else 1
        override fun areAllItemsEnabled() = false
        override fun isEnabled(position: Int) = rows[position] is App

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = rows[position]
            if (row is String) {
                val heading = convertView as? TextView ?: ui.sectionTitle("").apply {
                    setPadding(ui.dp(4), ui.dp(12), 0, ui.dp(4))
                }
                heading.text = row
                return heading
            }
            val app = row as App
            val view = convertView as? LinearLayout ?: newRow()
            val icon = view.getChildAt(0) as ImageView
            val text = view.getChildAt(1) as TextView
            icon.setImageDrawable(icons.getOrPut(app.pkg) {
                try {
                    pm.getApplicationIcon(app.pkg)
                } catch (e: Exception) {
                    null
                }
            })
            text.text = if (app.showPkg) "${app.label}\n${app.pkg}" else app.label
            return view
        }

        private fun newRow() = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = ui.dp(56)
            setPadding(ui.dp(4), ui.dp(6), ui.dp(4), ui.dp(6))
            addView(ImageView(activity), LinearLayout.LayoutParams(ui.dp(40), ui.dp(40)).apply { marginEnd = ui.dp(16) })
            addView(TextView(activity).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTextColor(ui.primaryText)
            }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        }
    }
}
