package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.Localization
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class ShareScreen {
    HOME,
    SEND_SELECT,
    DISCOVERY,
    RECEIVE_WAIT,
    PROGRESS,
    HISTORY
}

data class SelectableFile(
    val file: ShareableFile,
    val isSelected: Boolean = false
)

data class ActiveTransferState(
    val isIncoming: Boolean = false,
    val peerName: String = "",
    val files: List<ShareableFile> = emptyList(),
    val currentFileIndex: Int = 0,
    val transferredBytes: Long = 0,
    val totalBytes: Long = 0,
    val currentSpeedBps: Long = 0,
    val isPaused: Boolean = false,
    val status: String = "IDLE" // "IDLE", "RUNNING", "COMPLETED", "FAILED"
)

data class NearbyDevice(
    val name: String,
    val strength: Float, // 0.0 to 1.0 representing RSSI
    val deviceType: String, // "PHONE", "TABLET", "LAPTOP"
    val isSecure: Boolean = true
)

class ShareViewModel(application: Application) : AndroidViewModel(application) {
    private val db = RioDatabase.getDatabase(application)
    private val repository = TransferRepository(db.transferDao())

    // Language & Theme states
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage = _currentLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true) // Default to modern premium dark mode
    val isDarkMode = _isDarkMode.asStateFlow()

    // Screen navigation
    private val _currentScreen = MutableStateFlow(ShareScreen.HOME)
    val currentScreen = _currentScreen.asStateFlow()

    // Localization helper accessor
    fun getLocalization(): Localization = Localization(_currentLanguage.value)

    // History data loaded from Room Database
    val historyItems: StateFlow<List<TransferEntity>> = repository.allTransfers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selectable files for sharing
    private val _availableFiles = MutableStateFlow<List<SelectableFile>>(emptyList())
    val availableFiles = _availableFiles.asStateFlow()

    // Nearby discovered devices
    private val _nearbyDevices = MutableStateFlow<List<NearbyDevice>>(emptyList())
    val nearbyDevices = _nearbyDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    // Active file transfer state
    private val _activeTransfer = MutableStateFlow(ActiveTransferState())
    val activeTransfer = _activeTransfer.asStateFlow()

    // Simulated QR connection code (represents dynamic handshake credentials)
    private val _qrConnectCode = MutableStateFlow("rio-share://connect?device=Rio_Share_Device&pin=9521&secure=true")
    val qrConnectCode = _qrConnectCode.asStateFlow()

    // Active coroutine job for transfer simulation
    private var transferJob: Job? = null
    private var scanningJob: Job? = null

    init {
        // Load initial files to share
        resetFilesSelection()
    }

    fun toggleLanguage() {
        val nextLang = if (_currentLanguage.value == AppLanguage.ENGLISH) {
            AppLanguage.TAMIL
        } else {
            AppLanguage.ENGLISH
        }
        _currentLanguage.value = nextLang
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun navigateTo(screen: ShareScreen) {
        _currentScreen.value = screen
        if (screen == ShareScreen.DISCOVERY) {
            startDeviceDiscovery()
        } else if (screen == ShareScreen.RECEIVE_WAIT) {
            generateNewQrCode()
        }
    }

    // Reset selection list
    fun resetFilesSelection() {
        _availableFiles.value = repository.availableFilesToShare.map {
            SelectableFile(file = it, isSelected = false)
        }
    }

    fun toggleFileSelection(fileName: String) {
        _availableFiles.value = _availableFiles.value.map {
            if (it.file.fileName == fileName) {
                it.copy(isSelected = !it.isSelected)
            } else {
                it
            }
        }
    }

    fun selectCategory(category: String) {
        _availableFiles.value = _availableFiles.value.map {
            if (it.file.fileType == category) {
                it.copy(isSelected = true)
            } else {
                it
            }
        }
    }

    fun deselectCategory(category: String) {
        _availableFiles.value = _availableFiles.value.map {
            if (it.file.fileType == category) {
                it.copy(isSelected = false)
            } else {
                it
            }
        }
    }

    // Nearby Device Scanning Simulation
    fun startDeviceDiscovery() {
        scanningJob?.cancel()
        _isScanning.value = true
        _nearbyDevices.value = emptyList()

        scanningJob = viewModelScope.launch {
            val names = listOf(
                Pair("Anand's OnePlus Nord", "PHONE"),
                Pair("Priya's Galaxy S24", "PHONE"),
                Pair("Senthil's iPad Air", "TABLET"),
                Pair("Karthik's Pixel 9 Pro", "PHONE"),
                Pair("Chennai_Rio_PC", "LAPTOP"),
                Pair("Meena's Redmi Note", "PHONE")
            )
            
            var index = 0
            while (index < names.size && _isScanning.value) {
                delay(1200 + Random.nextLong(200, 1000))
                val device = NearbyDevice(
                    name = names[index].first,
                    strength = Random.nextFloat() * 0.4f + 0.6f,
                    deviceType = names[index].second,
                    isSecure = Random.nextBoolean()
                )
                _nearbyDevices.value = _nearbyDevices.value + device
                index++
            }
            _isScanning.value = false
        }
    }

    fun stopDeviceDiscovery() {
        scanningJob?.cancel()
        _isScanning.value = false
    }

    // Generate dynamic QR representing an E2E session PIN and safety handshake
    private fun generateNewQrCode() {
        val pin = Random.nextInt(1000, 9999)
        _qrConnectCode.value = "rio-share://connect?device=Rio_Share_Device_Receiver&pin=$pin&secure=true"
    }

    // Connect via QR simulation
    fun connectAndTransferViaQr(mockQrPayload: String = "") {
        _currentScreen.value = ShareScreen.PROGRESS
        val targetPayload = if (mockQrPayload.isEmpty()) _qrConnectCode.value else mockQrPayload
        val decName = if (targetPayload.contains("Receiver")) "Rio Receiver PEER" else "Rio Sender PEER"
        
        val selected = _availableFiles.value.filter { it.isSelected }.map { it.file }
        val finalFilesList = if (selected.isEmpty()) {
            // Fallback to a default if none selected (Receive screen or quick share flow)
            listOf(
                repository.availableFilesToShare[0], // beach_sunset.jpg
                repository.availableFilesToShare[7]  // tamil_dictionary.pdf
            )
        } else {
            selected
        }

        startTransferSim(
            files = finalFilesList,
            peer = decName,
            isIncoming = selected.isEmpty() // if we didn't select, we are receiving!
        )
    }

    // Choose list device to trigger transfer
    fun connectAndTransferToDevice(device: NearbyDevice) {
        stopDeviceDiscovery()
        _currentScreen.value = ShareScreen.PROGRESS
        
        val selected = _availableFiles.value.filter { it.isSelected }.map { it.file }
        val filesToTransfer = if (selected.isEmpty()) {
            // Auto share a nice dummy package for demonstration if they didn't pick anything
            listOf(repository.availableFilesToShare[9]) // rio_share_v2.apk
        } else {
            selected
        }

        startTransferSim(
            files = filesToTransfer,
            peer = device.name,
            isIncoming = false
        )
    }

    // Simulated transfer pipeline
    private fun startTransferSim(files: List<ShareableFile>, peer: String, isIncoming: Boolean) {
        transferJob?.cancel()
        
        val totalBytes = files.sumOf { it.size }
        _activeTransfer.value = ActiveTransferState(
            isIncoming = isIncoming,
            peerName = peer,
            files = files,
            currentFileIndex = 0,
            transferredBytes = 0,
            totalBytes = totalBytes,
            currentSpeedBps = 0,
            isPaused = false,
            status = "RUNNING"
        )

        transferJob = viewModelScope.launch {
            var fileIdx = 0
            var bytesDone = 0L

            while (fileIdx < files.size) {
                val currentFile = files[fileIdx]
                var fileBytesTransferred = 0L
                val fileSize = currentFile.size

                // File progress block
                while (fileBytesTransferred < fileSize) {
                    if (_activeTransfer.value.isPaused) {
                        _activeTransfer.value = _activeTransfer.value.copy(
                            currentSpeedBps = 0
                        )
                        delay(250)
                        continue
                    }

                    delay(150)
                    
                    // Speed fluctuates dynamically between 12 MB/s and 48 MB/s
                    val speedScale = Random.nextLong(12_000_000, 48_000_000)
                    val chunk = (speedScale * 0.150f).toLong() // 150ms segment

                    fileBytesTransferred += chunk
                    if (fileBytesTransferred > fileSize) {
                        fileBytesTransferred = fileSize
                    }

                    bytesDone = _activeTransfer.value.transferredBytes + (chunk)
                    val cumulativeTransferred = if (bytesDone > totalBytes) totalBytes else bytesDone

                    _activeTransfer.value = _activeTransfer.value.copy(
                        currentFileIndex = fileIdx,
                        transferredBytes = cumulativeTransferred,
                        currentSpeedBps = speedScale,
                        status = "RUNNING"
                    )
                }

                // File complete, save to history in Room database!
                val completedEntity = TransferEntity(
                    name = currentFile.fileName,
                    fileType = currentFile.fileType,
                    size = currentFile.size,
                    status = "COMPLETED",
                    speed = Random.nextLong(20_000_000, 42_000_000),
                    peerName = peer,
                    isIncoming = isIncoming
                )
                repository.insertTransfer(completedEntity)

                fileIdx++
            }

            // Finish transfer state
            _activeTransfer.value = _activeTransfer.value.copy(
                transferredBytes = totalBytes,
                currentSpeedBps = 0,
                status = "COMPLETED"
            )

            // Clear selected files checks
            resetFilesSelection()
        }
    }

    fun togglePauseTransfer() {
        val current = _activeTransfer.value
        if (current.status == "RUNNING") {
            _activeTransfer.value = current.copy(isPaused = !current.isPaused)
        }
    }

    fun cancelTransfer() {
        transferJob?.cancel()
        _activeTransfer.value = _activeTransfer.value.copy(
            status = "FAILED",
            currentSpeedBps = 0
        )
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteHistoryItem(entity: TransferEntity) {
        viewModelScope.launch {
            repository.deleteTransfer(entity)
        }
    }
}
