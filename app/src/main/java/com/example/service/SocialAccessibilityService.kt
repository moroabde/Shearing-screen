package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.atomic.AtomicBoolean

class SocialAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning.set(true)
        Log.d(TAG, "SocialAccessibilityService connected")
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != packageName && !pkg.contains("launcher", ignoreCase = true)) {
            lastDetectedPackage = pkg
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "SocialAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRunning.set(false)
        Log.d(TAG, "SocialAccessibilityService destroyed")
    }

    companion object {
        private const val TAG = "SocialAccessService"
        private val isRunning = AtomicBoolean(false)
        var instance: SocialAccessibilityService? = null
            private set

        var lastDetectedPackage: String = ""
            private set

        fun isAccessibilityEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${SocialAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true) ||
                    componentName.contains(context.packageName, ignoreCase = true)
                ) {
                    return true
                }
            }
            return isRunning.get()
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }

        fun extractVisibleScreenText(): ExtractedScreenData {
            val service = instance
            if (service == null) {
                return ExtractedScreenData(
                    text = "",
                    platform = detectPlatformFromPackage(lastDetectedPackage),
                    author = "",
                    isSuccess = false,
                    error = "Accessibility Service is not active. Enable it in Settings."
                )
            }

            return try {
                val rootNode = service.rootInActiveWindow
                if (rootNode == null) {
                    return ExtractedScreenData(
                        text = "",
                        platform = detectPlatformFromPackage(lastDetectedPackage),
                        author = "",
                        isSuccess = false,
                        error = "No active screen window content could be read."
                    )
                }

                val collectedTexts = mutableListOf<NodeTextItem>()
                traverseNodeHierarchy(rootNode, collectedTexts, service.packageName)
                rootNode.recycle()

                if (collectedTexts.isEmpty()) {
                    return ExtractedScreenData(
                        text = "",
                        platform = detectPlatformFromPackage(lastDetectedPackage),
                        author = "",
                        isSuccess = false,
                        error = "No text elements found on the current screen."
                    )
                }

                // Filter out very short strings (like timestamps, icon labels)
                // and join substantive sentences to form the social post body
                val substantive = collectedTexts
                    .filter { it.text.length > 10 }
                    .sortedBy { it.top }

                val fullText = if (substantive.isNotEmpty()) {
                    substantive.joinToString("\n\n") { it.text }
                } else {
                    collectedTexts.joinToString(" ") { it.text }
                }

                val possibleAuthor = collectedTexts.firstOrNull {
                    (it.text.startsWith("@") || it.viewId.contains("username", ignoreCase = true) || it.viewId.contains("author", ignoreCase = true)) &&
                            it.text.length in 3..40
                }?.text ?: ""

                ExtractedScreenData(
                    text = fullText,
                    platform = detectPlatformFromPackage(lastDetectedPackage),
                    author = possibleAuthor,
                    isSuccess = true,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting screen text", e)
                ExtractedScreenData(
                    text = "",
                    platform = "Social Media",
                    author = "",
                    isSuccess = false,
                    error = "Extraction error: ${e.localizedMessage}"
                )
            }
        }

        private fun traverseNodeHierarchy(
            node: AccessibilityNodeInfo?,
            output: MutableList<NodeTextItem>,
            ownPackageName: String
        ) {
            if (node == null) return
            val nodePkg = node.packageName?.toString() ?: ""
            if (nodePkg == ownPackageName) return // Don't inspect our own floating overlay

            val nodeText = node.text?.toString()?.trim()
            val nodeDesc = node.contentDescription?.toString()?.trim()
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)

            val textToUse = if (!nodeText.isNullOrBlank()) nodeText else if (!nodeDesc.isNullOrBlank() && nodeDesc.length > 20) nodeDesc else null

            if (!textToUse.isNullOrBlank()) {
                val viewId = node.viewIdResourceName ?: ""
                output.add(
                    NodeTextItem(
                        text = textToUse,
                        viewId = viewId,
                        top = bounds.top,
                        left = bounds.left
                    )
                )
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    traverseNodeHierarchy(child, output, ownPackageName)
                    child.recycle()
                }
            }
        }

        private fun detectPlatformFromPackage(pkg: String): String {
            return when {
                pkg.contains("twitter", ignoreCase = true) || pkg.contains("x.android", ignoreCase = true) -> "X (Twitter)"
                pkg.contains("instagram", ignoreCase = true) -> "Instagram"
                pkg.contains("linkedin", ignoreCase = true) -> "LinkedIn"
                pkg.contains("reddit", ignoreCase = true) -> "Reddit"
                pkg.contains("facebook", ignoreCase = true) || pkg.contains("katana", ignoreCase = true) -> "Facebook"
                pkg.contains("threads", ignoreCase = true) -> "Threads"
                pkg.contains("tiktok", ignoreCase = true) || pkg.contains("musically", ignoreCase = true) -> "TikTok"
                pkg.contains("youtube", ignoreCase = true) -> "YouTube"
                pkg.contains("chrome", ignoreCase = true) || pkg.contains("browser", ignoreCase = true) -> "Web Browser"
                else -> "Social Media App"
            }
        }
    }

    private data class NodeTextItem(
        val text: String,
        val viewId: String,
        val top: Int,
        val left: Int
    )
}

data class ExtractedScreenData(
    val text: String,
    val platform: String,
    val author: String,
    val isSuccess: Boolean,
    val error: String? = null
)
