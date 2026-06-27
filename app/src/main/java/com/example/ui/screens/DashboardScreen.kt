package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Project
import com.example.ui.ProjectViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: ProjectViewModel,
    onNavigateToUpload: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToEditor: () -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val images by viewModel.imageLibrary.collectAsState()
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Faceless AI logo",
                            tint = ElectricCyan,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "Faceless AI Studio",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToLibrary,
                        modifier = Modifier.testTag("nav_library_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = "Biblioteca de Imagens",
                            tint = ElectricCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.selectTemplateForNewProject(null)
                    onNavigateToUpload()
                },
                containerColor = ElectricPurple,
                contentColor = Color(0xFF003355),
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Filled.Add, "Criar novo projeto") },
                text = { Text("Novo Vídeo Faceless", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .testTag("create_project_fab")
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        var selectedTab by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // --- Hero Showcase Banner ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(ElectricPurple, DarkSurfaceVariant)
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MovieCreation,
                            contentDescription = "Movie Creation",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Gere Vídeos Curtos com IA",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Upload de áudio, legendas de impacto e efeitos dinâmicos.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Fast Metrics Bar ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Projetos Criados",
                    value = projects.size.toString(),
                    icon = Icons.Outlined.VideoSettings,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Minha Biblioteca",
                    value = images.size.toString(),
                    icon = Icons.Outlined.ImageSearch,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Tab Selector ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkBackground,
                contentColor = ElectricCyan,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("MEUS PROJETOS", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Filled.VideoLibrary, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("MODELOS", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Filled.DashboardCustomize, null, modifier = Modifier.size(18.dp)) }
                )
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                modifier = Modifier.weight(1f),
                label = "dashboard_tabs"
            ) { tab ->
                if (tab == 0) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // --- Project Grid / Empty State ---
                        if (projects.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(bottom = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStateView(onActionClick = {
                                    viewModel.selectTemplateForNewProject(null)
                                    onNavigateToUpload()
                                })
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("projects_grid")
                            ) {
                                items(projects) { project ->
                                    ProjectItemRow(
                                        project = project,
                                        onSelect = {
                                            viewModel.selectProject(project)
                                            onNavigateToEditor()
                                        },
                                        onDeleteRequest = { projectToDelete = project }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
                                }
                            }
                        }
                    }
                } else {
                    TemplatesTab(
                        viewModel = viewModel,
                        onApplyTemplate = { template ->
                            viewModel.selectTemplateForNewProject(template.name)
                            onNavigateToUpload()
                        }
                    )
                }
            }
        }
    }

    // --- Delete Confirmation Dialog ---
    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Excluir Projeto", color = TextPrimary) },
            text = { Text("Deseja realmente apagar o projeto '${projectToDelete?.title}'? Essa ação é irreversível.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        projectToDelete?.let { viewModel.deleteProject(it) }
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CoralRed)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { projectToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(DarkSurfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = title, color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(text = value, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProjectItemRow(
    project: Project,
    onSelect: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val durationSec = project.audioDurationMs / 1000
    val formattedDate = remember(project.dateCreated) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(project.dateCreated))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("project_item_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stylized Icon representation
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DarkSurfaceVariant, DarkBackground)
                        ),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (project.templateStyle) {
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
                    tint = ElectricCyan,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Estilo: ${project.templateStyle.uppercase()}",
                        color = ElectricCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•",
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${durationSec}s",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Criado em $formattedDate",
                    color = TextTertiary,
                    fontSize = 10.sp
                )
            }

            IconButton(onClick = onDeleteRequest) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Excluir projeto",
                    tint = CoralRed.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(onActionClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(DarkSurface, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.VideoCall,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nenhum Vídeo Criado",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Toque no botão abaixo para criar seu primeiro vídeo faceless automaticamente em segundos.",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onActionClick,
            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.AutoAwesome, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Começar Agora", fontWeight = FontWeight.Bold)
        }
    }
}
