// File: StorageRepository.kt
package com.example.storyhive.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class StorageRepository {
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadImage(uri: Uri, folder: String): String {
        return withContext(Dispatchers.IO) {
            val storageRef = storage.reference
            val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            val imageRef = storageRef.child("$folder/$fileName")

            try {
                // העלאת התמונה
                val uploadTask = imageRef.putFile(uri).await()
                // קבלת ה-URL להורדה
                val downloadUrl = uploadTask.storage.downloadUrl.await()
                downloadUrl.toString()
            } catch (e: Exception) {
                Log.e("StorageRepository", "Error uploading image: ${e.message}", e)
                throw e
            }
        }
    }

    // storage
// להוסיף לStorageRepository
    @OptIn(ExperimentalEncodingApi::class)
    fun encodeImageToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, CoroutineStart.DEFAULT)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decodeBase64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, CoroutineStart.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    fun getResizedBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    companion object {
        @OptIn(ExperimentalEncodingApi::class)
        fun decodeBase64ToBitmap(profileImageBase64: String): Bitmap? {
            val decodedBytes = Base64.decode(profileImageBase64, CoroutineStart.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)


        }
    }

}
