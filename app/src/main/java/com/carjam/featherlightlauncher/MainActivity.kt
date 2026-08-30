package com.carjam.featherlightlauncher

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "🪶 Feather Light Launcher"
            textSize = 24f
            setPadding(32, 32, 32, 32)
        }

        setContentView(title)
    }
}
