package com.voiceassistant.base.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility service for performing in-app actions
 */
class VoiceAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "VoiceAccessibilityService"
        var instance: VoiceAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to process events, just perform actions
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i(TAG, "Accessibility service destroyed")
    }

    // ==================== NAVIGATION ACTIONS ====================

    fun performBack(): Boolean {
        Log.d(TAG, "Performing BACK action")
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performHome(): Boolean {
        Log.d(TAG, "Performing HOME action")
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun performRecents(): Boolean {
        Log.d(TAG, "Performing RECENTS action")
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    fun openNotifications(): Boolean {
        Log.d(TAG, "Opening notifications")
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun openQuickSettings(): Boolean {
        Log.d(TAG, "Opening quick settings")
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun performScreenshot(): Boolean {
        Log.d(TAG, "Taking screenshot")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            Log.w(TAG, "Screenshot requires Android P or higher")
            false
        }
    }

    fun performLockScreen(): Boolean {
        Log.d(TAG, "Locking screen")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            Log.w(TAG, "Lock screen requires Android P or higher")
            false
        }
    }

    // ==================== SCROLL ACTIONS ====================

    fun performScrollDown(): Boolean {
        Log.d(TAG, "Performing scroll DOWN")
        
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            val scrollableNode = findScrollableNode(rootNode)
            if (scrollableNode != null) {
                val result = scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                scrollableNode.recycle()
                rootNode.recycle()
                if (result) {
                    Log.d(TAG, "Scrolled using node action")
                    return true
                }
            }
            rootNode.recycle()
        }
        
        return performGestureScroll(false)
    }

    fun performScrollUp(): Boolean {
        Log.d(TAG, "Performing scroll UP")
        
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            val scrollableNode = findScrollableNode(rootNode)
            if (scrollableNode != null) {
                val result = scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                scrollableNode.recycle()
                rootNode.recycle()
                if (result) {
                    Log.d(TAG, "Scrolled using node action")
                    return true
                }
            }
            rootNode.recycle()
        }
        
        return performGestureScroll(true)
    }

    fun performScrollLeft(): Boolean {
        Log.d(TAG, "Performing scroll LEFT")
        return performGestureScrollHorizontal(true)
    }

    fun performScrollRight(): Boolean {
        Log.d(TAG, "Performing scroll RIGHT")
        return performGestureScrollHorizontal(false)
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) {
            return AccessibilityNodeInfo.obtain(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findScrollableNode(child)
            child.recycle()
            if (result != null) {
                return result
            }
        }
        
        return null
    }

    private fun performGestureScroll(scrollUp: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(TAG, "Gesture scrolling requires Android N or higher")
            return false
        }

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val centerX = screenWidth / 2f
        val startY: Float
        val endY: Float

        if (scrollUp) {
            startY = screenHeight * 0.3f
            endY = screenHeight * 0.7f
        } else {
            startY = screenHeight * 0.7f
            endY = screenHeight * 0.3f
        }

        val path = Path()
        path.moveTo(centerX, startY)
        path.lineTo(centerX, endY)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    private fun performGestureScrollHorizontal(scrollLeft: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val centerY = screenHeight / 2f
        val startX: Float
        val endX: Float

        if (scrollLeft) {
            startX = screenWidth * 0.3f
            endX = screenWidth * 0.7f
        } else {
            startX = screenWidth * 0.7f
            endX = screenWidth * 0.3f
        }

        val path = Path()
        path.moveTo(startX, centerY)
        path.lineTo(endX, centerY)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    // ==================== TEXT ACTIONS ====================

    fun performCopy(): Boolean {
        Log.d(TAG, "Performing COPY action")
        
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = findFocusedNode(rootNode) ?: findEditableNode(rootNode)
        
        val result = focusedNode?.performAction(AccessibilityNodeInfo.ACTION_COPY) ?: false
        
        focusedNode?.recycle()
        rootNode.recycle()
        
        return result
    }

    fun performPaste(): Boolean {
        Log.d(TAG, "Performing PASTE action")
        
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = findFocusedNode(rootNode) ?: findEditableNode(rootNode)
        
        val result = focusedNode?.performAction(AccessibilityNodeInfo.ACTION_PASTE) ?: false
        
        focusedNode?.recycle()
        rootNode.recycle()
        
        return result
    }

    fun performSelectAll(): Boolean {
        Log.d(TAG, "Performing SELECT ALL action")
        
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = findFocusedNode(rootNode) ?: findEditableNode(rootNode)
        
        val result = if (focusedNode != null) {
            val arguments = Bundle()
            arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
            arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, Int.MAX_VALUE)
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments)
        } else {
            false
        }
        
        focusedNode?.recycle()
        rootNode.recycle()
        
        return result
    }

    fun performType(text: String): Boolean {
        Log.d(TAG, "Performing TYPE action: '$text'")
        
        val rootNode = rootInActiveWindow ?: run {
            Log.w(TAG, "No root node available")
            return false
        }
        
        var targetNode = findFocusedNode(rootNode) ?: findEditableNode(rootNode)
        
        val result = if (targetNode != null) {
            targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val arguments = Bundle()
                arguments.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
                    text
                )
                targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            } else {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("text", text))
                targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            }
        } else {
            Log.w(TAG, "No editable node found")
            false
        }
        
        targetNode?.recycle()
        rootNode.recycle()
        
        return result
    }

    private fun findFocusedNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused && node.isEditable) {
            return AccessibilityNodeInfo.obtain(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFocusedNode(child)
            child.recycle()
            if (result != null) {
                return result
            }
        }
        
        return null
    }

    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) {
            return AccessibilityNodeInfo.obtain(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditableNode(child)
            child.recycle()
            if (result != null) {
                return result
            }
        }
        
        return null
    }

    // ==================== CLICK ACTIONS ====================

    fun performClickOnText(text: String): Boolean {
        Log.d(TAG, "Performing CLICK on text: '$text'")
        
        if (text.isBlank()) return false
        
        val rootNode = rootInActiveWindow ?: return false
        val targetNode = findNodeByText(rootNode, text.lowercase())
        
        val result = if (targetNode != null) {
            var clickResult = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (!clickResult) {
                val parent = targetNode.parent
                clickResult = parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
                parent?.recycle()
            }
            clickResult
        } else {
            Log.w(TAG, "Could not find node with text: '$text'")
            false
        }
        
        targetNode?.recycle()
        rootNode.recycle()
        
        return result
    }

    fun performClickAt(x: Float, y: Float): Boolean {
        Log.d(TAG, "Performing CLICK at ($x, $y)")
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }

        val path = Path()
        path.moveTo(x, y)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodeText = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        if (nodeText.contains(text) || contentDesc.contains(text)) {
            return AccessibilityNodeInfo.obtain(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByText(child, text)
            child.recycle()
            if (result != null) {
                return result
            }
        }
        
        return null
    }

    fun performSwipe(direction: String): Boolean {
        Log.d(TAG, "Performing SWIPE: $direction")
        
        return when (direction.lowercase()) {
            "up" -> performGestureScroll(false)
            "down" -> performGestureScroll(true)
            "left" -> performGestureScrollHorizontal(false)
            "right" -> performGestureScrollHorizontal(true)
            else -> false
        }
    }
}
