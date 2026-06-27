package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.UserImage
import com.example.ui.ProjectViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: ProjectViewModel,
    onNavigateBack: () -> Unit
) {
    val images by viewModel.imageLibrary.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isUploading by viewModel.isUploadingImage.collectAsState()

    var selectedImageForDetail by remember { mutableStateOf<UserImage?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var manualImageName by remember { mutableStateOf("") }
    var manualImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    // Set up the system photo picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            manualImageUri = uri
            showAddDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Galeria de Ativos", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = ElectricCyan)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Quick demo add presets
                        val presets = listOf(
                            Pair("Planeta Vermelho", "android.resource://com.example/drawable/ic_launcher_background"),
                            Pair("Cidade Cibernética", "android.resource://com.example/drawable/ic_launcher_foreground"),
                            Pair("Espaço Cósmico", "android.resource://com.example/mipmap/ic_launcher_round"),
                            Pair("Buraco Negro", "android.resource://com.example/mipmap/ic_launcher")
                        )
                        val chosen = presets.random()
                        viewModel.uploadImage(chosen.first, Uri.parse(chosen.second))
                    }) {
                        Icon(Icons.Filled.AutoAwesome, "Adicionar Presets Rápidos", tint = ElectricCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                containerColor = ElectricPurple,
                contentColor = Color(0xFF003355),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("upload_image_fab")
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, "Importar Imagem")
            }
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
            // --- Search Field ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("image_search_bar"),
                placeholder = { Text("Buscar por tags, nome ou categoria...", color = TextTertiary, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, "Buscar", tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Close, "Limpar", tint = TextSecondary)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricPurple,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (isUploading) {
                LinearProgressIndicator(
                    color = ElectricCyan,
                    trackColor = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- Images Grid ---
            if (images.isEmpty()) {
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
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhuma Imagem na Biblioteca",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Importe imagens ou use os presets no topo para iniciar o slideshow.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("library_images_grid")
                ) {
                    items(images) { image ->
                        ImageGalleryCard(
                            image = image,
                            onSelect = { selectedImageForDetail = image },
                            onDelete = { viewModel.deleteLibraryImage(image) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
                    }
                }
            }
        }
    }

    // --- Image Detail Sheet / Dialog ---
    selectedImageForDetail?.let { image ->
        AlertDialog(
            onDismissRequest = { selectedImageForDetail = null },
            title = {
                Text(
                    text = image.name,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground)
                    ) {
                        AsyncImage(
                            model = image.uriString,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Categoria: ${image.category}",
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Descrição: ${image.visualDescription}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Render Tags
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Tags de IA:",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        FlowRow(
                            horizontalGap = 6.dp,
                            verticalGap = 6.dp
                        ) {
                            image.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "#$tag", color = TextPrimary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { selectedImageForDetail = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = ElectricPurple)
                ) {
                    Text("Ok", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface
        )
    }

    // --- Import Detail Selection Dialog ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nomear Imagem", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Dê um título curto para ajudar a IA a encontrar a imagem para as cenas.", color = TextSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = manualImageName,
                        onValueChange = { manualImageName = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricPurple,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        singleLine = true,
                        placeholder = { Text("Ex: Astronauta no espaço", color = TextTertiary) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = if (manualImageName.isBlank()) "Imagem Galeria" else manualImageName
                        manualImageUri?.let { viewModel.uploadImage(name, it) }
                        showAddDialog = false
                        manualImageName = ""
                        manualImageUri = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ElectricCyan)
                ) {
                    Text("Importar com IA", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun ImageGalleryCard(
    image: UserImage,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = image.uriString,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Category tag top-left overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = image.category,
                    color = ElectricCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Title bottom details banner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = image.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = image.tags.split(",").take(2).joinToString(" "),
                            color = TextSecondary,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Excluir",
                            tint = CoralRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Dynamic FlowRow layout helper ---
@Composable
fun FlowRow(
    horizontalGap: androidx.compose.ui.unit.Dp = 0.dp,
    verticalGap: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content = content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val layoutWidth = constraints.maxWidth
        
        var x = 0
        var y = 0
        var rowHeight = 0
        val positions = mutableListOf<Pair<Int, Int>>()

        placeables.forEach { placeable ->
            if (x + placeable.width > layoutWidth) {
                x = 0
                y += rowHeight + verticalGap.roundToPx()
                rowHeight = 0
            }
            positions.add(x to y)
            x += placeable.width + horizontalGap.roundToPx()
            rowHeight = maxOf(rowHeight, placeable.height)
        }

        layout(
            width = layoutWidth,
            height = if (positions.isEmpty()) 0 else y + rowHeight
        ) {
            placeables.forEachIndexed { i, placeable ->
                val (posX, posY) = positions[i]
                placeable.place(posX, posY)
            }
        }
    }
}
