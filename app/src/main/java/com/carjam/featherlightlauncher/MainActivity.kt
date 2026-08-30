package com.carjam.featherlightlauncher

import android.content.Intent
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : android.app.Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var content: LinearLayout
    private lateinit var clock: TextView
    private var showingDrawer = false
    private var apps: List<AppInfo> = emptyList()

    private val clockUpdater = object : Runnable {
        override fun run() {
            if (::clock.isInitialized) {
                clock.text = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())
            }
            handler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.BLACK
        apps = loadApps()
        buildShell()
        showHome()
        handler.post(clockUpdater)
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockUpdater)
        super.onDestroy()
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(248, 248, 248))
        }

        clock = TextView(this).apply {
            textSize = 54f
            setTextColor(Color.rgb(25, 25, 25))
            gravity = Gravity.CENTER
            setPadding(16, 36, 16, 4)
        }
        root.addView(clock, LinearLayout.LayoutParams(-1, -2))

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 12, 20, 12)
        }
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        val drawerButton = Button(this).apply {
            text = "Apps"
            textSize = 16f
            setOnClickListener {
                if (showingDrawer) showHome() else showDrawer()
            }
        }
        root.addView(drawerButton, LinearLayout.LayoutParams(-1, 58))
        setContentView(root)
    }

    private fun showHome() {
        showingDrawer = false
        content.removeAllViews()

        val date = TextView(this).apply {
            text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(8, 0, 8, 24)
        }
        content.addView(date)

        val battery = getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val batteryText = TextView(this).apply {
            text = "🔋 $level%"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(8, 8, 8, 20)
        }
        content.addView(batteryText)

        val favorites = TextView(this).apply {
            text = if (apps.isEmpty()) "No apps found yet" else "Tap Apps to open your app drawer"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(8, 40, 8, 8)
        }
        content.addView(favorites, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun showDrawer() {
        showingDrawer = true
        content.removeAllViews()

        val search = EditText(this).apply {
            hint = "Search apps"
            setSingleLine(true)
            setPadding(18, 8, 18, 8)
        }
        content.addView(search, LinearLayout.LayoutParams(-1, 58))

        val scroll = ScrollView(this)
        val grid = GridLayout(this).apply {
            columnCount = 4
            useDefaultMargins = true
            alignmentMode = GridLayout.ALIGN_MARGINS
            setPadding(0, 12, 0, 20)
        }
        scroll.addView(grid, ViewGroup.LayoutParams(-1, -2))
        content.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        fun render(filter: String = "") {
            grid.removeAllViews()
            apps.filter { it.label.contains(filter, ignoreCase = true) }.forEach { app ->
                val button = TextView(this).apply {
                    text = "${app.icon}\n${app.label}"
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setTextColor(Color.rgb(25, 25, 25))
                    setPadding(4, 12, 4, 12)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { launchApp(app) }
                }
                grid.addView(button, GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                })
            }
        }

        render()
        search.addTextChangedListener(SimpleTextWatcher { render(it) })
    }

    private fun loadApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { resolve ->
                val info = resolve.activityInfo ?: return@mapNotNull null
                AppInfo(
                    label = info.loadLabel(packageManager).toString(),
                    icon = "📱",
                    packageName = info.packageName,
                    activityName = info.name
                )
            }
            .distinctBy { "${it.packageName}/${it.activityName}" }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun launchApp(app: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(app.packageName, app.activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }
    }

    data class AppInfo(
        val label: String,
        val icon: String,
        val packageName: String,
        val activityName: String
    )
}

private class SimpleTextWatcher(private val callback: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = callback(s?.toString().orEmpty())
    override fun afterTextChanged(s: android.text.Editable?) = Unit
}
