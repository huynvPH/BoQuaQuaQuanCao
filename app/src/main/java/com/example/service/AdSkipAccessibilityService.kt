package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Dịch vụ tối giản thuần Android (Zero Framework, Ultra-low RAM & CPU):
 * - Chỉ lắng nghe duy nhất ứng dụng YouTube.
 * - Cooldown thông minh sau khi skip (0% CPU).
 * - Thu hồi triệt để bộ nhớ Binder IPC (recycle node).
 * - So khớp CharSequence trực tiếp không cấp phát chuỗi mới trong heap.
 */
class AdSkipAccessibilityService : AccessibilityService() {

    private var lastClickTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.packageNames = TARGET_PACKAGES
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        info.notificationTimeout = 250
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val now = System.currentTimeMillis()
        // Cooldown 4 giây sau khi bấm thành công: 0% CPU, thoát ngay lập tức
        if (now - lastClickTime < 4000L) return

        val pkg = event.packageName ?: return
        if (!pkg.contentEquals(YOUTUBE_PKG)) return

        // 1. Kiểm tra nhanh node nguồn của sự kiện trước (tránh quét toàn bộ cây giao diện)
        val source = event.source
        if (source != null) {
            try {
                if (checkAndClickNode(source)) {
                    lastClickTime = now
                    return
                }
            } finally {
                source.recycle()
            }
        }

        // 2. Quét cây giao diện chính nếu kiểm tra nhanh chưa thấy
        val root = rootInActiveWindow ?: return
        try {
            // Quét theo ID chuẩn của YouTube
            for (id in TARGET_IDS) {
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                if (!nodes.isNullOrEmpty()) {
                    try {
                        for (node in nodes) {
                            if (tryClick(node)) {
                                lastClickTime = now
                                return
                            }
                        }
                    } finally {
                        nodes.forEach { it.recycle() }
                    }
                }
            }

            // Quét theo từ khóa
            for (text in TARGET_TEXTS) {
                val nodes = root.findAccessibilityNodeInfosByText(text)
                if (!nodes.isNullOrEmpty()) {
                    try {
                        for (node in nodes) {
                            val content = node.text ?: node.contentDescription
                            if (isSkipMatch(content)) {
                                if (tryClick(node)) {
                                    lastClickTime = now
                                    return
                                }
                            }
                        }
                    } finally {
                        nodes.forEach { it.recycle() }
                    }
                }
            }
        } finally {
            root.recycle()
        }
    }

    private fun checkAndClickNode(node: AccessibilityNodeInfo): Boolean {
        val viewId = node.viewIdResourceName
        if (viewId != null) {
            for (id in TARGET_IDS) {
                if (viewId.contentEquals(id)) {
                    return tryClick(node)
                }
            }
        }
        val content = node.text ?: node.contentDescription
        if (isSkipMatch(content)) {
            return tryClick(node)
        }
        return false
    }

    private fun isSkipMatch(text: CharSequence?): Boolean {
        if (text == null || text.isEmpty()) return false
        return text.contains("Bỏ qua", ignoreCase = true) || text.contains("Skip", ignoreCase = true)
    }

    private fun tryClick(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                val clicked = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) {
                    current.recycle()
                    return true
                }
            }
            val parent = current.parent
            current.recycle()
            current = parent
        }
        return false
    }

    override fun onInterrupt() {
        isServiceRunning = false
    }

    override fun onDestroy() {
        isServiceRunning = false
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
            "Bỏ qua",
            "Skip"
        )

        @Volatile
        var isServiceRunning: Boolean = false
            private set
    }
}
