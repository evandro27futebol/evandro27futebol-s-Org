package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream

class ProjectViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "ProjectViewModel"
    private val repository: ProjectRepository
    private val geminiRepo = GeminiRepository()

    // --- State Streams ---
    val projects: StateFlow<List<Project>>
    val imageLibrary: StateFlow<List<UserImage>>
    val templates: StateFlow<List<VideoTemplate>>

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _currentScenes = MutableStateFlow<List<Scene>>(emptyList())
    val currentScenes: StateFlow<List<Scene>> = _currentScenes.asStateFlow()

    private val _selectedSceneIndex = MutableStateFlow(0)
    val selectedSceneIndex: StateFlow<Int> = _selectedSceneIndex.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _isGeneratingTemplate = MutableStateFlow(false)
    val isGeneratingTemplate: StateFlow<Boolean> = _isGeneratingTemplate.asStateFlow()

    private val _selectedTemplateForNewProject = MutableStateFlow<String?>(null)
    val selectedTemplateForNewProject: StateFlow<String?> = _selectedTemplateForNewProject.asStateFlow()

    fun selectTemplateForNewProject(templateName: String?) {
        _selectedTemplateForNewProject.value = templateName
    }

    // --- Image Search ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- Video Preview States ---
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs: StateFlow<Long> = _currentTimeMs.asStateFlow()

    // --- Export Screen States ---
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _exportLog = MutableStateFlow("Pronto para exportar.")
    val exportLog: StateFlow<String> = _exportLog.asStateFlow()

    private val _exportSuccessful = MutableStateFlow(false)
    val exportSuccessful: StateFlow<Boolean> = _exportSuccessful.asStateFlow()

    private var playbackJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProjectRepository(database)

        projects = repository.projects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        templates = repository.templates.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Combine search query and repository images
        imageLibrary = _searchQuery
            .debounce(200)
            .flatMapLatest { query ->
                if (query.isEmpty()) repository.images else repository.searchImages(query)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Seed initial images if library is empty to provide immediate value
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.images.firstOrNull()
            if (existing.isNullOrEmpty()) {
                seedDefaultImages()
            }
        }

        // Seed initial templates if empty
        viewModelScope.launch(Dispatchers.IO) {
            val existingTemplates = repository.getTemplatesSync()
            if (existingTemplates.isEmpty()) {
                seedDefaultTemplates()
            }
        }
    }

    // --- Project Operations ---

    fun selectProject(project: Project) {
        _currentProject.value = project
        viewModelScope.launch {
            repository.getScenes(project.id).collect { scenes ->
                _currentScenes.value = scenes
                if (scenes.isNotEmpty() && _selectedSceneIndex.value >= scenes.size) {
                    _selectedSceneIndex.value = 0
                }
            }
        }
    }

    fun deselectProject() {
        _currentProject.value = null
        _currentScenes.value = emptyList()
        _selectedSceneIndex.value = 0
        pausePreview()
    }

    fun selectScene(index: Int) {
        if (index in _currentScenes.value.indices) {
            _selectedSceneIndex.value = index
            val scene = _currentScenes.value[index]
            _currentTimeMs.value = scene.startTimeMs
        }
    }

    fun createNewProject(
        title: String,
        narrationText: String,
        templateStyle: String,
        bgMusic: String,
        audioUriString: String? = null,
        audioDurationMs: Long = 0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            try {
                val projectId = repository.createProjectWithAutomation(
                    title = title,
                    narrationText = narrationText,
                    styleTemplate = templateStyle,
                    audioUriString = audioUriString,
                    audioDurationMs = audioDurationMs,
                    bgMusicName = bgMusic
                )
                val proj = repository.getProjectSync(projectId)
                if (proj != null) {
                    selectProject(proj)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error creating project: ${e.message}")
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_currentProject.value?.id == project.id) {
                deselectProject()
            }
            repository.deleteProject(project)
        }
    }

    fun updateActiveProjectStyle(style: String) {
        val proj = _currentProject.value ?: return
        val updated = proj.copy(templateStyle = style)
        _currentProject.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProject(updated)
        }
    }

    fun updateActiveProjectMusic(musicName: String, volume: Float) {
        val proj = _currentProject.value ?: return
        val updated = proj.copy(bgMusicName = musicName, bgMusicVolume = volume)
        _currentProject.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProject(updated)
        }
    }

    // --- Scene Operations ---

    fun updateSceneText(scene: Scene, narration: String, overlay: String) {
        val updated = scene.copy(narrationText = narration, overlayText = overlay)
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateScene(updated)
        }
    }

    fun updateSceneMotion(scene: Scene, motion: String) {
        val updated = scene.copy(motionStyle = motion)
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateScene(updated)
        }
    }

    fun updateSceneImage(scene: Scene, image: UserImage) {
        val updated = scene.copy(imageUriString = image.uriString, imageId = image.id)
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateScene(updated)
        }
    }

    fun updateSceneDuration(scene: Scene, seconds: Int) {
        val currentList = _currentScenes.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == scene.id }
        if (index != -1) {
            val deltaMs = (seconds * 1000L) - (scene.endTimeMs - scene.startTimeMs)
            
            // Adjust all subsequent scene start/end times
            var curStartMs = scene.startTimeMs
            for (i in index until currentList.size) {
                val sc = currentList[i]
                val duration = if (i == index) seconds * 1000L else (sc.endTimeMs - sc.startTimeMs)
                currentList[i] = sc.copy(
                    startTimeMs = curStartMs,
                    endTimeMs = curStartMs + duration
                )
                curStartMs += duration
            }

            // Save all updated scenes
            viewModelScope.launch(Dispatchers.IO) {
                currentList.forEach { repository.updateScene(it) }
                
                // Update project duration
                _currentProject.value?.let { proj ->
                    repository.updateProject(proj.copy(audioDurationMs = curStartMs))
                }
            }
        }
    }

    // --- Image Library Operations ---

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun uploadImage(name: String, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isUploadingImage.value = true
            try {
                val context = getApplication<Application>()
                var bitmap: Bitmap? = null
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        bitmap = BitmapFactory.decodeStream(inputStream)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to decode bitmap from Uri: ${e.message}")
                }

                repository.importUserImage(name, uri.toString(), bitmap)
            } catch (e: Exception) {
                Log.e(tag, "Error saving image: ${e.message}")
            } finally {
                _isUploadingImage.value = false
            }
        }
    }

    fun deleteLibraryImage(image: UserImage) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteImage(image)
        }
    }

    // --- Video Preview Engine (Continuous State Ticker) ---

    fun togglePlayback() {
        if (_isPlaying.value) {
            pausePreview()
        } else {
            playPreview()
        }
    }

    fun playPreview() {
        val totalDuration = _currentProject.value?.audioDurationMs ?: 0L
        if (totalDuration == 0L) return

        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch(Dispatchers.Main) {
            if (_currentTimeMs.value >= totalDuration) {
                _currentTimeMs.value = 0L
            }

            var lastTime = System.currentTimeMillis()
            while (_isPlaying.value && _currentTimeMs.value < totalDuration) {
                delay(33) // ~30 FPS Ticker
                val now = System.currentTimeMillis()
                val delta = now - lastTime
                lastTime = now

                val nextTime = _currentTimeMs.value + delta
                if (nextTime >= totalDuration) {
                    _currentTimeMs.value = totalDuration
                    _isPlaying.value = false
                } else {
                    _currentTimeMs.value = nextTime
                }
            }
        }
    }

    fun pausePreview() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun seekPreview(timeMs: Long) {
        val totalDuration = _currentProject.value?.audioDurationMs ?: 0L
        _currentTimeMs.value = timeMs.coerceIn(0, totalDuration)
    }

    // --- Video Export Simulation ---

    fun startVideoExport() {
        val project = _currentProject.value ?: return
        val scenes = _currentScenes.value
        if (scenes.isEmpty()) return

        _isExporting.value = true
        _exportProgress.value = 0f
        _exportSuccessful.value = false
        _exportLog.value = "Iniciando pipeline de renderização..."

        viewModelScope.launch(Dispatchers.IO) {
            val logs = listOf(
                "Criando diretório temporário para frames...",
                "Carregando faixa de narração de áudio...",
                "Configurando resolução: 1920x1080 (16:9) a 30 FPS...",
                "Processando transições e efeitos de câmera Ken Burns...",
                "Renderizando Cena 1: Aplicando zoom suave...",
                "Sincronizando áudio da narração com legendas...",
                "Renderizando Cena 2: Movimentando imagem lateralmente...",
                "Inserindo overlays de impacto e legendas estilizadas...",
                "Renderizando Cena 3: Mesclando trilha de música de fundo...",
                "Ajustando volume relativo da trilha sonora (nível: ${(project.bgMusicVolume * 100).toInt()}%)...",
                "Renderizando Cena 4: Salvando quadros adicionais...",
                "Encodando vídeo h264 e muxing de faixas de áudio...",
                "Comprimindo arquivo final MP4...",
                "Exportação finalizada com sucesso! Salvo na Galeria."
            )

            val steps = logs.size
            for (i in 0 until steps) {
                delay(1200) // Realistic delay per step
                _exportProgress.value = (i + 1) / steps.toFloat()
                _exportLog.value = logs[i]
            }

            _isExporting.value = false
            _exportSuccessful.value = true
        }
    }

    // --- Default Image Seeder ---
    // Seed initial visual placeholder items so user starts with content
    private suspend fun seedDefaultImages() {
        val defaults = listOf(
            Pair("Universo Estelar", "android.resource://com.example/drawable/ic_launcher_background"),
            Pair("Tecnologia e Código", "android.resource://com.example/drawable/ic_launcher_foreground"),
            Pair("Galáxia Cósmica", "android.resource://com.example/mipmap/ic_launcher_round"),
            Pair("Natureza Abstrata", "android.resource://com.example/mipmap/ic_launcher")
        )

        defaults.forEachIndexed { i, d ->
            repository.insertImage(
                UserImage(
                    name = d.first,
                    uriString = d.second,
                    tags = "espaço, galáxia, cosmos, universo, tecnologia, abstrato",
                    category = if (i % 2 == 0) "Espaço" else "Tecnologia",
                    visualDescription = "Imagem de amostra para preencher a galeria."
                )
            )
        }
    }

    // --- Custom Template Operations ---

    fun createTemplate(template: VideoTemplate) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTemplate(template)
        }
    }

    fun updateTemplate(template: VideoTemplate) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTemplate(template)
        }
    }

    fun deleteTemplate(template: VideoTemplate) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTemplate(template)
        }
    }

    fun generateTemplateWithIA(prompt: String, onComplete: (VideoTemplate) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isGeneratingTemplate.value = true
            try {
                val suggestion = geminiRepo.generateTemplateWithIA(prompt)
                if (suggestion != null) {
                    val template = VideoTemplate(
                        name = suggestion.name,
                        primaryColorHex = suggestion.primaryColorHex,
                        backgroundColorHex = suggestion.backgroundColorHex,
                        textColorHex = suggestion.textColorHex,
                        fontStyle = suggestion.fontStyle,
                        motionStyle = suggestion.motionStyle,
                        defaultSceneDurationSeconds = suggestion.defaultSceneDurationSeconds,
                        overlayTextType = suggestion.overlayTextType,
                        bgMusicName = suggestion.bgMusicName,
                        bgMusicVolume = suggestion.bgMusicVolume,
                        isCustom = true
                    )
                    repository.insertTemplate(template)
                    onComplete(template)
                } else {
                    onError("Não foi possível obter sugestões do Gemini.")
                }
            } catch (e: Exception) {
                onError("Erro ao gerar com Gemini: ${e.message}")
            } finally {
                _isGeneratingTemplate.value = false
            }
        }
    }

    private suspend fun seedDefaultTemplates() {
        val defaults = listOf(
            VideoTemplate(
                name = "documentary",
                primaryColorHex = "#D1E4FF",
                backgroundColorHex = "#130902", // sepia
                textColorHex = "#E2E2E6",
                fontStyle = "Sans Serif",
                motionStyle = "Zoom In",
                defaultSceneDurationSeconds = 5,
                overlayTextType = "Uppercase Bold",
                bgMusicName = "Cosmic Ambient",
                bgMusicVolume = 0.15f,
                isCustom = false
            ),
            VideoTemplate(
                name = "suspense",
                primaryColorHex = "#98FFB3",
                backgroundColorHex = "#030E14", // deep cold dark
                textColorHex = "#C4C8C5",
                fontStyle = "Serif",
                motionStyle = "Pan Left",
                defaultSceneDurationSeconds = 6,
                overlayTextType = "Italic Highlight",
                bgMusicName = "Cinematic Tension",
                bgMusicVolume = 0.25f,
                isCustom = false
            ),
            VideoTemplate(
                name = "news",
                primaryColorHex = "#D0BCFF",
                backgroundColorHex = "#020914", // blue dark
                textColorHex = "#E6E1E5",
                fontStyle = "Sans Serif",
                motionStyle = "Static",
                defaultSceneDurationSeconds = 5,
                overlayTextType = "Subtitle Center",
                bgMusicName = "News Flash",
                bgMusicVolume = 0.12f,
                isCustom = false
            ),
            VideoTemplate(
                name = "curiosities",
                primaryColorHex = "#FFB2BE",
                backgroundColorHex = "#1A1C1E",
                textColorHex = "#E2E2E6",
                fontStyle = "Space Grotesk",
                motionStyle = "Zoom Out",
                defaultSceneDurationSeconds = 4,
                overlayTextType = "Large Splash",
                bgMusicName = "Synth Pulse",
                bgMusicVolume = 0.18f,
                isCustom = false
            )
        )
        defaults.forEach { repository.insertTemplate(it) }
    }
}
