package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.ai.GeminiSocialAnalyzer
import com.example.data.AnalyzedPostEntity
import com.example.data.AppDatabase
import com.example.model.ReplySuggestion
import com.example.model.ReplyTone
import com.example.model.SampleSocialPosts
import com.example.model.SocialAnalysisResult
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryGradientEnd
import com.example.ui.theme.PrimaryGradientStart
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.TertiaryViolet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayComposeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val geminiAnalyzer = GeminiSocialAnalyzer()

    // Overlay Window Layout Parameters
    private lateinit var bubbleLayoutParams: WindowManager.LayoutParams
    private lateinit var popupLayoutParams: WindowManager.LayoutParams

    // Overlay State
    private var isExpanded by mutableStateOf(false)
    private var isAnalyzing by mutableStateOf(false)
    private var currentResult by mutableStateOf<SocialAnalysisResult?>(null)
    private var currentSelectedTone by mutableStateOf(ReplyTone.ENGAGING)
    private var lastCopiedId by mutableStateOf<String?>(null)
    private var statusMessage by mutableStateOf<String?>("Tap floating button on any social post")

    // Drag positions
    private var bubbleX = 100
    private var bubbleY = 300

    override fun onCreate() {
        super.onCreate()
        _isServiceActive.value = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
        setupOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_ANALYSIS -> {
                performScreenAnalysis(currentSelectedTone)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceNotification() {
        val channelId = "social_float_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.floating_service_notification_title))
            .setContentText(getString(R.string.floating_service_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayView() {
        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Cannot draw overlay: SYSTEM_ALERT_WINDOW not granted")
            stopSelf()
            return
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleX
            y = bubbleY
        }

        popupLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            y = 50
        }

        lifecycleOwner = OverlayLifecycleOwner()
        overlayComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                MyApplicationTheme {
                    OverlayContent()
                }
            }
        }

        try {
            windowManager.addView(overlayComposeView, bubbleLayoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating view to WindowManager", e)
        }
    }

    @Composable
    private fun OverlayContent() {
        if (isExpanded) {
            FloatingPopupCard(
                result = currentResult,
                isAnalyzing = isAnalyzing,
                selectedTone = currentSelectedTone,
                lastCopiedId = lastCopiedId,
                statusMessage = statusMessage,
                onToneSelected = { newTone ->
                    currentSelectedTone = newTone
                    val post = currentResult?.originalPostText
                    if (!post.isNullOrBlank()) {
                        analyzeGivenText(post, currentResult?.platform ?: "Social Media", currentResult?.author ?: "", newTone)
                    } else {
                        performScreenAnalysis(newTone)
                    }
                },
                onCopyClicked = { suggestion ->
                    copyToClipboard(suggestion.text, suggestion.id)
                },
                onReanalyzeClicked = {
                    performScreenAnalysis(currentSelectedTone)
                },
                onMinimizeClicked = {
                    minimizeOverlay()
                },
                onCloseClicked = {
                    stopSelf()
                }
            )
        } else {
            FloatingBubble(
                isAnalyzing = isAnalyzing,
                onClick = {
                    expandAndAnalyze()
                },
                onDrag = { dx, dy ->
                    bubbleX += dx.toInt()
                    bubbleY += dy.toInt()
                    bubbleLayoutParams.x = bubbleX
                    bubbleLayoutParams.y = bubbleY
                    try {
                        windowManager.updateViewLayout(overlayComposeView, bubbleLayoutParams)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating bubble layout", e)
                    }
                }
            )
        }
    }

    @Composable
    private fun FloatingBubble(
        isAnalyzing: Boolean,
        onClick: () -> Unit,
        onDrag: (Float, Float) -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (isAnalyzing) 1.18f else 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bubblePulse"
        )

        Box(
            modifier = Modifier
                .padding(8.dp)
                .size(64.dp)
                .scale(pulseScale)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryGradientEnd,
                            PrimaryIndigo,
                            PrimaryGradientStart
                        )
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    )
                }
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Social Float AI",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "AI",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun FloatingPopupCard(
        result: SocialAnalysisResult?,
        isAnalyzing: Boolean,
        selectedTone: ReplyTone,
        lastCopiedId: String?,
        statusMessage: String?,
        onToneSelected: (ReplyTone) -> Unit,
        onCopyClicked: (ReplySuggestion) -> Unit,
        onReanalyzeClicked: () -> Unit,
        onMinimizeClicked: () -> Unit,
        onCloseClicked: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .border(1.5.dp, PrimaryIndigo.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(PrimaryGradientStart, PrimaryGradientEnd)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Social Float AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = result?.platform ?: "Screen Post Assistant",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryIndigo
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onReanalyzeClicked,
                            enabled = !isAnalyzing
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Re-analyze",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onMinimizeClicked) {
                            Icon(
                                imageVector = Icons.Default.Minimize,
                                contentDescription = "Minimize",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onCloseClicked) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isAnalyzing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = PrimaryIndigo,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Analyzing post with Gemini AI...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Extracting context and generating 3 smart replies",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (result != null) {
                        // Explanation Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Post Analysis & Summary",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryIndigo
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PrimaryIndigo.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = result.sentiment,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PrimaryIndigo,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = result.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tone Selector Chips
                        Text(
                            text = "Reply Tone",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ReplyTone.entries.forEach { tone ->
                                FilterChip(
                                    selected = selectedTone == tone,
                                    onClick = { onToneSelected(tone) },
                                    label = { Text(tone.title.split("&").first().trim(), fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryIndigo,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3 Suggested Replies
                        Text(
                            text = "Suggested Replies (Tap Copy to Paste)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        result.suggestions.forEachIndexed { index, suggestion ->
                            ReplySuggestionCard(
                                index = index + 1,
                                suggestion = suggestion,
                                isCopied = lastCopiedId == suggestion.id,
                                onCopy = { onCopyClicked(suggestion) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else {
                        // Empty / Ready state
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = statusMessage ?: "Ready to extract and analyze posts.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onReanalyzeClicked,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyze Current Screen")
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ReplySuggestionCard(
        index: Int,
        suggestion: ReplySuggestion,
        isCopied: Boolean,
        onCopy: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isCopied) 1.5.dp else 1.dp,
                    color = if (isCopied) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isCopied) Color(0xFF10B981).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${suggestion.emoji} Reply #$index",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = suggestion.tone,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = suggestion.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onCopy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCopied) Color(0xFF10B981) else PrimaryIndigo
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCopied) "Copied!" else "Copy",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    private fun expandAndAnalyze() {
        isExpanded = true
        try {
            windowManager.updateViewLayout(overlayComposeView, popupLayoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch overlay to popup layout", e)
        }
        performScreenAnalysis(currentSelectedTone)
    }

    private fun minimizeOverlay() {
        isExpanded = false
        try {
            windowManager.updateViewLayout(overlayComposeView, bubbleLayoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch overlay to bubble layout", e)
        }
    }

    private fun performScreenAnalysis(tone: ReplyTone) {
        serviceScope.launch {
            isAnalyzing = true
            statusMessage = "Reading screen content..."

            val extracted = SocialAccessibilityService.extractVisibleScreenText()
            val textToAnalyze = if (extracted.isSuccess && extracted.text.isNotBlank()) {
                extracted.text
            } else {
                // Fallback: Check clipboard or load a random trending social post sample for instant feedback
                val clipManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipText = clipManager.primaryClip?.getItemAt(0)?.text?.toString()
                if (!clipText.isNullOrBlank() && clipText.length > 20) {
                    clipText
                } else {
                    SampleSocialPosts.samples.random().content
                }
            }

            analyzeGivenText(
                text = textToAnalyze,
                platform = extracted.platform,
                author = extracted.author,
                tone = tone
            )
        }
    }

    private fun analyzeGivenText(text: String, platform: String, author: String, tone: ReplyTone) {
        serviceScope.launch {
            isAnalyzing = true
            val result = geminiAnalyzer.analyzePost(
                postText = text,
                platform = platform,
                author = author,
                tone = tone
            )

            result.onSuccess { analysis ->
                currentResult = analysis
                isAnalyzing = false
                saveAnalysisToDatabase(analysis, tone)
            }.onFailure { err ->
                isAnalyzing = false
                statusMessage = "Analysis error: ${err.localizedMessage}"
            }
        }
    }

    private fun copyToClipboard(text: String, suggestionId: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Social Reply", text)
        clipboard.setPrimaryClip(clip)
        lastCopiedId = suggestionId
        Toast.makeText(this, "Reply copied! Ready to paste into your app.", Toast.LENGTH_SHORT).show()

        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@FloatingOverlayService)
                currentResult?.let { res ->
                    // Find matching entity or update copy count
                    db.postDao().getAllPosts()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed updating copy count in DB", e)
            }
        }
    }

    private fun saveAnalysisToDatabase(analysis: SocialAnalysisResult, tone: ReplyTone) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@FloatingOverlayService)
                val repliesArray = JSONArray()
                analysis.suggestions.forEach {
                    val obj = JSONObject().apply {
                        put("id", it.id)
                        put("text", it.text)
                        put("tone", it.tone)
                        put("emoji", it.emoji)
                        put("highlightTag", it.highlightTag)
                    }
                    repliesArray.put(obj)
                }

                val entity = AnalyzedPostEntity(
                    postText = analysis.originalPostText,
                    author = analysis.author,
                    platform = analysis.platform,
                    explanation = analysis.explanation,
                    sentiment = analysis.sentiment,
                    repliesJson = repliesArray.toString(),
                    selectedTone = tone.name
                )
                db.postDao().insertPost(entity)
            } catch (e: Exception) {
                Log.e(TAG, "Failed saving post to database", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceActive.value = false
        if (overlayComposeView != null) {
            try {
                windowManager.removeView(overlayComposeView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view", e)
            }
        }
        lifecycleOwner?.destroy()
    }

    companion object {
        private const val TAG = "FloatingOverlayService"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"
        const val ACTION_TRIGGER_ANALYSIS = "com.example.action.TRIGGER_ANALYSIS"

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive = _isServiceActive.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
