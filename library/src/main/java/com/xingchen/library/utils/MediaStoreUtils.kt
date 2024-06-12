/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xingchen.library.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A utility class for accessing this app's photo storage.
 *
 * Since this app doesn't request any external storage permissions, it will only be able to access
 * photos taken with this app. If the app is uninstalled, the photos taken with this app will stay
 * on the device, but reinstalling the app will not give it access to photos taken with the app's
 * previous instance. You can request further permissions to change this app's access. See this
 * guide for more: https://developer.android.com/training/data-storage.
 */
class MediaStoreUtils(private val context: Context) {

    private val mediaStoreCollection: Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            context.getExternalFilesDir(null)?.toUri()
        }

    private suspend fun getMediaImageCursor(mediaStoreCollection: Uri?): Cursor? {
        var cursor: Cursor?
        if (mediaStoreCollection == null) return null
        withContext(Dispatchers.IO) {
            val projection = arrayOf(IMAGE_DATA_INDEX, IMAGE_ID_INDEX)
            val sortOrder = "DATE_ADDED DESC"
            cursor = context.contentResolver.query(
                mediaStoreCollection, projection, null, null, sortOrder
            )
        }
        return cursor
    }

    suspend fun getLatestImageFilename(): String? {
        var filename: String? = null
        if (mediaStoreCollection == null) return null
        getMediaImageCursor(mediaStoreCollection).use { cursor ->
            if (cursor?.moveToFirst() != true) return null
            val imageDataColumn = cursor.getColumnIndexOrThrow(IMAGE_DATA_INDEX)
            filename = cursor.getString(imageDataColumn)
        }
        return filename
    }

    suspend fun getMediaStoreFiles(): MutableList<MediaStoreFile> {
        val mediaStoreFiles = mutableListOf<MediaStoreFile>()
        getMediaImageCursor(mediaStoreCollection)?.use { cursor ->
            val imageDataColumn = cursor.getColumnIndexOrThrow(IMAGE_DATA_INDEX)
            val imageIdColumn = cursor.getColumnIndexOrThrow(IMAGE_ID_INDEX)
            while (cursor.moveToNext()) {
                val imageId = cursor.getLong(imageIdColumn)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId
                )
                val contentFile = File(cursor.getString(imageDataColumn))
                mediaStoreFiles.add(MediaStoreFile(contentUri, contentFile, imageId))
            }
        }
        return mediaStoreFiles
    }

    companion object {
        private const val IMAGE_DATA_INDEX = MediaStore.Images.Media.DATA
        private const val IMAGE_ID_INDEX = MediaStore.Images.Media._ID
    }
}

data class MediaStoreFile(val uri: Uri, val file: File, val id: Long)