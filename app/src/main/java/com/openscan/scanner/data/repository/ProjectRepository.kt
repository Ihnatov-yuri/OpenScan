package com.openscan.scanner.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.openscan.scanner.data.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProjectRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("projects", Context.MODE_PRIVATE)
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()
    
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()
    
    init {
        loadProjects()
    }
    
    fun createProject(name: String): Project {
        val project = Project(name = name)
        val updatedProjects = _projects.value.toMutableList()
        updatedProjects.add(0, project) // Add to beginning
        _projects.value = updatedProjects
        saveProjects()
        return project
    }
    
    fun setCurrentProject(project: Project?) {
        _currentProject.value = project
    }
    
    fun addPageToProject(projectId: String, imagePath: String): Boolean {
        val project = _projects.value.find { it.id == projectId } ?: return false
        project.addPage(imagePath)
        updateProject(project)
        return true
    }
    
    fun addPageToCurrentProject(imagePath: String): Boolean {
        val project = _currentProject.value ?: return false
        project.addPage(imagePath)
        updateProject(project)
        // Update current project as well
        _currentProject.value = project.copy(updatedAt = Date())
        return true
    }
    
    fun removePageFromProject(projectId: String, pageIndex: Int): Boolean {
        val project = _projects.value.find { it.id == projectId } ?: return false
        project.removePage(pageIndex)
        updateProject(project)
        return true
    }
    
    fun updateProject(updatedProject: Project) {
        val projects = _projects.value.toMutableList()
        val index = projects.indexOfFirst { it.id == updatedProject.id }
        if (index != -1) {
            projects[index] = updatedProject.copy(updatedAt = Date())
            _projects.value = projects
            saveProjects()
        }
    }
    
    fun deleteProject(projectId: String): Boolean {
        val project = _projects.value.find { it.id == projectId } ?: return false
        
        // Delete all image files
        project.imagePaths.forEach { imagePath ->
            try {
                File(imagePath).delete()
            } catch (e: Exception) {
                // Continue deleting other files
            }
        }
        
        // Remove from list
        val updatedProjects = _projects.value.filter { it.id != projectId }
        _projects.value = updatedProjects
        saveProjects()
        
        // Clear current project if it was deleted
        if (_currentProject.value?.id == projectId) {
            _currentProject.value = null
        }
        
        return true
    }
    
    fun getProject(projectId: String): Project? {
        return _projects.value.find { it.id == projectId }
    }
    
    private fun loadProjects() {
        try {
            val projectsJson = prefs.getString("projects_list", "[]") ?: "[]"
            val jsonArray = JSONArray(projectsJson)
            val projects = mutableListOf<Project>()
            
            for (i in 0 until jsonArray.length()) {
                val projectJson = jsonArray.getJSONObject(i)
                projects.add(projectFromJson(projectJson))
            }
            
            _projects.value = projects.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            e.printStackTrace()
            _projects.value = emptyList()
        }
    }
    
    private fun saveProjects() {
        try {
            val jsonArray = JSONArray()
            _projects.value.forEach { project ->
                jsonArray.put(projectToJson(project))
            }
            prefs.edit()
                .putString("projects_list", jsonArray.toString())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun projectToJson(project: Project): JSONObject {
        return JSONObject().apply {
            put("id", project.id)
            put("name", project.name)
            put("imagePaths", JSONArray(project.imagePaths))
            put("createdAt", project.createdAt.time)
            put("updatedAt", project.updatedAt.time)
        }
    }
    
    private fun projectFromJson(json: JSONObject): Project {
        val imagePathsJson = json.getJSONArray("imagePaths")
        val imagePaths = mutableListOf<String>()
        for (i in 0 until imagePathsJson.length()) {
            imagePaths.add(imagePathsJson.getString(i))
        }
        
        return Project(
            id = json.getString("id"),
            name = json.getString("name"),
            imagePaths = imagePaths,
            createdAt = Date(json.getLong("createdAt")),
            updatedAt = Date(json.getLong("updatedAt"))
        )
    }
    
    fun generateProjectName(): String {
        val timestamp = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            .format(Date())
        return "Scan $timestamp"
    }
} 