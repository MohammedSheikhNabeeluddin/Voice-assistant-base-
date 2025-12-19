package com.voiceassistant.base.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility service for performing actions within apps
 * Enables scrolling, clicking, typing, copy/paste operations
 * 
 * All gesture methods return a Boolean indicating success/failure for proper error handling.
 */
class VoiceAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "VoiceAccessibilityService"
        var instance: VoiceAccessibilityService? = null
    }
    
    /**
     * Safely recycle an AccessibilityNodeInfo to avoid memory leaks
     */
    private fun safeRecycle(nodeInfo: AccessibilityNodeInfo?) {
        try {
            nodeInfo?.recycle()
        } catch (e: Exception) {
            Log.w(TAG, "safeRecycle: Error recycling node", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "Accessibility service created and instance set")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events if needed
        event?.let {
            Log.v(TAG, "Accessibility event received: type=${it.eventType}")
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i(TAG, "Accessibility service destroyed, instance cleared")
    }

    /**
     * Perform scroll up action
     * @return true if action was performed successfully, false otherwise
     */
    fun performScrollUp(): Boolean {
        Log.d(TAG, "performScrollUp: Attempting scroll up action")
        
        val nodeInfo = rootInActiveWindow
        if (nodeInfo == null) {
            Log.w(TAG, "performScrollUp: rootInActiveWindow is null, cannot scroll")
            return false
        }
        
        return try {
            val result = nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            Log.d(TAG, "performScrollUp: Action result=$result")
            nodeInfo.recycle()
            result
        } catch (e: Exception) {
            Log.e(TAG, "performScrollUp: Error performing scroll up", e)
            safeRecycle(nodeInfo)
            false
        }
    }

    /**
     * Perform scroll down action
     * @return true if action was performed successfully, false otherwise
     */
    fun performScrollDown(): Boolean {
        Log.d(TAG, "performScrollDown: Attempting scroll down action")
        
        val nodeInfo = rootInActiveWindow
        if (nodeInfo == null) {
            Log.w(TAG, "performScrollDown: rootInActiveWindow is null, cannot scroll")
            return false
        }
        
        return try {
            val result = nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            Log.d(TAG, "performScrollDown: Action result=$result")
            nodeInfo.recycle()
            result
        } catch (e: Exception) {
            Log.e(TAG, "performScrollDown: Error performing scroll down", e)
            safeRecycle(nodeInfo)
            false
        }
    }

    /**
     * Perform back navigation
     * @return true if action was performed successfully, false otherwise
     */
    fun performBack(): Boolean {
        Log.d(TAG, "performBack: Attempting back navigation")
        
        return try {
            val result = performGlobalAction(GLOBAL_ACTION_BACK)
            Log.d(TAG, "performBack: Action result=$result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "performBack: Error performing back action", e)
            false
        }
    }

    /**
     * Perform home navigation
     * @return true if action was performed successfully, false otherwise
     */
    fun performHome(): Boolean {
        Log.d(TAG, "performHome: Attempting home navigation")
        
        return try {
            val result = performGlobalAction(GLOBAL_ACTION_HOME)
            Log.d(TAG, "performHome: Action result=$result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "performHome: Error performing home action", e)
            false
        }
    }

    /**
     * Perform recent apps navigation
     * @return true if action was performed successfully, false otherwise
     */
    fun performRecents(): Boolean {
        Log.d(TAG, "performRecents: Attempting recents navigation")
        
        return try {
            val result = performGlobalAction(GLOBAL_ACTION_RECENTS)
            Log.d(TAG, "performRecents: Action result=$result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "performRecents: Error performing recents action", e)
            false
        }
    }

    /**
     * Copy selected text
     * @return true if action was performed successfully, false otherwise
     */
    fun performCopy(): Boolean {
        Log.d(TAG, "performCopy: Attempting copy action")
        
        val nodeInfo = rootInActiveWindow
        if (nodeInfo == null) {
            Log.w(TAG, "performCopy: rootInActiveWindow is null, cannot copy")
            return false
        }
        
        return try {
            val selectedNode = findFocusedNode(nodeInfo)
            val result = if (selectedNode != null) {
                val copyResult = selectedNode.performAction(AccessibilityNodeInfo.ACTION_COPY)
                Log.d(TAG, "performCopy: Copy action result=$copyResult")
                selectedNode.recycle()
                copyResult
            } else {
                Log.w(TAG, "performCopy: No focused node found")
                false
            }
            nodeInfo.recycle()
            result
        } catch (e: Exception) {
            Log.e(TAG, "performCopy: Error performing copy", e)
            safeRecycle(nodeInfo)
            false
        }
    }

    /**
     * Paste text
     * @return true if action was performed successfully, false otherwise
     */
    fun performPaste(): Boolean {
        Log.d(TAG, "performPaste: Attempting paste action")
        
        val nodeInfo = rootInActiveWindow
        if (nodeInfo == null) {
            Log.w(TAG, "performPaste: rootInActiveWindow is null, cannot paste")
            return false
        }
        
        return try {
            val selectedNode = findFocusedNode(nodeInfo)
            val result = if (selectedNode != null) {
                val pasteResult = selectedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                Log.d(TAG, "performPaste: Paste action result=$pasteResult")
                selectedNode.recycle()
                pasteResult
            } else {
                Log.w(TAG, "performPaste: No focused node found")
                false
            }
            nodeInfo.recycle()
            result
        } catch (e: Exception) {
            Log.e(TAG, "performPaste: Error performing paste", e)
            safeRecycle(nodeInfo)
            false
        }
    }

    /**
     * Type text into focused field
     * @return true if action was performed successfully, false otherwise
     */
    fun performType(text: String): Boolean {
        Log.d(TAG, "performType: Attempting to type '$text'")
        
        if (text.isEmpty()) {
            Log.w(TAG, "performType: Text is empty, nothing to type")
            return false
        }
        
        val nodeInfo = rootInActiveWindow
        if (nodeInfo == null) {
            Log.w(TAG, "performType: rootInActiveWindow is null, cannot type")
            return false
        }
        
        return try {
            val editableNode = findEditableNode(nodeInfo)
            
            val result = if (editableNode != null) {
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                val typeResult = editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                Log.d(TAG, "performType: Type action result=$typeResult")
                editableNode.recycle()
                typeResult
            } else {
                Log.w(TAG, "performType: No editable node found")
                false
            }
            
            nodeInfo.recycle()
            result
        } catch (e: Exception) {
            Log.e(TAG, "performType: Error performing type", e)
            safeRecycle(nodeInfo)
            false
        }
    }

    /**
     * Click at specific coordinates
     * @return true if gesture was dispatched, false otherwise
     */
    fun performClick(x: Float, y: Float): Boolean {
        Log.d(TAG, "performClick: Attempting click at ($x, $y)")
        
        return try {
            val path = Path()
            path.moveTo(x, y)
            
            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            
            val gesture = gestureBuilder.build()
            val result = dispatchGesture(gesture, null, null)
            Log.d(TAG, "performClick: Gesture dispatched=$result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "performClick: Error performing click", e)
            false
        }
    }

    /**
     * Perform swipe gesture
     * @return true if gesture was dispatched, false otherwise
     */
    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 300): Boolean {
        Log.d(TAG, "performSwipe: Attempting swipe from ($startX, $startY) to ($endX, $endY)")
        
        return try {
            val path = Path()
            path.moveTo(startX, startY)
            path.lineTo(endX, endY)
            
            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            
            val gesture = gestureBuilder.build()
            val result = dispatchGesture(gesture, null, null)
            Log.d(TAG, "performSwipe: Gesture dispatched=$result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "performSwipe: Error performing swipe", e)
            false
        }
    }

    /**
     * Find focused node in tree
     */
    private fun findFocusedNode(nodeInfo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (nodeInfo.isFocused) return nodeInfo
        
        for (i in 0 until nodeInfo.childCount) {
            val child = nodeInfo.getChild(i) ?: continue
            val focused = findFocusedNode(child)
            if (focused != null) return focused
            child.recycle()
        }
        
        return null
    }

    /**
     * Find editable text node
     */
    private fun findEditableNode(nodeInfo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (nodeInfo.isEditable) return nodeInfo
        
        for (i in 0 until nodeInfo.childCount) {
            val child = nodeInfo.getChild(i) ?: continue
            val editable = findEditableNode(child)
            if (editable != null) return editable
            child.recycle()
        }
        
        return null
    }

    /**
     * Click on node with specific text
     * @return true if node was found and clicked, false otherwise
     */
    fun clickOnText(text: String): Boolean {
        Log.d(TAG, "clickOnText: Attempting to click on text '$text'")
        
        val nodeInfo = rootInActiveWindow
        if (nodeInfo == null) {
            Log.w(TAG, "clickOnText: rootInActiveWindow is null")
            return false
        }
        
        return try {
            val targetNode = findNodeByText(nodeInfo, text)
            
            val result = if (targetNode != null) {
                val clickResult = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "clickOnText: Click action result=$clickResult")
                targetNode.recycle()
                clickResult
            } else {
                Log.w(TAG, "clickOnText: No node found with text '$text'")
                false
            }
            
            nodeInfo.recycle()
            result
        } catch (e: Exception) {
            Log.e(TAG, "clickOnText: Error clicking on text", e)
            safeRecycle(nodeInfo)
            false
        }
    }

    /**
     * Find node by text
     */
    private fun findNodeByText(nodeInfo: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (nodeInfo.text?.toString()?.contains(text, ignoreCase = true) == true) {
            return nodeInfo
        }
        
        for (i in 0 until nodeInfo.childCount) {
            val child = nodeInfo.getChild(i) ?: continue
            val found = findNodeByText(child, text)
            if (found != null) return found
            child.recycle()
        }
        
        return null
    }

    /**
     * Get all clickable nodes
     */
    fun getClickableNodes(): List<AccessibilityNodeInfo> {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        val nodeInfo = rootInActiveWindow ?: return nodes
        collectClickableNodes(nodeInfo, nodes)
        return nodes
    }

    private fun collectClickableNodes(nodeInfo: AccessibilityNodeInfo, list: MutableList<AccessibilityNodeInfo>) {
        if (nodeInfo.isClickable) {
            list.add(nodeInfo)
        }
        
        for (i in 0 until nodeInfo.childCount) {
            val child = nodeInfo.getChild(i) ?: continue
            collectClickableNodes(child, list)
        }
    }
}
