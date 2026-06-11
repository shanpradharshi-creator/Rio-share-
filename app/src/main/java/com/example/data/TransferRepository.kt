package com.example.data

import kotlinx.coroutines.flow.Flow

class TransferRepository(private val transferDao: TransferDao) {
    val allTransfers: Flow<List<TransferEntity>> = transferDao.getAllTransfers()

    suspend fun insertTransfer(transfer: TransferEntity) {
        transferDao.insertTransfer(transfer)
    }

    suspend fun deleteTransfer(transfer: TransferEntity) {
        transferDao.deleteTransfer(transfer)
    }

    suspend fun clearHistory() {
        transferDao.clearAllHistory()
    }

    // Static structures representing realistic files available on the sender device
    val availableFilesToShare = listOf(
        ShareableFile("beach_sunset.jpg", "PHOTO", 4_380_450, "Sunset at Marina Beach"),
        ShareableFile("temple_gopuram.jpg", "PHOTO", 5_620_120, "Meenakshi Temple gopuram"),
        ShareableFile("family_portrait.png", "PHOTO", 8_120_500, "Deepavali celebrations"),
        ShareableFile("rio_vlog_2026.mp4", "VIDEO", 149_422_080, "Vlog of Rio city"),
        ShareableFile("dance_festival.mp4", "VIDEO", 78_748_876, "Classical Bharatanatyam dance"),
        ShareableFile("movie_clip.mp4", "VIDEO", 262_144_000, "High definition movie clip"),
        ShareableFile("project_proposal.pdf", "DOCUMENT", 1_258_291, "Business proposal draft"),
        ShareableFile("tamil_dictionary.pdf", "DOCUMENT", 921_600, "Tamil-English dictionary"),
        ShareableFile("lecture_notes.docx", "DOCUMENT", 3_565_158, "Computer Networks notes"),
        ShareableFile("rio_share_v2.apk", "APK", 24_117_248, "Rio Share Latest Beta"),
        ShareableFile("pubg_mobile_installer.apk", "APK", 152_043_520, "P2P game installer package"),
        ShareableFile("tamil_keyboard.apk", "APK", 14_680_064, "Tamil regional context input"),
        ShareableFile("backup_archive.zip", "OTHER", 536_870_912, "Full device backup zip"),
        ShareableFile("favorite_song.mp3", "OTHER", 12_582_912, "High fidelity dynamic stereo music"),
        ShareableFile("preset_config.json", "OTHER", 46_080, "App synthesizer setup parameters")
    )
}

data class ShareableFile(
    val fileName: String,
    val fileType: String, // "PHOTO", "VIDEO", "DOCUMENT", "APK", "OTHER"
    val size: Long,
    val description: String
)
