package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.ArchitectureGuideSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryGradientEnd
import com.example.ui.theme.PrimaryGradientStart
import com.example.ui.theme.PrimaryIndigo

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionStates()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val hasOverlay by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()
    val hasAccessibility by viewModel.hasAccessibilityPermission.collectAsStateWithLifecycle()
    val isFloatingServiceRunning by viewModel.isFloatingServiceRunning.collectAsStateWithLifecycle()

    val currentInputText by viewModel.currentInputText.collectAsStateWithLifecycle()
    val selectedTone by viewModel.selectedTone.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val analysisResult by viewModel.analysisResult.collectAsStateWithLifecycle()
    val lastCopiedId by viewModel.lastCopiedReplyId.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val historyList by viewModel.savedHistory.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
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
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Social Float AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTabIndex == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Assistant") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = PrimaryIndigo
                    )
                )

                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTabIndex == 1) Icons.Filled.Code else Icons.Outlined.Code,
                            contentDescription = "Architecture"
                        )
                    },
                    label = { Text("Architecture") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = PrimaryIndigo
                    )
                )

                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTabIndex == 2) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "History"
                        )
                    },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = PrimaryIndigo
                    )
                )
            }
        }
    ) { innerPadding ->
        when (selectedTabIndex) {
            0 -> DashboardScreen(
                hasOverlayPermission = hasOverlay,
                hasAccessibilityPermission = hasAccessibility,
                isFloatingServiceRunning = isFloatingServiceRunning,
                currentInputText = currentInputText,
                selectedTone = selectedTone,
                isAnalyzing = isAnalyzing,
                analysisResult = analysisResult,
                lastCopiedReplyId = lastCopiedId,
                errorMessage = errorMessage,
                onToggleFloatingService = { viewModel.toggleFloatingService() },
                onOpenOverlaySettings = { viewModel.openOverlaySettings() },
                onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
                onInputChange = { viewModel.setInputText(it) },
                onSelectTone = { viewModel.setSelectedTone(it) },
                onSelectSample = { viewModel.loadSamplePost(it) },
                onAnalyzePost = { viewModel.analyzePost() },
                onPasteFromClipboard = { viewModel.pasteFromClipboard() },
                onCopyReply = { viewModel.copyReply(it) },
                modifier = Modifier.padding(innerPadding)
            )
            1 -> ArchitectureGuideSheet(
                modifier = Modifier.padding(innerPadding)
            )
            2 -> HistoryScreen(
                historyList = historyList,
                lastCopiedId = lastCopiedId,
                onCopyReply = { viewModel.copyReply(it) },
                onDeleteItem = { viewModel.deleteHistoryItem(it) },
                onClearAll = { viewModel.clearAllHistory() },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

