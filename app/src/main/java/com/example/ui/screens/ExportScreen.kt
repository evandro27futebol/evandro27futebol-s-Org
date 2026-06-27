package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ProjectViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ProjectViewModel,
    onNavigateBack: () -> Unit
) {
    val project by viewModel.currentProject.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val progress by viewModel.exportProgress.collectAsState()
    val logText by viewModel.exportLog.collectAsState()
    val success by viewModel.exportSuccessful.collectAsState()

    val scrollState = rememberScrollState()

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ElectricCyan)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exportação & Estilo", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (success) {
                // --- Celebration / Export Successful Layout ---
                ExportSuccessView(
                    onDownload = {
                        // Simulated Download
                    },
                    onShare = {
                        // Simulated Share
                    },
                    onBack = onNavigateBack
                )
            } else {
                // --- Style Template Selection Grid ---
                Text(
                    text = "Ajustar Template Visual",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("documentary", "suspense", "curiosities", "news", "sports").forEach { style ->
                        val (title, desc) = when (style) {
                            "documentary" -> "Documentário Histórico" to "Cores sepia quentes, transições lentas, fontes clássicas elegantes."
                            "suspense" -> "Mistério e Suspense" to "Filtro frio dessaturado, cortes abruptos, fontes em caixa alta com sombras."
                            "curiosities" -> "Curiosidades Virais" to "Aceleração sutil, legendas com destaque amarelo, trilha animada."
                            "news" -> "Noticiário Noticioso" to "Layout formal de notícias, cores neutras, tarja de legenda inferior."
                            "sports" -> "Esportivo & Dinâmico" to "Filtros de alto contraste, cortes velozes, legendas itálicas agressivas."
                            else -> style.capitalize() to ""
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (project!!.templateStyle == style) 1.5.dp else 1.dp,
                                    color = if (project!!.templateStyle == style) ElectricCyan else DarkSurfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                        .clickable { viewModel.updateActiveProjectStyle(style) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (project!!.templateStyle == style) ElectricCyan.copy(alpha = 0.05f) else DarkSurface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (project!!.templateStyle == style) ElectricCyan else DarkSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when (style) {
                                        "documentary" -> Icons.Filled.HistoryEdu
                                        "suspense" -> Icons.Filled.VisibilityOff
                                        "curiosities" -> Icons.Filled.HelpOutline
                                        "sports" -> Icons.Filled.SportsVolleyball
                                        "news" -> Icons.Filled.Feed
                                        else -> Icons.Filled.MovieCreation
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (project!!.templateStyle == style) Color.Black else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = if (project!!.templateStyle == style) ElectricCyan else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = desc,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                if (project!!.templateStyle == style) {
                                    Icon(Icons.Filled.CheckCircle, "Selecionado", tint = ElectricCyan)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // --- Resolution Output Metadata Card ---
                Text(
                    text = "Configurações de Saída do Vídeo",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VideoSpecRow(label = "Resolução", value = "1920x1080 (Full HD)")
                        VideoSpecRow(label = "Formato do Quadro", value = "16:9 Widescreen (YouTube)")
                        VideoSpecRow(label = "Taxa de Quadros", value = "30 FPS")
                        VideoSpecRow(label = "Codec de Render", value = "H.264 / AAC MP4")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isExporting) {
                    // --- Active Rendering Console Progress ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Renderizando...",
                                    color = ElectricCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    color = ElectricCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            LinearProgressIndicator(
                                progress = progress,
                                color = ElectricCyan,
                                trackColor = DarkSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )

                            Text(
                                text = "> $logText",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // --- Render Trigger Button ---
                    Button(
                        onClick = { viewModel.startVideoExport() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("render_video_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color(0xFF003355)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.VideoSettings, null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("INICIAR RENDERIZAÇÃO EM MP4", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun VideoSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(text = value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun ExportSuccessView(
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(ElectricCyan.copy(alpha = 0.15f), RoundedCornerShape(50))
                .border(2.dp, ElectricCyan, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(44.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Vídeo Pronto com Sucesso!",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "O arquivo MP4 1080p foi renderizado, polido e salvo nos seus ativos locais.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                    Text("Otimizado para Shorts e YouTube", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                }
                Text(
                    text = "As legendas e fontes foram colorizadas dinamicamente de acordo com o template de estilo selecionado.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onDownload,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color(0xFF003355)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Download, null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salvar MP4", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Share, null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartilhar", fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
            border = borderStroke(1.dp, ElectricCyan),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Filled.Home, null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Voltar para Dashboard", fontWeight = FontWeight.Bold)
        }
    }
}

// Simple helper border stroke
@Composable
fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)
