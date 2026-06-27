package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY dateCreated DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Int): Flow<Project?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectByIdSync(id: Int): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)
}

@Dao
interface SceneDao {
    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY sequenceNumber ASC")
    fun getScenesForProject(projectId: Int): Flow<List<Scene>>

    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY sequenceNumber ASC")
    suspend fun getScenesForProjectSync(projectId: Int): List<Scene>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenes(scenes: List<Scene>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: Scene): Long

    @Update
    suspend fun updateScene(scene: Scene)

    @Query("DELETE FROM scenes WHERE projectId = :projectId")
    suspend fun deleteScenesForProject(projectId: Int)

    @Delete
    suspend fun deleteScene(scene: Scene)
}

@Dao
interface UserImageDao {
    @Query("SELECT * FROM user_images ORDER BY dateAdded DESC")
    fun getAllImages(): Flow<List<UserImage>>

    @Query("SELECT * FROM user_images ORDER BY dateAdded DESC")
    suspend fun getAllImagesSync(): List<UserImage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: UserImage): Long

    @Update
    suspend fun updateImage(image: UserImage)

    @Delete
    suspend fun deleteImage(image: UserImage)

    @Query("SELECT * FROM user_images WHERE tags LIKE :searchQuery OR category LIKE :searchQuery OR name LIKE :searchQuery")
    fun searchImages(searchQuery: String): Flow<List<UserImage>>
}

@Dao
interface VideoTemplateDao {
    @Query("SELECT * FROM video_templates ORDER BY id DESC")
    fun getAllTemplates(): Flow<List<VideoTemplate>>

    @Query("SELECT * FROM video_templates ORDER BY id DESC")
    suspend fun getAllTemplatesSync(): List<VideoTemplate>

    @Query("SELECT * FROM video_templates WHERE id = :id")
    fun getTemplateById(id: Int): Flow<VideoTemplate?>

    @Query("SELECT * FROM video_templates WHERE name = :name LIMIT 1")
    suspend fun getTemplateByName(name: String): VideoTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: VideoTemplate): Long

    @Update
    suspend fun updateTemplate(template: VideoTemplate)

    @Delete
    suspend fun deleteTemplate(template: VideoTemplate)
}

