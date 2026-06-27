package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.platform.LocalContext
import com.example.ui.ProjectViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: ProjectViewModel,
    onNavigateToEditor: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val isGenerating by viewModel.isGenerating.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var selectedStyle by remember { mutableStateOf("documentary") }
    var selectedMusic by remember { mutableStateOf("Cosmic Ambient") }

    val dbTemplates by viewModel.templates.collectAsState()
    val selectedTemplateForNewProject by viewModel.selectedTemplateForNewProject.collectAsState()

    // Autofill selectedStyle when preselected from the templates tab
    LaunchedEffect(selectedTemplateForNewProject) {
        selectedTemplateForNewProject?.let {
            selectedStyle = it
        }
    }

    LaunchedEffect(selectedStyle, dbTemplates) {
        val matchingTemplate = dbTemplates.find { it.name == selectedStyle }
        if (matchingTemplate != null) {
            selectedMusic = matchingTemplate.bgMusicName
        }
    }

    var projectTitle by remember { mutableStateOf("Mistérios do Espaço Sideral") }
    var narrationText by remember {
        mutableStateOf(
            "O universo guarda mistérios insondáveis. No coração das galáxias distantes, buracos negros supermassivos devoram estrelas inteiras em frações de segundos. Enquanto isso, estrelas de nêutrons giram centenas de vezes por segundo, emitindo feixes de radiação que cortam a escuridão cósmica. Nós somos apenas uma poeira estelar flutuando neste mar infinito de mistérios."
        )
    }

    // Audio recording simulator states
    var isRecording by remember { mutableStateOf(false) }
    var recordTimeSeconds by remember { mutableStateOf(0) }
    var voiceRecordedText by remember { mutableStateOf<String?>(null) }
    var selectedAudioSourceTab by remember { mutableStateOf(0) } // 0: Gravar, 1: Enviar Áudio Pronto

    val context = LocalContext.current
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAudioName by remember { mutableStateOf<String?>(null) }
    var selectedAudioDurationMs by remember { mutableStateOf(0L) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAudioUri = uri
            val name = getFileNameFromUri(context, uri)
            selectedAudioName = name
            val cleanTitle = name.substringBeforeLast(".")
            projectTitle = if (cleanTitle.isNotBlank()) cleanTitle else name
            voiceRecordedText = null // Reset recorded voice
            val durationMs = getAudioDurationMs(context, uri)
            selectedAudioDurationMs = durationMs
            val durationSec = durationMs / 1000
            val min = durationSec / 60
            val sec = durationSec % 60
            val durationStr = String.format("%02d:%02d", min, sec)
            narrationText = "Áudio pronto enviado: $name (${durationStr}). A inteligência artificial identificou a locução pronta e criará as legendas e cenas dinamicamente sincronizadas com o ritmo e pausas do áudio."
        }
    }

    // Viral templates
    val templates = listOf(
        TemplateScript(
            name = "🌌 Espaço",
            title = "O Enigma do Horizonte de Eventos",
            style = "documentary",
            music = "Cosmic Ambient",
            text = "O horizonte de eventos é a fronteira final da gravidade. Nada, nem mesmo a luz, pode escapar de sua atração colossal. Se você caísse em um buraco negro, o próprio tempo pareceria parar para quem o observa de fora, enquanto você seria espaguetificado pela força gravitacional extrema em segundos."
        ),
        TemplateScript(
            name = "🧠 Curiosidades",
            title = "3 Fatos Absurdos Sobre o Cérebro",
            style = "curiosities",
            music = "Synth Pulse",
            text = "Sabia que seu cérebro consome cerca de vinte por cento de toda a energia do seu corpo? Além disso, ele gera eletricidade suficiente para acender uma pequena lâmpada de LED. Por fim, as informações em seus neurônios viajam a incríveis quatrocentos quilômetros por hora!"
        ),
        TemplateScript(
            name = "📰 Notícia",
            title = "A Descoberta Histórica na Antártica",
            style = "news",
            music = "News Flash",
            text = "Cientistas acabam de perfurar um lago subterrâneo intocado na Antártica que ficou isolado por mais de quinze milhões de anos. O que eles encontraram lá dentro pode reescrever completamente a história da biologia molecular e a busca por vida em outros planetas gelados."
        ),
        TemplateScript(
            name = "🎬 Suspense",
            title = "O Enigma do Farol de Flannan",
            style = "suspense",
            music = "Cinematic Tension",
            text = "Em dezembro de mil novecentos, três guardas de um farol isolado simplesmente desapareceram sem deixar nenhum rastro. O jantar estava intocado na mesa, o diário de bordo relatava uma tempestade misteriosa que ninguém na costa presenciou, e a porta estava trancada por dentro."
        )
    )

    // Trigger navigation automatically when generation is completed and currentProject becomes active
    LaunchedEffect(isGenerating) {
        if (!isGenerating && viewModel.currentProject.value != null) {
            onNavigateToEditor()
        }
    }

    if (isGenerating) {
        // AI Automation Progress Overlay Screen
        AIProcessingOverlay()
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Estúdio de Criação", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // --- Top Instructions ---
                Text(
                    text = "Monte seu vídeo em 1 clique",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ElectricCyan,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // --- Title Input ---
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Título do Projeto", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = { projectTitle = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricPurple,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        singleLine = true,
                        placeholder = { Text("Ex: Mistérios Cósmicos", color = TextTertiary) }
                    )
                }

                // --- Voice / Audio Upload Tool Widget ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Sleek tab switcher
                        TabRow(
                            selectedTabIndex = selectedAudioSourceTab,
                            containerColor = Color.Transparent,
                            contentColor = ElectricCyan,
                            divider = {},
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Tab(
                                selected = selectedAudioSourceTab == 0,
                                onClick = { selectedAudioSourceTab = 0 },
                                text = { Text("🎙️ Gravar Voz", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                            )
                            Tab(
                                selected = selectedAudioSourceTab == 1,
                                onClick = { selectedAudioSourceTab = 1 },
                                text = { Text("📂 Enviar Áudio Pronto", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                            )
                        }

                        if (selectedAudioSourceTab == 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = null, tint = CoralRed)
                                Text("Gravar Minha Narração", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            }
                            Text(
                                text = "Grave sua própria voz e nossa IA transcreverá o áudio automaticamente sincronizando com as cenas.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isRecording) CoralRed else TextTertiary)
                                    )
                                    Text(
                                        text = if (isRecording) "Gravando... ${recordTimeSeconds}s" else "Microfone Pronto",
                                        color = if (isRecording) CoralRed else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (isRecording) {
                                            // Stop Recording
                                            isRecording = false
                                            voiceRecordedText = "O enigma do farol isolado intrigou a humanidade por décadas. Três homens sumiram sem deixar vestígios."
                                            narrationText = voiceRecordedText!!
                                            projectTitle = "Gravação de Voz"
                                        } else {
                                            // Start Recording Simulation
                                            isRecording = true
                                            recordTimeSeconds = 0
                                            coroutineScope.launch {
                                                while (isRecording) {
                                                    delay(1000)
                                                    recordTimeSeconds++
                                                    if (recordTimeSeconds >= 10) {
                                                        isRecording = false
                                                        voiceRecordedText = "A inteligência artificial está revolucionando a criação de vídeos de forma simples e direta no celular."
                                                        narrationText = voiceRecordedText!!
                                                        projectTitle = "Gravação de Voz"
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRecording) CoralRed else ElectricPurple,
                                         contentColor = if (isRecording) Color(0xFF601410) else Color(0xFF003355)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord, null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isRecording) "Parar" else "Gravar")
                                }
                            }

                            voiceRecordedText?.let {
                                Text(
                                    text = "Transcrição Automática por IA: \"$it\"",
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = ElectricCyan,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Audiotrack, contentDescription = null, tint = ElectricCyan)
                                Text("Carregar Locução / Áudio Pronto", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            }
                            Text(
                                text = "Selecione um arquivo de áudio ou narração pré-gravada do seu dispositivo. Nossa IA irá analisá-la e sincronizar perfeitamente as cenas e legendas.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (selectedAudioName != null) {
                                        Text(
                                            text = "Arquivo: $selectedAudioName",
                                            color = ElectricCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Carregado com sucesso!",
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    } else {
                                        Text(
                                             text = "Nenhum arquivo selecionado",
                                             color = TextTertiary,
                                             fontSize = 12.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        audioPickerLauncher.launch("audio/*")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElectricCyan,
                                        contentColor = Color(0xFF003355)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("upload_audio_file_button")
                                ) {
                                    Icon(Icons.Filled.Upload, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (selectedAudioName != null) "Alterar Áudio" else "Selecionar Áudio", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // --- Script Templates LazyRow ---
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Exemplos de Roteiro Viral (Use ou Edite)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(templates) { item ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = if (narrationText == item.text) ElectricCyan else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        narrationText = item.text
                                        projectTitle = item.title
                                        selectedStyle = item.style
                                        selectedMusic = item.music
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = item.name,
                                    color = if (narrationText == item.text) ElectricCyan else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // --- Narration Script Input ---
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Texto do Roteiro / Narração", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = narrationText,
                        onValueChange = { narrationText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("narration_text_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricPurple,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        placeholder = { Text("Insira o texto que será narrado no seu vídeo...", color = TextTertiary) }
                    )
                }

                // --- Style Template Selector ---
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Template de Estilo Visual", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("style_template_selector_row")
                    ) {
                        items(dbTemplates) { template ->
                            val style = template.name
                            val label = when (style) {
                                "documentary" -> "Documentário"
                                "suspense" -> "Suspense"
                                "curiosities" -> "Curiosidades"
                                "news" -> "Notícias"
                                else -> style.uppercase()
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedStyle == style) ElectricPurple else DarkSurface)
                                    .border(
                                        width = 1.dp,
                                        color = if (selectedStyle == style) Color.Transparent else DarkSurfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedStyle = style }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selectedStyle == style) Color(0xFF003355) else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // --- Background Music Selector ---
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Trilha de Música de Fundo", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(listOf("Silent", "Cosmic Ambient", "Synth Pulse", "News Flash", "Cinematic Tension")) { music ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedMusic == music) ElectricCyan.copy(alpha = 0.2f) else DarkSurface)
                                    .border(
                                        width = 1.dp,
                                        color = if (selectedMusic == music) ElectricCyan else DarkSurfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedMusic = music }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (music == "Silent") Icons.Filled.MusicOff else Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = if (selectedMusic == music) ElectricCyan else TextTertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = music,
                                        color = if (selectedMusic == music) ElectricCyan else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Automate Button ---
                Button(
                    onClick = {
                        if (projectTitle.isNotBlank() && narrationText.isNotBlank()) {
                            val audioUri = if (selectedAudioSourceTab == 1) selectedAudioUri?.toString() else null
                            val audioDuration = if (selectedAudioSourceTab == 1) selectedAudioDurationMs else 0L
                            viewModel.createNewProject(
                                title = projectTitle,
                                narrationText = narrationText,
                                templateStyle = selectedStyle,
                                bgMusic = selectedMusic,
                                audioUriString = audioUri,
                                audioDurationMs = audioDuration
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_project_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple, contentColor = Color(0xFF003355)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "GERAR VÍDEO COMPLETO POR IA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun AIProcessingOverlay() {
    val steps = listOf(
        "Carregando arquivos de áudio...",
        "IA transcrevendo áudio em blocos...",
        "Dividindo transcrição em cenas de 4 a 7 segundos...",
        "Analisando emoções e gerando legendas curtas...",
        "Pesquisando na biblioteca de imagens por tags de relevância...",
        "Encontrando melhores correspondências visuais...",
        "Montando linha do tempo e Ken Burns dinâmicos..."
    )
    
    var currentStepIdx by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (currentStepIdx < steps.size - 1) {
            delay(1500)
            currentStepIdx++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            CircularProgressIndicator(
                color = ElectricCyan,
                strokeWidth = 4.dp,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Montando Vídeo com Inteligência Artificial",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = steps[currentStepIdx],
                transitionSpec = {
                    fadeIn() + slideInVertically { it } togetherWith fadeOut() + slideOutVertically { -it }
                },
                label = "step_animation"
            ) { step ->
                Text(
                    text = step,
                    color = ElectricCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Aguarde, isso pode levar alguns segundos.",
                color = TextTertiary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class TemplateScript(
    val name: String,
    val title: String,
    val style: String,
    val music: String,
    val text: String
)

fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result ?: "audio_pronto.mp3"
}

fun getAudioDurationMs(context: android.content.Context, uri: Uri): Long {
    var duration = 0L
    try {
        val retriever = android.media.MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
        duration = time?.toLong() ?: 0L
        retriever.release()
    } catch (e: Exception) {
        e.printStackTrace()
        // Fallback using MediaPlayer
        try {
            val mediaPlayer = android.media.MediaPlayer.create(context, uri)
            if (mediaPlayer != null) {
                duration = mediaPlayer.duration.toLong()
                mediaPlayer.release()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
    return if (duration > 0) duration else 15000L // Default to 15s if failed
}


