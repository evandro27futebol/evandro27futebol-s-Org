package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Scene
import com.example.data.UserImage
import com.example.ui.ProjectViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: ProjectViewModel,
    onNavigateToPreview: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val project by viewModel.currentProject.collectAsState()
    val scenes by viewModel.currentScenes.collectAsState()
    val selectedIndex by viewModel.selectedSceneIndex.collectAsState()
    val images by viewModel.imageLibrary.collectAsState()

    var showImagePickerDialog by remember { mutableStateOf(false) }
    var showDurationMenu by remember { mutableStateOf(false) }
    var showMotionMenu by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    if (project == null || scenes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ElectricCyan)
        }
        return
    }

    val activeScene = scenes.getOrNull(selectedIndex) ?: scenes.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project!!.title,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Estilo: ${project!!.templateStyle.uppercase()}",
                            color = ElectricCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.Home, contentDescription = "Dashboard", tint = ElectricCyan)
                    }
                },
                actions = {
                    // Navigate to visual preview
                    IconButton(onClick = onNavigateToPreview) {
                        Icon(imageVector = Icons.Filled.PlayCircle, contentDescription = "Visualizar Prévia", tint = ElectricCyan)
                    }

                    Button(
                        onClick = onNavigateToExport,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color(0xFF003355)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("nav_export_button")
                    ) {
                        Icon(Icons.Filled.VideoSettings, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exportar", fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
        ) {
            // --- Bottom Timeline/Reel list (We show it on top/middle for great vertical reachability) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Linha do Tempo das Cenas",
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timeline_reel")
                ) {
                    itemsIndexed(scenes) { index, scene ->
                        TimelineReelItem(
                            scene = scene,
                            index = index,
                            isSelected = selectedIndex == index,
                            onClick = { viewModel.selectScene(index) }
                        )
                    }
                }
            }

            // --- Form Editor Space (Vertical scrollable details) ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Scene Preview Box (Tapping swaps image)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .border(
                            width = 1.dp,
                            color = ElectricCyan.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { showImagePickerDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (!activeScene.imageUriString.isNullOrEmpty()) {
                        AsyncImage(
                            model = activeScene.imageUriString,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Image, null, tint = TextTertiary, modifier = Modifier.size(40.dp))
                            Text("Sem Imagem", color = TextSecondary, fontSize = 12.sp)
                        }
                    }

                    // Floating swap button overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                            .clickable { showImagePickerDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Sync, null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                            Text("Trocar Imagem", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    // Sequence Label Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(ElectricPurple, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "CENA ${activeScene.sequenceNumber}",
                            color = Color(0xFF003355),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                // Scene parameters layout (Duration and Motion Side-by-Side)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Duration Input Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDurationMenu = true },
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.AccessTime, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                Text("Duração", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val durationSec = (activeScene.endTimeMs - activeScene.startTimeMs) / 1000
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$durationSec segundos", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Icon(Icons.Filled.ArrowDropDown, null, tint = TextSecondary)
                            }
                        }

                        DropdownMenu(
                            expanded = showDurationMenu,
                            onDismissRequest = { showDurationMenu = false },
                            modifier = Modifier.background(DarkSurfaceVariant)
                        ) {
                            listOf(4, 5, 6, 7).forEach { secs ->
                                DropdownMenuItem(
                                    text = { Text("$secs segundos", color = TextPrimary) },
                                    onClick = {
                                        viewModel.updateSceneDuration(activeScene, secs)
                                        showDurationMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Motion Style Ken Burns Selection Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showMotionMenu = true },
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Videocam, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                Text("Câmera / Motion", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(activeScene.motionStyle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Icon(Icons.Filled.ArrowDropDown, null, tint = TextSecondary)
                            }
                        }

                        DropdownMenu(
                            expanded = showMotionMenu,
                            onDismissRequest = { showMotionMenu = false },
                            modifier = Modifier.background(DarkSurfaceVariant)
                        ) {
                            listOf("Zoom In", "Zoom Out", "Pan Left", "Pan Right", "Static").forEach { motion ->
                                DropdownMenuItem(
                                    text = { Text(motion, color = TextPrimary) },
                                    onClick = {
                                        viewModel.updateSceneMotion(activeScene, motion)
                                        showMotionMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Narration Phrase Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Transcrição da Narração (Áudio Falado)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = activeScene.narrationText,
                        onValueChange = { viewModel.updateSceneText(activeScene, it, activeScene.overlayText) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricPurple,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        placeholder = { Text("Insira a frase narrada nesta cena...", color = TextTertiary) }
                    )
                }

                // Overlay impact subtitle input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.TextFields, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Text("Texto de Impacto na Tela (Max 5 palavras)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedTextField(
                        value = activeScene.overlayText,
                        onValueChange = {
                            if (it.split(" ").size <= 6) {
                                viewModel.updateSceneText(activeScene, activeScene.narrationText, it)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricPurple,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        placeholder = { Text("Texto que pisca na tela. Ex: FATO REAL!", color = TextTertiary) },
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // --- Image Library Swapping Picker Dialog ---
    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = { Text("Substituir Imagem", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selecione um ativo da sua biblioteca para esta cena:", color = TextSecondary, fontSize = 11.sp)
                    
                    if (images.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sua biblioteca está vazia.", color = TextTertiary, fontSize = 12.sp)
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            itemsIndexed(images) { _, img ->
                                Card(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .fillMaxHeight()
                                        .border(
                                            width = if (activeScene.imageUriString == img.uriString) 2.dp else 0.dp,
                                            color = ElectricCyan,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            viewModel.updateSceneImage(activeScene, img)
                                            showImagePickerDialog = false
                                        },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = img.uriString,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .align(Alignment.BottomCenter)
                                                .padding(2.dp)
                                        ) {
                                            Text(
                                                text = img.name,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImagePickerDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun TimelineReelItem(
    scene: Scene,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val durationSec = (scene.endTimeMs - scene.startTimeMs) / 1000

    Card(
        modifier = Modifier
            .width(100.dp)
            .height(72.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ElectricCyan else DarkSurfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!scene.imageUriString.isNullOrEmpty()) {
                AsyncImage(
                    model = scene.imageUriString,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Image, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                }
            }

            // Time badge bottom right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${durationSec}s",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Index label top left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(if (isSelected) ElectricCyan else Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "#${index + 1}",
                    color = if (isSelected) Color.Black else Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
