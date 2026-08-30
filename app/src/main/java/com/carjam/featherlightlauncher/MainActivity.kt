package com.carjam.featherlightlauncher

import android.app.Activity
import android.app.AppWidgetHost
import android.app.AppWidgetHostView
import android.app.AppWidgetManager
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.BatteryManager
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var clock: TextView
    private lateinit var drawerButton: TextView
    private var showingDrawer = false
    private var apps: List<AppInfo> = emptyList()
    private var downY = 0f
    private val widgetHostId = 1001
    private lateinit var widgetHost: AppWidgetHost
    private lateinit var widgetManager: AppWidgetManager
    private val widgetIds = mutableListOf<Int>()

    private val clockUpdater = object : Runnable {
        override fun run() {
            if (::clock.isInitialized) clock.text = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())
            handler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apps = loadApps()
        widgetManager = AppWidgetManager.getInstance(this)
        widgetHost = AppWidgetHost(this, widgetHostId)
        buildUi()
        showHome()
        handler.post(clockUpdater)
    }

    override fun onStart() {
        super.onStart()
        widgetHost.startListening()
    }

    override fun onStop() {
        widgetHost.stopListening()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized) {
            apps = loadApps()
            if (!showingDrawer) showHome()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockUpdater)
        widgetHost.stopListening()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 42 || resultCode != RESULT_OK || data == null) return
        val appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        if (appWidgetId == -1) return
        val info = widgetManager.getAppWidgetInfo(appWidgetId)
        if (info == null) {
            widgetHost.deleteAppWidgetId(appWidgetId)
            return
        }
        val hostView = widgetHost.createView(this, appWidgetId, info)
        hostView.setAppWidget(appWidgetId, info)
        widgetIds.add(appWidgetId)
        showHome()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = runCatching { WallpaperManager.getInstance(this@MainActivity).drawable }
                .getOrNull() ?: ColorDrawable(Color.rgb(248, 248, 248))
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { downY = event.rawY; false }
                    MotionEvent.ACTION_UP -> {
                        val deltaY = event.rawY - downY
                        if (!showingDrawer && deltaY < -100f) { showDrawer(); true }
                        else if (showingDrawer && deltaY > 100f) { showHome(); true }
                        else false
                    }
                    else -> false
                }
            }
        }

        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 30, 20, 8)
            setBackgroundColor(Color.argb(45, 0, 0, 0))
        }
        root.addView(overlay, LinearLayout.LayoutParams(-1, 0, 1f))
        clock = TextView(this).apply {
            textSize = 52f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setShadowLayer(8f, 0f, 2f, Color.BLACK)
        }
        overlay.addView(clock, LinearLayout.LayoutParams(-1, -2))
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        overlay.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        drawerButton = TextView(this).apply {
            text = "⌃   Apps"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setShadowLayer(5f, 0f, 1f, Color.BLACK)
            setPadding(12, 14, 12, 14)
            isClickable = true
            setOnClickListener { if (showingDrawer) showHome() else showDrawer() }
        }
        overlay.addView(drawerButton, LinearLayout.LayoutParams(-1, 58))
        setContentView(root)
    }

    private fun showHome() {
        showingDrawer = false
        drawerButton.text = "⌃   Apps"
        content.removeAllViews()
        val date = TextView(this).apply {
            text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
            textSize = 17f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            setShadowLayer(5f, 0f, 1f, Color.BLACK); setPadding(8, 0, 8, 10)
        }
        content.addView(date)
        val battery = getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val status = TextView(this).apply {
            text = "🔋 $level%"; textSize = 15f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setShadowLayer(4f, 0f, 1f, Color.BLACK)
        }
        content.addView(status)

        val widgetTitle = TextView(this).apply {
            text = "Widgets"; textSize = 14f; setTextColor(Color.WHITE); setShadowLayer(4f, 0f, 1f, Color.BLACK)
            setPadding(4, 8, 4, 4)
        }
        content.addView(widgetTitle)
        widgetIds.forEach { id ->
            val info = widgetManager.getAppWidgetInfo(id)
            if (info != null) content.addView(widgetHost.createView(this, id, info), LinearLayout.LayoutParams(-1, -2))
        }
        val widgetButton = TextView(this).apply {
            text = "+ Add widget"; textSize = 15f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setShadowLayer(4f, 0f, 1f, Color.BLACK); setPadding(8, 10, 8, 10)
            setOnClickListener { pickWidget() }
        }
        content.addView(widgetButton)

        val favorites = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, 12, 0, 16) }
        apps.take(5).forEach { app -> favorites.addView(createAppView(app, 64)) }
        content.addView(favorites, LinearLayout.LayoutParams(-1, -2))
        val settings = TextView(this).apply {
            text = "⚙ Launcher settings"; textSize = 15f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setShadowLayer(4f, 0f, 1f, Color.BLACK); setPadding(8, 8, 8, 8)
            setOnClickListener { startActivity(Intent(this@MainActivity, LauncherSettingsActivity::class.java)) }
        }
        content.addView(settings)
        content.addView(TextView(this).apply {
            text = "Swipe up for apps"; textSize = 15f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setShadowLayer(4f, 0f, 1f, Color.BLACK)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun pickWidget() {
        val appWidgetId = widgetHost.allocateAppWidgetId()
        val pick = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        startActivityForResult(pick, 42)
    }

    private fun showDrawer() {
        showingDrawer = true; drawerButton.text = "⌄   Home"; content.removeAllViews()
        val search = EditText(this).apply {
            hint = "Search apps"; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY)
            setPadding(18, 8, 18, 8); setBackgroundColor(Color.argb(100, 0, 0, 0))
        }
        content.addView(search, LinearLayout.LayoutParams(-1, 58))
        val scroll = ScrollView(this)
        val grid = GridLayout(this).apply { columnCount = 4; useDefaultMargins = true; setPadding(0, 12, 0, 20) }
        scroll.addView(grid, ViewGroup.LayoutParams(-1, -2)); content.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        fun render(filter: String = "") {
            grid.removeAllViews()
            val query = filter.trim().lowercase(Locale.getDefault())
            apps.asSequence().filter { query.isEmpty() || searchScore(it.label, query) > 0 }
                .sortedWith(compareByDescending<AppInfo> { searchScore(it.label, query) }.thenBy { it.label.lowercase(Locale.getDefault()) })
                .forEach { app ->
                    grid.addView(createAppView(app, 76), GridLayout.LayoutParams().apply {
                        width = 0; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    })
                }
        }
        render(); search.addTextChangedListener(SimpleTextWatcher { render(it) })
    }

    private fun searchScore(label: String, query: String): Int {
        if (query.isEmpty()) return 1
        val name = label.lowercase(Locale.getDefault())
        if (name == query) return 100
        if (name.startsWith(query)) return 80
        if (name.split(" ", "-", "_").any { it.startsWith(query) }) return 65
        if (name.contains(query)) return 50
        var q = 0
        for (c in name) if (q < query.length && c == query[q]) q++
        return if (q == query.length) 25 else 0
    }

    private fun createAppView(app: AppInfo, size: Int): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; isClickable = true; isFocusable = true
            setPadding(4, 6, 4, 6); setOnClickListener { launchApp(app) }
        }
        box.addView(ImageView(this).apply { setImageDrawable(app.icon); scaleType = ImageView.ScaleType.CENTER_INSIDE }, LinearLayout.LayoutParams(size, size))
        box.addView(TextView(this).apply {
            text = app.label; textSize = 12f; gravity = Gravity.CENTER; maxLines = 2
            setTextColor(Color.WHITE); setShadowLayer(4f, 0f, 1f, Color.BLACK)
        }, LinearLayout.LayoutParams(-1, -2))
        return box
    }

    private fun loadApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0).mapNotNull { resolve ->
            val info = resolve.activityInfo ?: return@mapNotNull null
            AppInfo(info.loadLabel(packageManager).toString(), info.loadIcon(packageManager), info.packageName, info.name)
        }.distinctBy { "${it.packageName}/${it.activityName}" }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun launchApp(app: AppInfo) {
        runCatching { startActivity(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER); setClassName(app.packageName, app.activityName) }) }
    }

    data class AppInfo(val label: String, val icon: android.graphics.drawable.Drawable, val packageName: String, val activityName: String)
}

private class SimpleTextWatcher(private val callback: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = callback(s?.toString().orEmpty())
    override fun afterTextChanged(s: android.text.Editable?) = Unit
}
