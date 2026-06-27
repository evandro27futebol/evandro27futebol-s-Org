package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val audioUriString: String? = null,
    val audioDurationMs: Long = 0,
    val templateStyle: String = "documentary", // documentary, suspense, curiosities, sports, news
    val bgMusicName: String? = "Silent",
    val bgMusicVolume: Float = 0.15f,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "scenes",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class Scene(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val sequenceNumber: Int,
    val narrationText: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val imageUriString: String? = null,
    val imageId: Int? = null,
    val overlayText: String = "",
    val motionStyle: String = "Zoom In", // "Zoom In", "Zoom Out", "Pan Left", "Pan Right", "Static"
    val emotion: String = "neutral",
    val keywords: String = ""
)

@Entity(tableName = "user_images")
data class UserImage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val uriString: String,
    val tags: String = "", // comma-separated
    val category: String = "General",
    val visualDescription: String = "",
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "video_templates")
data class VideoTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val primaryColorHex: String = "#D1E4FF",
    val backgroundColorHex: String = "#1A1C1E",
    val textColorHex: String = "#E2E2E6",
    val fontStyle: String = "Sans Serif", // Sans Serif, Serif, Monospace, Space Grotesk
    val motionStyle: String = "Zoom In", // Zoom In, Zoom Out, Pan Left, Pan Right, Static
    val defaultSceneDurationSeconds: Int = 5,
    val overlayTextType: String = "Uppercase Bold", // Uppercase Bold, Italic Highlight, Subtitle Center, Large Splash
    val bgMusicName: String = "Silent",
    val bgMusicVolume: Float = 0.15f,
    val isCustom: Boolean = true
)

