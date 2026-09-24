package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Dịch vụ tối giản thuần Android (Hỗ trợ toàn diện Cửa sổ nổi / Pop-up / PiP):
 * - Quét trực tiếp qua windows hierarchy khi YouTube ở dạng Pop-up/PiP không có focus.
 * - Quét event.source và cây node con.
 * - Cờ flagIncludeNotImportantViews bắt được cả view thu nhỏ trong mini-player.
 * - Thu hồi triệt để bộ nhớ Binder IPC (recycle node).
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
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        info.notificationTimeout = 150
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val now = System.currentTimeMillis()
        // Cooldown 1.5s để không bỏ lỡ quảng cáo kép (Ad 1 of 2 -> Ad 2 of 2)
        if (now - lastClickTime < 1500L) return

        val pkg = event.packageName ?: return
        if (!pkg.contentEquals(YOUTUBE_PKG)) return

        // 1. Quét từ node nguồn của sự kiện (thường là node trong cửa sổ pop-up vừa cập nhật)
        val source = event.source
        if (source != null) {
            try {
                if (scanNodeHierarchy(source)) {
                    lastClickTime = now
                    return
                }
            } finally {
                source.recycle()
            }
        }

        // 2. Quét qua TẤT CẢ các cửa sổ hiển thị trên màn hình (Hỗ trợ Pop-up / Cửa sổ nổi / PiP)
        // Khi ở chế độ pop-up, người dùng bấm ra ngoài thì YouTube không còn là active window
        val currentWindows = windows
        if (!currentWindows.isNullOrEmpty()) {
            for (window in currentWindows) {
                val windowRoot = window.root ?: continue
                try {
                    val rootPkg = windowRoot.packageName
                    if (rootPkg == null || rootPkg.contentEquals(YOUTUBE_PKG)) {
                        if (scanNodeHierarchy(windowRoot)) {
                            lastClickTime = now
                            return
                        }
                    }
                } finally {
                    windowRoot.recycle()
                }
            }
        }

        // 3. Dự phòng cho cửa sổ chính
        val activeRoot = rootInActiveWindow
        if (activeRoot != null) {
            try {
                if (scanNodeHierarchy(activeRoot)) {
                    lastClickTime = now
                    return
                }
            } finally {
                activeRoot.recycle()
            }
        }
    }

    private fun scanNodeHierarchy(root: AccessibilityNodeInfo): Boolean {
        // Kiểm tra chính node này
        if (isTargetNode(root)) {
            if (tryClick(root)) return true
        }

        // Quét theo ID nút bấm
        for (id in TARGET_IDS) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                try {
                    for (node in nodes) {
                        if (tryClick(node)) return true
                    }
                } finally {
                    nodes.forEach { it.recycle() }
                }
            }
        }

        // Quét theo từ khóa hiển thị
        for (text in TARGET_TEXTS) {
            val nodes = root.findAccessibilityNodeInfosByText(text)
            if (!nodes.isNullOrEmpty()) {
                try {
                    for (node in nodes) {
                        val content = node.text ?: node.contentDescription
                        if (isSkipMatch(content)) {
                            if (tryClick(node)) return true
                        }
                    }
                } finally {
                    nodes.forEach { it.recycle() }
                }
            }
        }
        return false
    }

    private fun isTargetNode(node: AccessibilityNodeInfo): Boolean {
        val viewId = node.viewIdResourceName
        if (viewId != null) {
            for (id in TARGET_IDS) {
                if (viewId.contentEquals(id)) return true
            }
        }
        val content = node.text ?: node.contentDescription
        return isSkipMatch(content)
    }

    private fun isSkipMatch(text: CharSequence?): Boolean {
        if (text == null || text.isEmpty()) return false
        return text.contains("Bỏ qua", ignoreCase = true) || text.contains("Skip", ignoreCase = true)
    }

    private fun tryClick(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                val clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                parent.recycle()
                return clicked
            }
            val next = parent.parent
            parent.recycle()
            parent = next
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
            "com.google.android.youtube:id/ad_skip_button",
            "com.google.android.youtube:id/sub_skip_ad_button",
            "com.google.android.youtube:id/skip_ad_button_text",
            "com.google.android.youtube:id/modern_skip_ad_button_text",
            "com.google.android.youtube:id/action_skip",
            "com.google.android.youtube:id/countdown_skip_ad_button"
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
