package com.openscan.scanner.data.model

import java.util.*

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val imagePaths: MutableList<String> = mutableListOf(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val pageCount: Int
        get() = imagePaths.size
    
    fun addPage(imagePath: String) {
        imagePaths.add(imagePath)
    }
    
    fun removePage(index: Int) {
        if (index in 0 until imagePaths.size) {
            imagePaths.removeAt(index)
        }
    }
    
    fun reorderPages(fromIndex: Int, toIndex: Int) {
        if (fromIndex in 0 until imagePaths.size && toIndex in 0 until imagePaths.size) {
            val item = imagePaths.removeAt(fromIndex)
            imagePaths.add(toIndex, item)
        }
    }
} 