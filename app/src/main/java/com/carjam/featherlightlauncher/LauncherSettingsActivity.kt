package com.carjam.featherlightlauncher

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class LauncherSettingsActivity : Activity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("launcher", MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(Color.rgb(250, 250, 250))
        }

        root.addView(TextView(this).apply {
            text = "Feather Light Settings"
            textSize = 28f
            setTextColor(Color.rgb(25, 25, 25))
            setPadding(0, 0, 0, 24)
        })

        addSwitch(root, "Dim wallpaper", "dim_wallpaper", true)
        addSwitch(root, "Show battery percentage", "show_battery", true)
        addSwitch(root, "Show app labels", "show_labels", true)
        addSwitch(root, "Use swipe gestures", "gestures", true)

        root.addView(header("Dock themes", 22f, 28))
        root.addView(TextView(this).apply {
            text = "Choose a lightweight look for the recent-app dock."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 12)
        })

        addTheme(root, "Feather Glass", "glass", "Translucent minimalist dock")
        addTheme(root, "Windows Dock", "windows", "Clean Windows-style dock")
        addTheme(root, "Mac Dock", "mac", "Simple macOS-inspired dock")
        addTheme(root, "Aero Dock", "aero", "Lightweight glossy Aero-inspired dock")

        root.addView(header("App drawer columns", 16f, 24))
        val columns = SeekBar(this).apply {
            max = 4
            progress = (prefs.getInt("columns", 4) - 4).coerceIn(0, 4)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    prefs.edit().putInt("columns", progress + 4).apply()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        root.addView(columns, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }

    private fun header(textValue: String, size: Float, top: Int): TextView = TextView(this).apply {
        text = textValue
        textSize = size
        setTextColor(Color.rgb(25, 25, 25))
        setPadding(0, top, 0, 8)
    }

    private fun addTheme(root: LinearLayout, title: String, value: String, subtitle: String) {
        val row = TextView(this).apply {
            val selected = prefs.getString("dock_theme", "glass") == value
            text = if (selected) "✓ $title\n$subtitle" else "$title\n$subtitle"
            textSize = 16f
            setTextColor(Color.rgb(30, 30, 30))
            setPadding(12, 14, 12, 14)
            isClickable = true
            setOnClickListener {
                prefs.edit().putString("dock_theme", value).apply()
                recreate()
            }
        }
        root.addView(row, LinearLayout.LayoutParams(-1, -2))
    }

    private fun addSwitch(root: LinearLayout, title: String, key: String, default: Boolean) {
        val row = Switch(this).apply {
            text = title
            textSize = 16f
            setTextColor(Color.rgb(30, 30, 30))
            gravity = Gravity.CENTER_VERTICAL
            isChecked = prefs.getBoolean(key, default)
            setPadding(0, 10, 0, 10)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
            }
        }
        root.addView(row, LinearLayout.LayoutParams(-1, -2))
    }
}