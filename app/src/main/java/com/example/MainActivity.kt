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
import android.widget.TextView
import com.example.service.AdSkipAccessibilityService

class MainActivity : Activity() {

    private lateinit var tvStatusIcon: TextView
    private lateinit var tvStatusTitle: TextView
    private lateinit var tvStatusDesc: TextView
    private lateinit var btnToggleService: Button
    private lateinit var btnUnlockSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatusIcon = findViewById(R.id.tvStatusIcon)
        tvStatusTitle = findViewById(R.id.tvStatusTitle)
        tvStatusDesc = findViewById(R.id.tvStatusDesc)
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
            tvStatusIcon.text = "✓"
            tvStatusIcon.setTextColor(Color.parseColor("#10B981"))
            tvStatusTitle.text = "Đang Hoạt Động"
            tvStatusTitle.setTextColor(Color.parseColor("#10B981"))
            tvStatusDesc.text = "Dịch vụ đang chạy ngầm, tự động bỏ qua quảng cáo khi mở YouTube."

            btnToggleService.text = "ĐANG BẬT (BẤM ĐỂ CÀI ĐẶT)"
            btnToggleService.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
            btnToggleService.setTextColor(Color.BLACK)

            btnUnlockSettings.visibility = View.GONE
        } else {
            tvStatusIcon.text = "▶"
            tvStatusIcon.setTextColor(Color.parseColor("#EF4444"))
            tvStatusTitle.text = "Chưa Bật Trợ Năng"
            tvStatusTitle.setTextColor(Color.WHITE)
            tvStatusDesc.text = "Cấp quyền Hỗ trợ tiếp cận để app tự động bấm Bỏ qua quảng cáo YouTube."

            btnToggleService.text = "BẬT DỊCH VỤ TRỢ NĂNG"
            btnToggleService.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00D2FF"))
            btnToggleService.setTextColor(Color.BLACK)

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
