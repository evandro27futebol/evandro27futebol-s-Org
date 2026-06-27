package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Scene
import com.example.ui.ProjectViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    viewModel: ProjectViewModel,
    onNavigateBack: () -> Unit
) {
    val project by viewModel.currentProject.collectAsState()
    val scenes by viewModel.currentScenes.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()

    var bgMusicVolume by remember { mutableStateOf(0.15f) }

    if (project == null || scenes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ElectricCyan)
        }
        return
    }

    val totalDurationMs = project!!.audioDurationMs
    val totalSeconds = totalDurationMs / 1000

    // Determine current active scene
    val activeSceneIndex = remember(currentTimeMs, scenes) {
        val idx = scenes.indexOfFirst { currentTimeMs >= it.startTimeMs && currentTimeMs < it.endTimeMs }
        if (idx != -1) idx else if (currentTimeMs >= totalDurationMs) scenes.size - 1 else 0
    }
    val activeScene = scenes.getOrNull(activeSceneIndex) ?: scenes.first()

    // Calculate scene progress fraction for Ken Burns effect
    val sceneProgressFraction = remember(currentTimeMs, activeScene) {
        val sceneDuration = activeScene.endTimeMs - activeScene.startTimeMs
        if (sceneDuration <= 0) 0f
        else {
            val elapsed = currentTimeMs - activeScene.startTimeMs
            (elapsed.toFloat() / sceneDuration).coerceIn(0f, 1f)
        }
    }

    // Determine Ken Burns transformations dynamically
    val (scale, transX, transY) = remember(activeScene, sceneProgressFraction) {
        when (activeScene.motionStyle) {
            "Zoom In" -> Triple(1.0f + (sceneProgressFraction * 0.20f), 0f, 0f)
            "Zoom Out" -> Triple(1.20f - (sceneProgressFraction * 0.20f), 0f, 0f)
            "Pan Left" -> Triple(1.15f, 20f - (sceneProgressFraction * 40f), 0f)
            "Pan Right" -> Triple(1.15f, -20f + (sceneProgressFraction * 40f), 0f)
            else -> Triple(1.05f, 0f, 0f) // Static / subtle
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prévia do Vídeo", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = ElectricCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 16:9 Aspect Video Player Viewport ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black)
                    .testTag("preview_player_viewport"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. Ken Burns Animated Image Layers
                    if (!activeScene.imageUriString.isNullOrEmpty()) {
                        AsyncImage(
                            model = activeScene.imageUriString,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = transX,
                                    translationY = transY
                                )
                        )
                    } else {
                        // Color grading fallback
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(DarkSurfaceVariant, DarkBackground)
                                    )
                                )
                        )
                    }

                    // 2. Heavy Color Grading Overlay based on template style
                    val colorGradingAlpha = when (project!!.templateStyle) {
                        "suspense" -> 0.45f
                        "documentary" -> 0.20f
                        "news" -> 0.15f
                        else -> 0.25f
                    }
                    val gradingColor = when (project!!.templateStyle) {
                        "suspense" -> Color(0xFF030E14) // dark cyan tint
                        "documentary" -> Color(0xFF130902) // warm sepia
                        "news" -> Color(0xFF020914) // cold blue
                        else -> Color.Black
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(gradingColor.copy(alpha = colorGradingAlpha))
                    )

                    // 3. Dynamic Subtitles (Floating bottom)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .padding(horizontal = 16.dp)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = activeScene.narrationText,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 4. Large Bold Impact Text Overlay (Flashes centered based on progress)
                    if (activeScene.overlayText.isNotBlank()) {
                        val isTextVisible = sceneProgressFraction in 0.15f..0.85f
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isTextVisible,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut(),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(
                                text = activeScene.overlayText.uppercase(),
                                color = ElectricCyan,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center,
                                letterSpacing = 2.sp,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Scene Index Tag top-right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CENA ${activeSceneIndex + 1}/${scenes.size}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- Timeline progress Scrubbing Bar ---
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentSec = currentTimeMs / 1000
                    Text(
                        text = String.format("%02d:%02d", currentSec / 60, currentSec % 60),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Slider(
                    value = currentTimeMs.toFloat(),
                    onValueChange = { viewModel.seekPreview(it.toLong()) },
                    valueRange = 0f..totalDurationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        activeTrackColor = ElectricCyan,
                        inactiveTrackColor = DarkSurfaceVariant,
                        thumbColor = ElectricCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preview_scrubber_timeline")
                )
            }

            // --- Video Action Controls Bar ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val prevIdx = (activeSceneIndex - 1).coerceAtLeast(0)
                        viewModel.seekPreview(scenes[prevIdx].startTimeMs)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.SkipPrevious, "Cena Anterior", tint = TextPrimary, modifier = Modifier.size(28.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                FloatingActionButton(
                    onClick = { viewModel.togglePlayback() },
                    containerColor = ElectricPurple,
                    contentColor = Color(0xFF003355),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Tocar/Pausar",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = {
                        val nextIdx = (activeSceneIndex + 1).coerceAtMost(scenes.size - 1)
                        viewModel.seekPreview(scenes[nextIdx].startTimeMs)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.SkipNext, "Próxima Cena", tint = TextPrimary, modifier = Modifier.size(28.dp))
                }
            }

            // --- Background Soundtrack Volume Controller ---
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (project!!.bgMusicName == "Silent") Icons.Filled.VolumeMute else Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Música: ${project!!.bgMusicName}",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                        }

                        Text(
                            text = "${(bgMusicVolume * 100).toInt()}% vol",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Slider(
                        value = bgMusicVolume,
                        onValueChange = {
                            bgMusicVolume = it
                            viewModel.updateActiveProjectMusic(project!!.bgMusicName ?: "Silent", it)
                        },
                        valueRange = 0f..0.50f, // cap volume to avoid competition
                        colors = SliderDefaults.colors(
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = DarkSurfaceVariant,
                            thumbColor = ElectricCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "A música é mixada automaticamente com volume balanceado abaixo da narração.",
                        fontSize = 10.sp,
                        color = TextTertiary,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}
