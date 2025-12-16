package com.voiceassistant.base.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility service for performing actions within apps
 * Enables scrolling, clicking, typing, copy/paste operations
 */
class VoiceAccessibilityService : AccessibilityService() {

    companion object {
        var instance: VoiceAccessibilityService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events if needed
    }

    override fun onInterrupt() {
        // Handle interrupts
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * Perform scroll up action
     */
    fun performScrollUp() {
        val nodeInfo = rootInActiveWindow ?: return
        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        nodeInfo.recycle()
    }

    /**
     * Perform scroll down action
     */
    fun performScrollDown() {
        val nodeInfo = rootInActiveWindow ?: return
        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        nodeInfo.recycle()
    }

    /**
     * Perform back navigation
     */
    fun performBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Perform home navigation
     */
    fun performHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Perform recent apps navigation
     */
    fun performRecents() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /**
     * Copy selected text
     */
    fun performCopy() {
        val nodeInfo = rootInActiveWindow ?: return
        val selectedNode = findFocusedNode(nodeInfo)
        if (selectedNode != null) {
            selectedNode.performAction(AccessibilityNodeInfo.ACTION_COPY)
            selectedNode.recycle()
        }
        nodeInfo.recycle()
    }

    /**
     * Paste text
     */
    fun performPaste() {
        val nodeInfo = rootInActiveWindow ?: return
        val selectedNode = findFocusedNode(nodeInfo)
        if (selectedNode != null) {
            selectedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            selectedNode.recycle()
        }
        nodeInfo.recycle()
    }

    /**
     * Type text into focused field
     */
    fun performType(text: String) {
        val nodeInfo = rootInActiveWindow ?: return
        val editableNode = findEditableNode(nodeInfo)
        
        editableNode?.let { node ->
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            node.recycle()
        }
        
        nodeInfo.recycle()
    }

    /**
     * Click at specific coordinates
     */
    fun performClick(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        
        val gesture = gestureBuilder.build()
        dispatchGesture(gesture, null, null)
    }

    /**
     * Perform swipe gesture
     */
    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 300) {
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        
        val gesture = gestureBuilder.build()
        dispatchGesture(gesture, null, null)
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
     */
    fun clickOnText(text: String): Boolean {
        val nodeInfo = rootInActiveWindow ?: return false
        val targetNode = findNodeByText(nodeInfo, text)
        
        return if (targetNode != null) {
            targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            targetNode.recycle()
            nodeInfo.recycle()
            true
        } else {
            nodeInfo.recycle()
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
