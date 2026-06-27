package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoTemplate
import com.example.ui.ProjectViewModel
import com.example.ui.theme.*

// --- Utility function to parse Hex color strings safely ---
fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.White
    }
}

// --- Helper to convert Color to Hex String for persistence ---
fun Color.toHex(): String {
    return String.format("#%06X", 0xFFFFFF and this.value.toLong().toInt())
}

@Composable
fun TemplatesTab(
    viewModel: ProjectViewModel,
    onApplyTemplate: (VideoTemplate) -> Unit
) {
    val templates by viewModel.templates.collectAsState()
    val isGeneratingTemplate by viewModel.isGeneratingTemplate.collectAsState()

    var showEditorDialog by remember { mutableStateOf(false) }
    var selectedTemplateForEdit by remember { mutableStateOf<VideoTemplate?>(null) }
    var showAiGeneratorDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        // --- Control Headers / Actions ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    selectedTemplateForEdit = null
                    showEditorDialog = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("create_template_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = ElectricCyan),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Icon(Icons.Filled.AddCircleOutline, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Novo Modelo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = { showAiGeneratorDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("generate_template_ia_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color(0xFF003355)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Criar com Gemini", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ElectricCyan)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("templates_list")
            ) {
                items(templates) { template ->
                    TemplateItemCard(
                        template = template,
                        onApply = { onApplyTemplate(template) },
                        onEdit = {
                            selectedTemplateForEdit = template
                            showEditorDialog = true
                        },
                        onDelete = { viewModel.deleteTemplate(template) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // padding for floating buttons/nav
                }
            }
        }
    }

    // --- Create / Edit Dialog ---
    if (showEditorDialog) {
        TemplateEditorDialog(
            template = selectedTemplateForEdit,
            onDismiss = { showEditorDialog = false },
            onSave = { updated ->
                if (selectedTemplateForEdit != null) {
                    viewModel.updateTemplate(updated)
                } else {
                    viewModel.createTemplate(updated)
                }
                showEditorDialog = false
            }
        )
    }

    // --- Gemini AI Designer Dialog ---
    if (showAiGeneratorDialog) {
        GeminiTemplateGeneratorDialog(
            isGenerating = isGeneratingTemplate,
            onDismiss = { showAiGeneratorDialog = false },
            onGenerate = { prompt ->
                viewModel.generateTemplateWithIA(
                    prompt = prompt,
                    onComplete = {
                        showAiGeneratorDialog = false
                    },
                    onError = {
                        // handled gracefully
                    }
                )
            }
        )
    }
}

@Composable
fun TemplateItemCard(
    template: VideoTemplate,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val primaryColor = remember(template.primaryColorHex) { template.primaryColorHex.toColor() }
    val bgColor = remember(template.backgroundColorHex) { template.backgroundColorHex.toColor() }
    val textColor = remember(template.textColorHex) { template.textColorHex.toColor() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (template.isCustom) ElectricPurple.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with Name & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = template.name.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (template.isCustom) "Modelo Customizado" else "Estilo Base",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (template.isCustom) ElectricPurple else ElectricCyan
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Editar Modelo",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (template.isCustom) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Excluir Modelo",
                                tint = CoralRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Color Palette and Font Preview Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color previews
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Visualização de Legenda",
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = when (template.fontStyle) {
                            "Serif" -> FontFamily.Serif
                            "Monospace" -> FontFamily.Monospace
                            "Space Grotesk" -> FontFamily.SansSerif
                            else -> FontFamily.Default
                        }
                    )
                    Text(
                        text = "Texto: ${template.overlayTextType}",
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Configuration Parameters Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ParamBadge(
                    icon = Icons.Outlined.Timer,
                    label = "${template.defaultSceneDurationSeconds}s/Cena",
                    modifier = Modifier.weight(1f)
                )
                ParamBadge(
                    icon = Icons.Outlined.MovieFilter,
                    label = template.motionStyle,
                    modifier = Modifier.weight(1f)
                )
                ParamBadge(
                    icon = Icons.Outlined.MusicNote,
                    label = if (template.bgMusicName == "Silent") "Sem música" else template.bgMusicName,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button: Use this template
            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF003355)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("CRIAR VÍDEO COM ESTE MODELO", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ParamBadge(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- FULL TEMPLATE EDITOR DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorDialog(
    template: VideoTemplate?,
    onDismiss: () -> Unit,
    onSave: (VideoTemplate) -> Unit
) {
    var name by remember { mutableStateOf(template?.name ?: "Novo Modelo") }
    var primaryColorHex by remember { mutableStateOf(template?.primaryColorHex ?: "#FF5E97") }
    var backgroundColorHex by remember { mutableStateOf(template?.backgroundColorHex ?: "#121212") }
    var textColorHex by remember { mutableStateOf(template?.textColorHex ?: "#FFFFFF") }
    var fontStyle by remember { mutableStateOf(template?.fontStyle ?: "Space Grotesk") }
    var motionStyle by remember { mutableStateOf(template?.motionStyle ?: "Zoom In") }
    var duration by remember { mutableStateOf(template?.defaultSceneDurationSeconds ?: 5) }
    var overlayTextType by remember { mutableStateOf(template?.overlayTextType ?: "Uppercase Bold") }
    var bgMusicName by remember { mutableStateOf(template?.bgMusicName ?: "Silent") }
    var bgMusicVolume by remember { mutableStateOf(template?.bgMusicVolume ?: 0.15f) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (template != null) "Editar Modelo" else "Criar Modelo",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Modelo") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricPurple,
                        unfocusedBorderColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Visual Styles (Colors Selection Grid)
                Text("Cores Visuais (Estilo)", fontWeight = FontWeight.Bold, color = ElectricCyan, fontSize = 12.sp)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quick Color Picker presets for Primary Color
                    Text("Cor de Destaque", color = TextSecondary, fontSize = 11.sp)
                    val colorPresets = listOf("#FF5E97", "#00F5FF", "#FFB300", "#10FA72", "#FFFFFF")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorPresets.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color.toColor())
                                    .border(
                                        width = if (primaryColorHex == color) 2.dp else 1.dp,
                                        color = if (primaryColorHex == color) ElectricCyan else Color.White.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .clickable { primaryColorHex = color }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Fundo de Tela", color = TextSecondary, fontSize = 11.sp)
                    val bgPresets = listOf("#121212", "#0A1128", "#120902", "#1F1F1F")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bgPresets.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color.toColor())
                                    .border(
                                        width = if (backgroundColorHex == color) 2.dp else 1.dp,
                                        color = if (backgroundColorHex == color) ElectricCyan else Color.White.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .clickable { backgroundColorHex = color }
                            )
                        }
                    }
                }

                // Font Style Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Estilo de Fonte", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Sans Serif", "Serif", "Monospace", "Space Grotesk").forEach { font ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (fontStyle == font) ElectricPurple else DarkSurfaceVariant)
                                    .clickable { fontStyle = font }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = font,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fontStyle == font) Color(0xFF003355) else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Ken Burns Motion Pattern
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Padrão de Movimento (Ken Burns)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Zoom In", "Zoom Out", "Pan Left", "Pan Right", "Static", "Random").forEach { motion ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (motionStyle == motion) ElectricCyan.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = if (motionStyle == motion) ElectricCyan else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { motionStyle = motion }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = motion,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (motionStyle == motion) ElectricCyan else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Scene duration configuration
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Duração Padrão de Cena: ${duration}s", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = duration.toFloat(),
                        onValueChange = { duration = it.toInt() },
                        valueRange = 4f..7f,
                        steps = 2,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                }

                // Overlay Text Style / Type
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Estilo de Legendas na Tela", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    listOf("Uppercase Bold", "Italic Highlight", "Subtitle Center", "Large Splash").forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { overlayTextType = type }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = overlayTextType == type,
                                onClick = { overlayTextType = type },
                                colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(type, color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }

                // Background Music Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Seleção de Música", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    listOf("Silent", "Cosmic Ambient", "Synth Pulse", "News Flash", "Cinematic Tension").forEach { music ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { bgMusicName = music }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = bgMusicName == music,
                                onClick = { bgMusicName = music },
                                colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(music, color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        VideoTemplate(
                            id = template?.id ?: 0,
                            name = name,
                            primaryColorHex = primaryColorHex,
                            backgroundColorHex = backgroundColorHex,
                            textColorHex = textColorHex,
                            fontStyle = fontStyle,
                            motionStyle = motionStyle,
                            defaultSceneDurationSeconds = duration,
                            overlayTextType = overlayTextType,
                            bgMusicName = bgMusicName,
                            bgMusicVolume = bgMusicVolume,
                            isCustom = template?.isCustom ?: true
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color(0xFF003355))
            ) {
                Text("Salvar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("Cancelar")
            }
        },
        containerColor = DarkSurface
    )
}

// --- GEMINI PROMPT DESIGNER DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiTemplateGeneratorDialog(
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = if (isGenerating) { {} } else onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.AutoAwesome, "Gemini", tint = ElectricCyan)
                Text("Sugerir com Gemini", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Diga ao Gemini que tipo de clima e estilo você quer para seu template, e ele criará as combinações perfeitas de cores, fonte, movimentos e música de fundo automaticamente.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ElectricCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Gemini analisando e desenhando o template...", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { Text("Ex: Clima de ficção científica retro neon futurista cyberpunk rápido e animado...", color = TextTertiary, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricPurple,
                            unfocusedBorderColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("gemini_prompt_input")
                    )
                }
            }
        },
        confirmButton = {
            if (!isGenerating) {
                Button(
                    onClick = {
                        if (prompt.isNotBlank()) {
                            onGenerate(prompt)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color(0xFF003355))
                ) {
                    Text("Sugerir Modelo", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isGenerating) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Fechar")
                }
            }
        },
        containerColor = DarkSurface
    )
}
