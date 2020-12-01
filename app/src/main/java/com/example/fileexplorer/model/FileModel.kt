package com.example.fileexplorer.model

import com.example.fileexplorer.constants.FileType

data class FileModel(
    val path: String,
    val fileType: FileType,
    val name: String,
    val sizeInMB: Double,
    val extension: String = "",
    val subFiles: Int = 0
)