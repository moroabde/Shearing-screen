package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGradientEnd
import com.example.ui.theme.PrimaryGradientStart
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.TertiaryViolet

@Composable
fun ArchitectureGuideSheet(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(PrimaryGradientStart, TertiaryViolet, PrimaryGradientEnd)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "System Architecture & Logic",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Complete structural blueprint for building an on-screen floating social media AI assistant using Android & Kotlin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section 1: Floating Overlay Window
        ArchitectureCard(
            stepNumber = "1",
            icon = Icons.Default.Layers,
            title = "Floating Button & Window Management",
            badge = "WindowManager API",
            badgeColor = PrimaryIndigo,
            description = "Maintains a persistent, movable overlay on top of third-party apps without blocking user interactions.",
            keyPoints = listOf(
                "Permission: android.permission.SYSTEM_ALERT_WINDOW",
                "Service: Foreground Service with TYPE_APPLICATION_OVERLAY layout params.",
                "Flags: FLAG_NOT_FOCUSABLE prevents overlay from stealing keyboard focus until popup expands.",
                "Drag Physics: TouchListener tracks MotionEvent (ACTION_DOWN/MOVE/UP) to compute delta and drag offset, with edge-snapping."
            ),
            codeSnippet = """
val params = WindowManager.LayoutParams(
    WRAP_CONTENT, WRAP_CONTENT,
    TYPE_APPLICATION_OVERLAY,
    FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_NO_LIMITS,
    PixelFormat.TRANSLUCENT
)
windowManager.addView(floatingComposeView, params)
            """.trimIndent()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: Screen Extraction
        ArchitectureCard(
            stepNumber = "2",
            icon = Icons.Default.TouchApp,
            title = "Screen Text & Context Extraction",
            badge = "Accessibility Service",
            badgeColor = SecondaryCyan,
            description = "Extracts post text, usernames, and thread context directly from the active window hierarchy with zero OCR latency.",
            keyPoints = listOf(
                "Service: AccessibilityService with canRetrieveWindowContent='true'",
                "Node Traversal: Recursively traverses AccessibilityNodeInfo tree from rootInActiveWindow.",
                "Filtering: Skips own package overlay and status bar elements, orders visible text chronologically by vertical bounds (Rect.top).",
                "Package Identification: Detects target social app (Twitter/X, LinkedIn, Reddit, Instagram, Facebook, Threads)."
            ),
            codeSnippet = """
fun extractScreenText(node: AccessibilityNodeInfo?) {
    if (node == null || node.packageName == ownPackage) return
    node.text?.let { collectedTexts.add(it.toString()) }
    for (i in 0 until node.childCount) {
        extractScreenText(node.getChild(i))
    }
}
            """.trimIndent()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Gemini LLM Integration
        ArchitectureCard(
            stepNumber = "3",
            icon = Icons.Default.Psychology,
            title = "AI Analysis & 3 Reply Generation",
            badge = "Google Gemini 3.5 Flash",
            badgeColor = TertiaryViolet,
            description = "Sends the social post context to Gemini LLM with structured output constraints for rapid sub-second reply suggestions.",
            keyPoints = listOf(
                "Model: gemini-3.5-flash REST endpoint (v1beta) via OkHttp with 60s timeout.",
                "Prompt Blueprint: 'Analyze this post, explain it briefly, and suggest 3 engaging and appropriate reply comments.'",
                "Structured JSON: Returns explanation, sentiment analysis, key topics, and 3 distinct reply suggestions with tone tags.",
                "Tone Adaptation: Supports Engaging, Witty, Professional, Inquiring, and Supportive tones."
            ),
            codeSnippet = """
POST https://generativelanguage.googleapis.com/v1beta/
models/gemini-3.5-flash:generateContent?key={API_KEY}
{
  "contents": [{ "parts": [{ "text": userPrompt }] }],
  "generationConfig": { "responseMimeType": "application/json" }
}
            """.trimIndent()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section 4: Popup Result UI & 1-Tap Copy
        ArchitectureCard(
            stepNumber = "4",
            icon = Icons.Default.ContentCopy,
            title = "Floating Popup UI & Instant Clipboard Sync",
            badge = "Jetpack Compose",
            badgeColor = Color(0xFF10B981),
            description = "Presents explanation and generated comments in an ergonomic draggable card with one-tap clipboard copying.",
            keyPoints = listOf(
                "Composable Overlay: Uses ComposeView with custom ViewTreeLifecycleOwner inside WindowManager.",
                "1-Tap Copy: Uses android.content.ClipboardManager to sync chosen reply into primary clip.",
                "Local Persistence: Room Database records analyzed posts, timestamps, and favorite comments."
            ),
            codeSnippet = """
val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
clipboard.setPrimaryClip(ClipData.newPlainText("Reply", text))
Toast.makeText(this, "Copied! Paste in your app", Toast.LENGTH_SHORT).show()
            """.trimIndent()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section 5: Security & Permissions Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Permissions & Privacy Architecture",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "• SYSTEM_ALERT_WINDOW: Explicitly requested via Settings.ACTION_MANAGE_OVERLAY_PERMISSION.\n" +
                            "• BIND_ACCESSIBILITY_SERVICE: Declared with XML configuration and enabled manually by the user in Accessibility Settings.\n" +
                            "• No continuous logging: Accessibility is only read on-demand when the user explicitly taps the floating button.\n" +
                            "• Private & Ephemeral: Content is transmitted directly over HTTPS to Gemini API.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun ArchitectureCard(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    badge: String,
    badgeColor: Color,
    description: String,
    keyPoints: List<String>,
    codeSnippet: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                keyPoints.forEach { point ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("• ", color = badgeColor, fontWeight = FontWeight.Bold)
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F172A)
            ) {
                Text(
                    text = codeSnippet,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.padding(10.dp),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
