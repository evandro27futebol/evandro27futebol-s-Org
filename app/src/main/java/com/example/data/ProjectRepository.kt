package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ProjectRepository(private val db: AppDatabase) {
    private val tag = "ProjectRepository"
    private val geminiRepo = GeminiRepository()

    val projects: Flow<List<Project>> = db.projectDao().getAllProjects()
    val images: Flow<List<UserImage>> = db.userImageDao().getAllImages()

    fun getProject(id: Int): Flow<Project?> = db.projectDao().getProjectById(id)
    suspend fun getProjectSync(id: Int): Project? = db.projectDao().getProjectByIdSync(id)

    fun getScenes(projectId: Int): Flow<List<Scene>> = db.sceneDao().getScenesForProject(projectId)
    suspend fun getScenesSync(projectId: Int): List<Scene> = db.sceneDao().getScenesForProjectSync(projectId)

    fun searchImages(query: String): Flow<List<UserImage>> {
        return db.userImageDao().searchImages("%$query%")
    }

    suspend fun insertProject(project: Project): Long = db.projectDao().insertProject(project)
    suspend fun updateProject(project: Project) = db.projectDao().updateProject(project)
    suspend fun deleteProject(project: Project) = db.projectDao().deleteProject(project)

    suspend fun insertImage(image: UserImage): Long = db.userImageDao().insertImage(image)
    suspend fun deleteImage(image: UserImage) = db.userImageDao().deleteImage(image)
    suspend fun updateImage(image: UserImage) = db.userImageDao().updateImage(image)

    suspend fun insertScene(scene: Scene): Long = db.sceneDao().insertScene(scene)
    suspend fun updateScene(scene: Scene) = db.sceneDao().updateScene(scene)
    suspend fun deleteScene(scene: Scene) = db.sceneDao().deleteScene(scene)

    val templates: Flow<List<VideoTemplate>> = db.videoTemplateDao().getAllTemplates()
    suspend fun getTemplatesSync(): List<VideoTemplate> = db.videoTemplateDao().getAllTemplatesSync()
    suspend fun getTemplateByNameSync(name: String): VideoTemplate? = db.videoTemplateDao().getTemplateByName(name)
    suspend fun insertTemplate(template: VideoTemplate): Long = db.videoTemplateDao().insertTemplate(template)
    suspend fun updateTemplate(template: VideoTemplate) = db.videoTemplateDao().updateTemplate(template)
    suspend fun deleteTemplate(template: VideoTemplate) = db.videoTemplateDao().deleteTemplate(template)

    /**
     * Analyzes and saves an image with AI tags.
     */
    suspend fun importUserImage(name: String, uriString: String, bitmap: Bitmap?): Long {
        val analysis = if (bitmap != null && geminiRepo.isApiKeyConfigured()) {
            geminiRepo.analyzeImageWithIA(bitmap)
        } else {
            // Default tags based on name
            val tags = "upload, biblioteca, " + name.lowercase().split(" ", "_", "-").filter { it.length > 2 }.joinToString(", ")
            ImageAnalysisResult(
                name = name,
                category = "Biblioteca",
                tags = tags,
                visualDescription = "Imagem importada pelo usuário."
            )
        }

        val userImage = UserImage(
            name = analysis.name,
            uriString = uriString,
            tags = analysis.tags,
            category = analysis.category,
            visualDescription = analysis.visualDescription
        )
        return insertImage(userImage)
    }

    /**
     * Orchestrates the automatic creation of a project:
     * 1. Creates Project record.
     * 2. Calls Gemini (or fallback) to divide narration into scenes.
     * 3. For each scene, intelligently searches user's images for tag overlaps.
     * 4. Assigns matching images or beautiful thematic gradient placeholders if none found.
     */
    suspend fun createProjectWithAutomation(
        title: String,
        narrationText: String,
        styleTemplate: String,
        audioUriString: String? = null,
        audioDurationMs: Long = 0,
        bgMusicName: String? = "Silent"
    ): Int {
        // Look up custom template
        val customTemplate = db.videoTemplateDao().getTemplateByName(styleTemplate)
        
        val finalMusicName = customTemplate?.bgMusicName ?: bgMusicName ?: "Silent"
        val finalMusicVolume = customTemplate?.bgMusicVolume ?: 0.15f

        // Create the initial project
        val project = Project(
            title = title,
            audioUriString = audioUriString,
            audioDurationMs = audioDurationMs,
            templateStyle = styleTemplate,
            bgMusicName = finalMusicName,
            bgMusicVolume = finalMusicVolume,
            isCompleted = false
        )
        val projectId = insertProject(project).toInt()

        try {
            // Call Gemini to divide scenes
            val sceneDataList = geminiRepo.divideNarrationIntoScenes(narrationText, styleTemplate)
            
            // Fetch all library images for smart matching
            val allLibraryImages = db.userImageDao().getAllImagesSync()

            var currentTimeMs = 0L
            val scenesToInsert = sceneDataList.map { data ->
                val durationSec = customTemplate?.defaultSceneDurationSeconds ?: data.durationSeconds
                val durationMs = durationSec * 1000L
                val endMs = currentTimeMs + durationMs

                // Match image smart search
                val matchedImage = findMatchingImageForScene(data, allLibraryImages)

                val motion = when (val customMotion = customTemplate?.motionStyle) {
                    "Random" -> data.motionStyle
                    null -> data.motionStyle
                    else -> customMotion
                }

                val originalOverlay = data.overlayText
                val styledOverlay = when (customTemplate?.overlayTextType) {
                    "Uppercase Bold" -> originalOverlay.uppercase()
                    "Italic Highlight" -> if (originalOverlay.isNotBlank()) "\"$originalOverlay\"" else ""
                    "Subtitle Center" -> originalOverlay
                    "Large Splash" -> originalOverlay.uppercase()
                    else -> originalOverlay
                }

                val scene = Scene(
                    projectId = projectId,
                    sequenceNumber = data.sequenceNumber,
                    narrationText = data.narrationText,
                    startTimeMs = currentTimeMs,
                    endTimeMs = endMs,
                    imageUriString = matchedImage?.uriString,
                    imageId = matchedImage?.id,
                    overlayText = styledOverlay,
                    motionStyle = motion,
                    emotion = data.emotion,
                    keywords = data.keywords
                )

                currentTimeMs = endMs
                scene
            }

            db.sceneDao().insertScenes(scenesToInsert)
            
            // Update project with final audio duration if it was generated/automatic
            if (audioDurationMs == 0L) {
                val updatedProj = project.copy(id = projectId, audioDurationMs = currentTimeMs)
                updateProject(updatedProj)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to automate project creation: ${e.message}", e)
            // Create at least one baseline scene so the project is valid
            val fallbackScene = Scene(
                projectId = projectId,
                sequenceNumber = 1,
                narrationText = narrationText,
                startTimeMs = 0,
                endTimeMs = 5000,
                overlayText = "CRIADO!",
                motionStyle = customTemplate?.motionStyle ?: "Zoom In"
            )
            db.sceneDao().insertScene(fallbackScene)
        }

        return projectId
    }

    private fun findMatchingImageForScene(scene: SceneData, images: List<UserImage>): UserImage? {
        if (images.isEmpty()) return null

        val keywords = scene.keywords.lowercase().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (keywords.isEmpty()) return images.first() // default to first

        // Calculate score for each image
        var bestScore = 0
        var bestImage: UserImage? = null

        for (img in images) {
            val tagsList = img.tags.lowercase().split(",").map { it.trim() }
            val nameWords = img.name.lowercase().split(" ", "_", "-")
            val category = img.category.lowercase()

            var score = 0
            for (kw in keywords) {
                // Exact tag match
                if (tagsList.contains(kw)) score += 10
                // Partial tag match
                else if (tagsList.any { it.contains(kw) }) score += 4

                // Name match
                if (nameWords.contains(kw)) score += 6
                else if (img.name.lowercase().contains(kw)) score += 2

                // Category match
                if (category == kw) score += 8
            }

            if (score > bestScore) {
                bestScore = score
                bestImage = img
            }
        }

        // Return best match if score > 0, else return first image as default
        return bestImage ?: images.randomOrNull() ?: images.firstOrNull()
    }
}
