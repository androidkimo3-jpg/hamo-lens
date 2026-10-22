package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.ShutterSoundPlayer
import com.example.data.db.LensDatabase
import com.example.data.media.MediaStoreHelper
import com.example.data.model.PhotoEntity
import com.example.data.prefs.AppSettings
import com.example.data.prefs.SettingsRepository
import com.example.filter.ImageFilter
import com.example.localization.AppStrings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LensDatabase.getInstance(application)
    private val photoDao = db.photoDao()
    val settingsRepo = SettingsRepository(application)
    private val shutterSoundPlayer = ShutterSoundPlayer()

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val photos: StateFlow<List<PhotoEntity>> = photoDao.getAllPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPhotoIds: StateFlow<Set<Long>> = _selectedPhotoIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _currentFilter = MutableStateFlow(ImageFilter.ORIGINAL)
    val currentFilter: StateFlow<ImageFilter> = _currentFilter.asStateFlow()

    private val _isFilterStripVisible = MutableStateFlow(false)
    val isFilterStripVisible: StateFlow<Boolean> = _isFilterStripVisible.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _facing = MutableStateFlow("back") // "back" or "front"
    val facing: StateFlow<String> = _facing.asStateFlow()

    private val _flashEnabled = MutableStateFlow(false)
    val flashEnabled: StateFlow<Boolean> = _flashEnabled.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0) // 0, 3, 10
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _countdown = MutableStateFlow<Int?>(null)
    val countdown: StateFlow<Int?> = _countdown.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _showScreenFlash = MutableStateFlow(false)
    val showScreenFlash: StateFlow<Boolean> = _showScreenFlash.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isCameraPermissionGranted = MutableStateFlow(false)
    val isCameraPermissionGranted: StateFlow<Boolean> = _isCameraPermissionGranted.asStateFlow()

    private val _isCameraPermissionBlocked = MutableStateFlow(false)
    val isCameraPermissionBlocked: StateFlow<Boolean> = _isCameraPermissionBlocked.asStateFlow()

    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            settings.collect { current ->
                _facing.value = current.defaultCamera
            }
        }
    }

    fun setCameraPermissionState(granted: Boolean, blocked: Boolean = false) {
        _isCameraPermissionGranted.value = granted
        _isCameraPermissionBlocked.value = blocked
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.value = message
            delay(2200)
            if (_toastMessage.value == message) {
                _toastMessage.value = null
            }
        }
    }

    fun toggleFlash() {
        _flashEnabled.value = !_flashEnabled.value
    }

    fun cycleTimer() {
        _timerSeconds.value = when (_timerSeconds.value) {
            0 -> 3
            3 -> 10
            else -> 0
        }
    }

    fun toggleGrid() {
        viewModelScope.launch {
            settingsRepo.updateGrid(!settings.value.gridEnabled)
        }
    }

    fun toggleFilterStrip() {
        _isFilterStripVisible.value = !_isFilterStripVisible.value
    }

    fun selectFilter(filter: ImageFilter) {
        _currentFilter.value = filter
    }

    fun setZoom(ratio: Float) {
        _zoomRatio.value = ratio.coerceIn(1.0f, 4.0f)
    }

    fun flipCamera() {
        _facing.value = if (_facing.value == "back") "front" else "back"
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _countdown.value = null
        _isCapturing.value = false
    }

    fun triggerShutter(onCapture: () -> Unit) {
        if (_isCapturing.value) {
            // Tapping shutter during countdown cancels it
            cancelCountdown()
            return
        }

        val timer = _timerSeconds.value
        if (timer > 0) {
            _isCapturing.value = true
            countdownJob = viewModelScope.launch {
                for (remaining in timer downTo 1) {
                    _countdown.value = remaining
                    delay(1000)
                }
                _countdown.value = null
                executeCapture(onCapture)
            }
        } else {
            executeCapture(onCapture)
        }
    }

    private fun executeCapture(onCapture: () -> Unit) {
        viewModelScope.launch {
            _isCapturing.value = true
            val isFront = _facing.value == "front"

            // Shutter sound if enabled
            if (settings.value.shutterSound) {
                shutterSoundPlayer.play()
            }

            // White screen flash on front camera or simulated flash
            if (isFront || _flashEnabled.value) {
                _showScreenFlash.value = true
                delay(200)
                _showScreenFlash.value = false
            }

            onCapture()
        }
    }

    fun onBitmapCaptured(rawBitmap: Bitmap, onSaved: (PhotoEntity) -> Unit) {
        viewModelScope.launch {
            try {
                // Apply active image filter to bitmap
                val filteredBitmap = ImageFilter.applyFilter(rawBitmap, _currentFilter.value)

                val qualityPercent = if (settings.value.photoQuality == "high") 95 else 85
                val timestamp = System.currentTimeMillis()

                val entity = MediaStoreHelper.savePhotoToPictures(
                    context = getApplication(),
                    bitmap = filteredBitmap,
                    qualityPercent = qualityPercent,
                    timestamp = timestamp
                )

                photoDao.insertPhoto(entity)
                val lang = settings.value.language
                showToast(AppStrings.t("saved", lang))
                onSaved(entity)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isCapturing.value = false
            }
        }
    }

    // Gallery operations
    fun togglePhotoSelection(id: Long) {
        val current = _selectedPhotoIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedPhotoIds.value = current
    }

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedPhotoIds.value = emptySet()
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedPhotoIds.value = emptySet()
    }

    fun downloadPhotos(context: Context, photosToDownload: List<PhotoEntity>) {
        viewModelScope.launch {
            var success = false
            for (p in photosToDownload) {
                val ok = MediaStoreHelper.exportToDownloads(context, p.uriString.toUri(), p.fileName)
                if (ok) success = true
                delay(300)
            }
            if (success) {
                showToast(AppStrings.t("saved", settings.value.language))
            }
        }
    }

    fun sharePhotos(context: Context, photosToShare: List<PhotoEntity>) {
        val uris = photosToShare.map { it.uriString.toUri() }
        MediaStoreHelper.sharePhotos(context, uris)
    }

    fun deletePhotos(context: Context, photosToDelete: List<PhotoEntity>) {
        viewModelScope.launch {
            val ids = photosToDelete.map { it.id }
            for (p in photosToDelete) {
                MediaStoreHelper.deletePhoto(context, p.uriString.toUri())
            }
            photoDao.deleteByIds(ids)
            _selectedPhotoIds.value = emptySet()
            _isSelectionMode.value = false
        }
    }

    fun deleteAllPhotos(context: Context) {
        viewModelScope.launch {
            val all = photos.value
            for (p in all) {
                MediaStoreHelper.deletePhoto(context, p.uriString.toUri())
            }
            photoDao.deleteAll()
            _selectedPhotoIds.value = emptySet()
            _isSelectionMode.value = false
        }
    }
}
