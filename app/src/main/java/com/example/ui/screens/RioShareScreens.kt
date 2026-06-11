package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ShareableFile
import com.example.data.TransferEntity
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.Localization
import com.example.viewmodel.ActiveTransferState
import com.example.viewmodel.NearbyDevice
import com.example.viewmodel.SelectableFile
import com.example.viewmodel.ShareScreen
import com.example.viewmodel.ShareViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Utility function to format files sizes nicely
fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}

// Utility function to format speeds nicely
fun formatSpeed(speedBytesPerSec: Long): String {
    val mb = speedBytesPerSec / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f", mb)
}

// Utility function to format times nicely
fun formatRemaining(transferred: Long, total: Long, speedBps: Long, strings: Localization): String {
    if (speedBps <= 0) return "--"
    val remainingBytes = total - transferred
    if (remainingBytes <= 0) return "0s"
    val secondsTotal = (remainingBytes.toDouble() / speedBps).roundToInt()
    val minutes = secondsTotal / 60
    val seconds = secondsTotal % 60
    return if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}

fun getFileIcon(type: String): ImageVector {
    return when (type.uppercase()) {
        "PHOTO" -> Icons.Default.Image
        "VIDEO" -> Icons.Default.Videocam
        "DOCUMENT" -> Icons.Default.Description
        "APK" -> Icons.Default.Android
        else -> Icons.Default.FolderZip
    }
}

fun getFileColor(type: String): Color {
    return when (type.uppercase()) {
        "PHOTO" -> Color(0xFF10B981)   // Emerald Green
        "VIDEO" -> Color(0xFF3B82F6)   // Ocean Blue
        "DOCUMENT" -> Color(0xFFF59E0B) // Amber
        "APK" -> Color(0xFF8B5CF6)      // Violet Purple
        else -> Color(0xFF6B7280)       // Slate Gray
    }
}

@Composable
fun RioShareAppContent(viewModel: ShareViewModel) {
    val screen by viewModel.currentScreen.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val lang by viewModel.currentLanguage.collectAsState()
    val strings = viewModel.getLocalization()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = screen,
            label = "ScreenTransition"
        ) { targetScreen ->
            when (targetScreen) {
                ShareScreen.HOME -> HomeScreen(viewModel, strings)
                ShareScreen.SEND_SELECT -> SendFilesScreen(viewModel, strings)
                ShareScreen.DISCOVERY -> DeviceDiscoveryScreen(viewModel, strings)
                ShareScreen.RECEIVE_WAIT -> ReceiveFilesScreen(viewModel, strings)
                ShareScreen.PROGRESS -> TransferProgressScreen(viewModel, strings)
                ShareScreen.HISTORY -> HistoryScreen(viewModel, strings)
            }
        }
    }
}

// ----------------------------------------------------
// 1. HOME SCREEN
// ----------------------------------------------------
@Composable
fun HomeScreen(viewModel: ShareViewModel, strings: Localization) {
    val isDark by viewModel.isDarkMode.collectAsState()
    val lang by viewModel.currentLanguage.collectAsState()
    val history by viewModel.historyItems.collectAsState()

    // Calculate aggregated statistics from history
    val totalSharedBytes = history.filter { !it.isIncoming && it.status == "COMPLETED" }.sumOf { it.size }
    val totalReceivedBytes = history.filter { it.isIncoming && it.status == "COMPLETED" }.sumOf { it.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        // Top Brand Header Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = strings.get("appName"),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = strings.get("tagline"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }

            // Quick Toggle Pill (English / தமிழ் and Theme switcher side-by-side)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language switcher button
                IconButton(
                    onClick = { viewModel.toggleLanguage() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("lang_toggle_button")
                ) {
                    Text(
                        text = if (lang == AppLanguage.ENGLISH) "த" else "EN",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Dark/Light Theme Button
                IconButton(
                    onClick = { viewModel.toggleTheme() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Theme Switcher",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large tactile quick sharing dashboard cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SEND BUTTON CARD (Teal Gradient Theme)
            Card(
                onClick = { viewModel.navigateTo(ShareScreen.SEND_SELECT) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .testTag("send_button_card"),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send File Icon",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp).rotate(-45f)
                            )
                        }

                        Column {
                            Text(
                                text = strings.get("send"),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = strings.get("photos") + ", apks...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // RECEIVE BUTTON CARD (Ocean Blue Theme)
            Card(
                onClick = { viewModel.navigateTo(ShareScreen.RECEIVE_WAIT) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .testTag("receive_button_card"),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Receive File Icon",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column {
                            Text(
                                text = strings.get("receive"),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = strings.get("showMyQr"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Aggregate statistics section
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.get("quickStats"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Navigation link to history
                    TextButton(
                        onClick = { viewModel.navigateTo(ShareScreen.HISTORY) },
                        modifier = Modifier.testTag("view_history_link")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = strings.get("history"), style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Sent
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = strings.get("totalShared"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Text(
                                text = formatSize(totalSharedBytes),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Total Received
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = strings.get("totalReceived"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Text(
                                text = formatSize(totalReceivedBytes),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dynamic History List section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.get("recent"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (history.isNotEmpty()) {
                Text(
                    text = "${history.take(5).size} / ${history.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Recent items list
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderZip,
                        contentDescription = "Empty Folders",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.get("noHistory"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history.take(5)) { item ->
                    HistoryItemRow(item = item, onDelete = { viewModel.deleteHistoryItem(item) })
                }
            }
        }
    }
}

// ----------------------------------------------------
// 2. SEND FILE SELECTION SCREEN
// ----------------------------------------------------
@Composable
fun SendFilesScreen(viewModel: ShareViewModel, strings: Localization) {
    val files by viewModel.availableFiles.collectAsState()
    val listCategories = listOf("PHOTO", "VIDEO", "DOCUMENT", "APK", "OTHER")
    var selectedCategoryTab by remember { mutableStateOf("PHOTO") }

    val selectedFiles = files.filter { it.isSelected }
    val totalSelectedSize = selectedFiles.sumOf { it.file.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(ShareScreen.HOME) },
                modifier = Modifier.testTag("send_back_home_btn")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = strings.get("selectFiles"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        // Category scroll tab list
        ScrollableTabRow(
            selectedTabIndex = listCategories.indexOf(selectedCategoryTab),
            edgePadding = 16.dp,
            divider = {},
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            listCategories.forEach { category ->
                val isSelected = selectedCategoryTab == category
                Tab(
                    selected = isSelected,
                    onClick = { selectedCategoryTab = category },
                    text = {
                        val labelKey = when (category) {
                            "PHOTO" -> "photos"
                            "VIDEO" -> "videos"
                            "DOCUMENT" -> "documents"
                            "APK" -> "apks"
                            else -> "others"
                        }
                        Text(
                            text = strings.get(labelKey),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Quick Category Action Row (Select All in dynamic category)
        val filesInCategory = files.filter { it.file.fileType == selectedCategoryTab }
        val categoryAllSelected = filesInCategory.isNotEmpty() && filesInCategory.all { it.isSelected }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${strings.get("availableDevices")}: ${filesInCategory.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            TextButton(
                onClick = {
                    if (categoryAllSelected) {
                        viewModel.deselectCategory(selectedCategoryTab)
                    } else {
                        viewModel.selectCategory(selectedCategoryTab)
                    }
                },
                modifier = Modifier.testTag("select_all_category_btn")
            ) {
                Text(
                    text = if (categoryAllSelected) "Deselect All" else "Select All",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Selected files list grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filesInCategory) { selectableItem ->
                    FileGridSelectorItem(
                        selectable = selectableItem,
                        onChecked = { viewModel.toggleFileSelection(selectableItem.file.fileName) }
                    )
                }
            }
        }

        // Bottom progress summary card
        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.get("selectedFiles", selectedFiles.size, formatSize(totalSelectedSize)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedFiles.isEmpty()) "No files chosen" else "Ready to share securely",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Button(
                        onClick = { viewModel.navigateTo(ShareScreen.DISCOVERY) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("next_to_discovery_button")
                    ) {
                        Text(text = strings.get("next"), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.NavigateNext, contentDescription = "Next")
                    }
                }
            }
        }
    }
}

// Beautiful selector grid card
@Composable
fun FileGridSelectorItem(selectable: SelectableFile, onChecked: () -> Unit) {
    val file = selectable.file
    val themeColor = getFileColor(file.fileType)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selectable.isSelected) {
                themeColor.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selectable.isSelected) 2.dp else 1.dp,
                color = if (selectable.isSelected) themeColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onChecked() }
            .testTag("grid_file_${file.fileName}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getFileIcon(file.fileType),
                        contentDescription = "File Type",
                        tint = themeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Checkbox(
                    checked = selectable.isSelected,
                    onCheckedChange = { onChecked() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = themeColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = file.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = file.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatSize(file.size),
                style = MaterialTheme.typography.labelSmall,
                color = themeColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ----------------------------------------------------
// 3. DEVICE DISCOVERY SCREEN
// ----------------------------------------------------
@Composable
fun DeviceDiscoveryScreen(viewModel: ShareViewModel, strings: Localization) {
    val isScanning by viewModel.isScanning.collectAsState()
    val devices by viewModel.nearbyDevices.collectAsState()

    // Smooth continuous rotating ripple waves for scan active
    val scanRippleAnim = rememberInfiniteTransition(label = "pulse")
    val radiusRatio by scanRippleAnim.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    val opacityRatio by scanRippleAnim.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "opacity"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(ShareScreen.SEND_SELECT) },
                modifier = Modifier.testTag("discovery_back_btn")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = strings.get("deviceDiscovery"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        // Radar Scanning Canvas Display Panel
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary

                // Ripples Canvas
                if (isScanning && devices.size < 6) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val maxRadius = size.width.coerceAtMost(size.height) * 0.45f
                        
                        // Ripple 1
                        drawCircle(
                            color = primaryColor.copy(alpha = opacityRatio * 0.25f),
                            radius = maxRadius * radiusRatio,
                            center = center,
                            style = Stroke(width = 4.dp.toPx())
                        )

                        // Ripple 2 (staggered)
                        val r2 = (radiusRatio + 0.5f) % 1.0f
                        val o2 = (1.0f - r2)
                        drawCircle(
                            color = primaryColor.copy(alpha = o2 * 0.15f),
                            radius = maxRadius * r2,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Center Icon with secure badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothSearching,
                            contentDescription = "Bluetooth radar",
                            tint = primaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isScanning) strings.get("searchingDevices") else "Scan complete",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security End-to-End prompt info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Shield Connect Security",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = strings.get("securePrompt"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device lists header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.get("availableDevices"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                TextButton(
                    onClick = { viewModel.startDeviceDiscovery() },
                    modifier = Modifier.testTag("rescan_btn")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rescan")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Rescan")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Devices List
        if (devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.get("noDevices"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.startDeviceDiscovery() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Try Again")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(devices) { device ->
                    DeviceRowItem(
                        device = device,
                        onClick = { viewModel.connectAndTransferToDevice(device) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // QR Connector Trigger Button
        Card(
            onClick = { viewModel.connectAndTransferViaQr() }, // Fallback mock connection
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("qr_connector_mock_trigger"),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "QR Scanner", tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.get("simScanQr"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// Device Row visual
@Composable
fun DeviceRowItem(device: NearbyDevice, onClick: () -> Unit) {
    val devIcon = when (device.deviceType) {
        "PHONE" -> Icons.Default.Smartphone
        "TABLET" -> Icons.Default.TabletAndroid
        else -> Icons.Default.Laptop
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("device_row_${device.name}"),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = devIcon,
                    contentDescription = "Device icon",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (device.isSecure) "Encrypted pairing available" else "P2P open peer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            // Signal bar indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(14.dp)
            ) {
                val levels = listOf(0.3f, 0.5f, 0.7f, 1.0f)
                levels.forEachIndexed { idx, barValue ->
                    val color = if (device.strength >= barValue) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                    }
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height((4 + (idx * 3)).dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// 4. RECEIVE / WAITING SCREEN
// ----------------------------------------------------
@Composable
fun ReceiveFilesScreen(viewModel: ShareViewModel, strings: Localization) {
    val qrcodePayload by viewModel.qrConnectCode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(ShareScreen.HOME) },
                modifier = Modifier.testTag("receive_back_home_btn")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = strings.get("receive"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large high-fidelity procedural QR Code
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .size(310.dp)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Procedural QR Drawing
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val qrSize = size.width
                    val columns = 17
                    val cellSize = qrSize / columns

                    // Corners anchor boxes
                    val corners = listOf(
                        Offset(0f, 0f),
                        Offset(qrSize - (7 * cellSize), 0f),
                        Offset(0f, qrSize - (7 * cellSize))
                    )

                    // Draw anchor finders
                    corners.forEach { corner ->
                        drawRect(
                            color = Color(0xFF0F172A), // Dark Slate
                            topLeft = corner,
                            size = Size(7 * cellSize, 7 * cellSize)
                        )
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(corner.x + cellSize, corner.y + cellSize),
                            size = Size(5 * cellSize, 5 * cellSize)
                        )
                        drawRect(
                            color = Color(0xFF0D9488), // Teal target center
                            topLeft = Offset(corner.x + (2 * cellSize), corner.y + (2 * cellSize)),
                            size = Size(3 * cellSize, 3 * cellSize)
                        )
                    }

                    // Pseudo-random deterministic QR bits
                    val deterministicCode = qrcodePayload.hashCode()
                    val r = Random(deterministicCode.toLong())

                    for (i in 0 until columns) {
                        for (j in 0 until columns) {
                            // Skip anchor corners
                            val inTopLeft = i < 8 && j < 8
                            val inTopRight = i >= columns - 8 && j < 8
                            val inBottomLeft = i < 8 && j >= columns - 8
                            if (inTopLeft || inTopRight || inBottomLeft) continue

                            // Deterministic cells
                            if (r.nextBoolean()) {
                                drawRect(
                                    color = Color(0xFF1E293B),
                                    topLeft = Offset(i * cellSize, j * cellSize),
                                    size = Size(cellSize - 0.5f, cellSize - 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Security details info
        Text(
            text = strings.get("scanQrTitle"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = strings.get("scanQrSub"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Waiting status banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = strings.get("waitingSender"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Manual Accept transfer simulation button for desktop/P2P tests
        Button(
            onClick = { viewModel.connectAndTransferViaQr() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .testTag("simulate_receive_scan_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Accept Transfer (" + strings.get("completed") + ")", fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------------------------------------------
// 5. ACTIVE TRANSFER PROGRESS SCREEN
// ----------------------------------------------------
@Composable
fun TransferProgressScreen(viewModel: ShareViewModel, strings: Localization) {
    val activeState by viewModel.activeTransfer.collectAsState()

    val percent = if (activeState.totalBytes > 0) {
        (activeState.transferredBytes.toDouble() / activeState.totalBytes.toDouble() * 100).coerceIn(0.0, 100.0)
    } else {
        0.0
    }

    // Dynamic wave ripple scales based on transfer speed
    val infiniteTransition = rememberInfiniteTransition(label = "speedRip")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ripple"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Screen title
        Text(
            text = strings.get("transferProgress"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Progress details hero container
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Connection peer bubble details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (activeState.isIncoming) Icons.Default.Download else Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Status direction",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.get(if (activeState.isIncoming) "receiving" else "sending") + " " + activeState.peerName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Giant Animated Progress Core Percentage Dial
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val pulseModifier = if (activeState.status == "RUNNING" && !activeState.isPaused) {
                        Modifier.scale(waveScale)
                    } else {
                        Modifier
                    }

                    // Background soft wave glow
                    Box(
                        modifier = pulseModifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Material circular progress indic
                    CircularProgressIndicator(
                        progress = { (percent / 100f).toFloat() },
                        modifier = Modifier.size(150.dp),
                        strokeWidth = 10.dp,
                        color = MaterialTheme.colorScheme.primary,
                        strokeCap = StrokeCap.Round
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.US, "%.0f%%", percent),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 38.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${activeState.currentFileIndex + 1} / ${activeState.files.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Current Active filename in transport
                val activeFile = activeState.files.getOrNull(activeState.currentFileIndex)
                Text(
                    text = strings.get("activeFile", activeFile?.fileName ?: "File"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Grid stats row (Speed, remaining time, size)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = strings.get("speed"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (activeState.status == "RUNNING") {
                                "${formatSpeed(activeState.currentSpeedBps)} MB/s"
                            } else {
                                "0 MB/s"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (activeState.currentSpeedBps > 0) Color(0xFF10B981) else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = strings.get("remainingTime"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            text = formatRemaining(
                                activeState.transferredBytes,
                                activeState.totalBytes,
                                activeState.currentSpeedBps,
                                strings
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = strings.get("fileSize"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            text = formatSize(activeState.totalBytes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Files inline list inside the collection
        Text(
            text = "Total Files (${activeState.files.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activeState.files.indices.toList()) { index ->
                val f = activeState.files[index]
                val itemCompleted = activeState.currentFileIndex > index
                val isCurrentlyTransferring = activeState.currentFileIndex == index

                val bgStyle = when {
                    itemCompleted -> Color(0xFF10B981).copy(alpha = 0.06f)
                    isCurrentlyTransferring -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgStyle)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getFileIcon(f.fileType),
                        contentDescription = "file type",
                        tint = getFileColor(f.fileType).copy(alpha = if (itemCompleted || isCurrentlyTransferring) 1.0f else 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = f.fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrentlyTransferring) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatSize(f.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }

                    if (itemCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Finished",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (isCurrentlyTransferring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Actions flow (Cancel / Pause or Done when successful)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeState.status == "COMPLETED" || activeState.status == "FAILED") {
                Button(
                    onClick = { viewModel.navigateTo(ShareScreen.HOME) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("transfer_complete_done_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (activeState.status == "COMPLETED") strings.get("done") else strings.get("backToHome"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            } else {
                // Cancel
                OutlinedButton(
                    onClick = { viewModel.cancelTransfer() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("cancel_transfer_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    )
                ) {
                    Text(text = strings.get("cancel"), fontWeight = FontWeight.Bold)
                }

                // Pause
                Button(
                    onClick = { viewModel.togglePauseTransfer() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("pause_transfer_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (activeState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause button"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (activeState.isPaused) "Resume" else "Pause",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// 6. HISTORY SCREEN (SQL / ROOM DATABASE CONTROL)
// ----------------------------------------------------
@Composable
fun HistoryScreen(viewModel: ShareViewModel, strings: Localization) {
    val history by viewModel.historyItems.collectAsState()
    var filterType by remember { mutableStateOf("ALL") }

    val filteredHistory = when (filterType) {
        "SENT" -> history.filter { !it.isIncoming }
        "RECEIVED" -> history.filter { it.isIncoming }
        else -> history
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.navigateTo(ShareScreen.HOME) },
                    modifier = Modifier.testTag("history_back_home_btn")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = strings.get("transferHistory"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            if (history.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearHistory() },
                    modifier = Modifier.testTag("clear_history_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear all database histories",
                        tint = Color(0xFFEF4444)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // History Filters pills row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("ALL", "SENT", "RECEIVED").forEach { type ->
                val label = when (type) {
                    "SENT" -> strings.get("send")
                    "RECEIVED" -> strings.get("receive")
                    else -> "All"
                }

                val isSelected = filterType == type
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    modifier = Modifier
                        .clickable { filterType = type }
                        .height(38.dp)
                        .testTag("filter_pill_$type"),
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History rows body scrolling list
        if (filteredHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "No history icons",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.get("noHistory"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredHistory) { item ->
                    HistoryItemRow(
                        item = item,
                        onDelete = { viewModel.deleteHistoryItem(item) }
                    )
                }
            }
        }
    }
}

// Visual layout represent a SQL item completed
@Composable
fun HistoryItemRow(item: TransferEntity, onDelete: () -> Unit) {
    val themeColor = getFileColor(item.fileType)
    val formattedDate = remember(item.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${item.id}"),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Direction / type visual hybrid wrapper
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                // File Type color logo bubble
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(themeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getFileIcon(item.fileType),
                        contentDescription = "File icon",
                        tint = themeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Mini directional bubble overlay
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isIncoming) Color(0xFF10B981) else Color(0xFF3B82F6)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isIncoming) {
                            Icons.Default.ArrowDownward
                        } else {
                            Icons.Default.ArrowUpward
                        },
                        contentDescription = "Direction status outline",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatSize(item.size)} · ${item.peerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            // Quick deletion option
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_single_history_item_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete item",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
