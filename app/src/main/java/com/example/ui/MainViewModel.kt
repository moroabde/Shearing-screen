package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiSocialAnalyzer
import com.example.data.AnalyzedPostEntity
import com.example.data.AppDatabase
import com.example.model.ReplySuggestion
import com.example.model.ReplyTone
import com.example.model.SampleSocialPosts
import com.example.model.SocialAnalysisResult
import com.example.model.SocialPostSample
import com.example.service.FloatingOverlayService
import com.example.service.SocialAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val geminiAnalyzer = GeminiSocialAnalyzer()

    // Permission States
    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission = _hasOverlayPermission.asStateFlow()

    private val _hasAccessibilityPermission = MutableStateFlow(false)
    val hasAccessibilityPermission = _hasAccessibilityPermission.asStateFlow()

    // Floating Service Status
    val isFloatingServiceRunning = FloatingOverlayService.isServiceActive

    // In-App Playground / Tester State
    private val _currentInputText = MutableStateFlow(SampleSocialPosts.samples.first().content)
    val currentInputText = _currentInputText.asStateFlow()

    private val _selectedPlatform = MutableStateFlow(SampleSocialPosts.samples.first().platform)
    val selectedPlatform = _selectedPlatform.asStateFlow()

    private val _selectedAuthor = MutableStateFlow(SampleSocialPosts.samples.first().author)
    val selectedAuthor = _selectedAuthor.asStateFlow()

    private val _selectedTone = MutableStateFlow(ReplyTone.ENGAGING)
    val selectedTone = _selectedTone.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    private val _analysisResult = MutableStateFlow<SocialAnalysisResult?>(null)
    val analysisResult = _analysisResult.asStateFlow()

    private val _lastCopiedReplyId = MutableStateFlow<String?>(null)
    val lastCopiedReplyId = _lastCopiedReplyId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // History Flow from Room Database
    val savedHistory = db.postDao().getAllPosts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        refreshPermissionStates()
        // Run initial analysis on first launch so user sees immediate results
        analyzePost(SampleSocialPosts.samples.first().content, SampleSocialPosts.samples.first().platform, SampleSocialPosts.samples.first().author, ReplyTone.ENGAGING)
    }

    fun refreshPermissionStates() {
        val context = getApplication<Application>()
        _hasOverlayPermission.value = Settings.canDrawOverlays(context)
        _hasAccessibilityPermission.value = SocialAccessibilityService.isAccessibilityEnabled(context)
    }

    fun setInputText(text: String) {
        _currentInputText.value = text
    }

    fun setSelectedTone(tone: ReplyTone) {
        _selectedTone.value = tone
        if (_analysisResult.value != null && _currentInputText.value.isNotBlank()) {
            analyzePost(_currentInputText.value, _selectedPlatform.value, _selectedAuthor.value, tone)
        }
    }

    fun loadSamplePost(sample: SocialPostSample) {
        _currentInputText.value = sample.content
        _selectedPlatform.value = sample.platform
        _selectedAuthor.value = sample.author
        analyzePost(sample.content, sample.platform, sample.author, _selectedTone.value)
    }

    fun analyzePost(
        text: String = _currentInputText.value,
        platform: String = _selectedPlatform.value,
        author: String = _selectedAuthor.value,
        tone: ReplyTone = _selectedTone.value
    ) {
        if (text.isBlank()) {
            _errorMessage.value = "Please enter or paste some social post text."
            return
        }

        viewModelScope.launch {
            _isAnalyzing.value = true
            _errorMessage.value = null

            val result = geminiAnalyzer.analyzePost(
                postText = text,
                platform = platform,
                author = author,
                tone = tone
            )

            result.onSuccess { analysis ->
                _analysisResult.value = analysis
                _isAnalyzing.value = false
                savePostToDb(analysis, tone)
            }.onFailure { error ->
                _isAnalyzing.value = false
                _errorMessage.value = error.localizedMessage ?: "Analysis failed."
            }
        }
    }

    fun copyReply(suggestion: ReplySuggestion) {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AI Social Reply", suggestion.text)
        clipboard.setPrimaryClip(clip)
        _lastCopiedReplyId.value = suggestion.id
    }

    fun toggleFloatingService() {
        val context = getApplication<Application>()
        if (isFloatingServiceRunning.value) {
            FloatingOverlayService.stopService(context)
        } else {
            if (!Settings.canDrawOverlays(context)) {
                openOverlaySettings()
                return
            }
            FloatingOverlayService.startService(context)
        }
    }

    fun openOverlaySettings() {
        val context = getApplication<Application>()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openAccessibilitySettings() {
        val context = getApplication<Application>()
        SocialAccessibilityService.openAccessibilitySettings(context)
    }

    fun pasteFromClipboard() {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        if (!clip.isNullOrBlank()) {
            _currentInputText.value = clip
            _selectedPlatform.value = "Copied Post"
            analyzePost(clip, "Copied Post", "", _selectedTone.value)
        } else {
            _errorMessage.value = "Clipboard is empty."
        }
    }

    private fun savePostToDb(analysis: SocialAnalysisResult, tone: ReplyTone) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
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
            } catch (_: Exception) {}
        }
    }

    fun deleteHistoryItem(item: AnalyzedPostEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.postDao().deletePost(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            db.postDao().clearAll()
        }
    }
}
