package com.example

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import com.example.service.AdSkipAccessibilityService

class MainActivity : Activity() {

    private lateinit var btnToggleService: Button
    private lateinit var btnUnlockSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnToggleService = findViewById(R.id.btnToggleService)
        btnUnlockSettings = findViewById(R.id.btnUnlockSettings)

        btnToggleService.setOnClickListener {
            openAccessibilitySettings()
        }

        btnUnlockSettings.setOnClickListener {
            openAppDetails()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
    }

    private fun updateUiState() {
        val isRunning = AdSkipAccessibilityService.isServiceRunning
        if (isRunning) {
            btnToggleService.text = "ĐANG HOẠT ĐỘNG"
            btnToggleService.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
            btnToggleService.setTextColor(Color.WHITE)
            btnUnlockSettings.visibility = View.GONE
        } else {
            btnToggleService.text = "BẬT TRỢ NĂNG"
            btnToggleService.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0F172A"))
            btnToggleService.setTextColor(Color.WHITE)
            btnUnlockSettings.visibility = View.VISIBLE
        }
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun openAppDetails() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {}
    }
}
