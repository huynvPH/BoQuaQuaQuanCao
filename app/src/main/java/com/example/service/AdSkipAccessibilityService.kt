package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dịch vụ tối giản, siêu nhẹ (Ultra-low RAM & CPU):
 * - Chỉ lắng nghe duy nhất ứng dụng YouTube (packageNames = com.google.android.youtube).
 * - Không xử lý âm thanh, không lưu log nặng, không chạy ngầm tốn pin.
 * - Chỉ quét đúng ID nút "skip_ad_button" và từ khóa bỏ qua khi YouTube hoạt động.
 */
class AdSkipAccessibilityService : AccessibilityService() {

    private var lastClickTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceRunning.value = true

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.packageNames = TARGET_PACKAGES
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        info.notificationTimeout = 100
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg != YOUTUBE_PKG) return

        val now = System.currentTimeMillis()
        if (now - lastClickTime < 800L) return

        val root = rootInActiveWindow ?: return
        try {
            // 1. Quét nhanh theo ID chuẩn của YouTube
            for (id in TARGET_IDS) {
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                if (!nodes.isNullOrEmpty()) {
                    for (node in nodes) {
                        if (tryClick(node)) {
                            lastClickTime = now
                            return
                        }
                    }
                }
            }

            // 2. Quét nhanh theo từ khóa tiếng Việt & tiếng Anh
            for (text in TARGET_TEXTS) {
                val nodes = root.findAccessibilityNodeInfosByText(text)
                if (!nodes.isNullOrEmpty()) {
                    for (node in nodes) {
                        val content = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
                        if (isSkipMatch(content)) {
                            if (tryClick(node)) {
                                lastClickTime = now
                                return
                            }
                        }
                    }
                }
            }
        } finally {
            root.recycle()
        }
    }

    private fun isSkipMatch(text: String): Boolean {
        val s = text.trim().lowercase()
        return s == "bỏ qua quảng cáo" || s == "bỏ qua" || s == "skip ad" || s == "skip ads" || s == "skip"
    }

    private fun tryClick(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                val clicked = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) return true
            }
            current = current.parent
        }
        return false
    }

    override fun onInterrupt() {
        _isServiceRunning.value = false
    }

    override fun onDestroy() {
        _isServiceRunning.value = false
        super.onDestroy()
    }

    companion object {
        private const val YOUTUBE_PKG = "com.google.android.youtube"
        private val TARGET_PACKAGES = arrayOf(YOUTUBE_PKG)

        private val TARGET_IDS = arrayOf(
            "com.google.android.youtube:id/skip_ad_button",
            "com.google.android.youtube:id/modern_skip_ad_button",
            "com.google.android.youtube:id/ad_skip_button"
        )

        private val TARGET_TEXTS = arrayOf(
            "Bỏ qua quảng cáo",
            "Bỏ qua",
            "Skip Ad",
            "Skip"
        )

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    }
}
