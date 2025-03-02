
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

class StorageRepository {

    suspend fun uploadImage(context: Context, uri: Uri, path: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("StorageRepository", "Starting image encoding...")

                // Read the image data
                val inputStream = context.contentResolver.openInputStream(uri)

                if (inputStream == null) {
                    Log.e("StorageRepository", "Failed to open input stream for image")
                    throw IOException("Failed to read image data")
                }

                // Convert to bitmap for resizing
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }

                // Resize the bitmap to reduce size
                val resizedBitmap = getResizedBitmap(bitmap, 800) // Larger size for better quality

                // Convert to Base64
                val imageBase64 = encodeImageToBase64(resizedBitmap)
                Log.d("StorageRepository", "Image successfully encoded, length: ${imageBase64.length}")

                // Return the Base64 string directly without storing it separately
                return@withContext imageBase64
            } catch (e: Exception) {
                Log.e("StorageRepository", "Failed to upload image: ${e.message}", e)
                throw e
            }
        }
    }


    fun encodeImageToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
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

// בדיקת תקינות לפני פענוח Base64
fun decodeBase64ToBitmap(base64String: String): Bitmap? {
    return try {
        // וידוא שהמחרוזת תקינה לפני פענוח
        if (base64String.isEmpty()) {
            Log.e("StorageRepository", "Empty base64 string")
            return null
        }

        // ניסיון לפענח רק אם המחרוזת נראית חוקית
        val cleanedBase64 = base64String.trim()
        val decodedBytes = Base64.decode(cleanedBase64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: IllegalArgumentException) {
        Log.e("StorageRepository", "Failed to decode Base64", e)
        null
    } catch (e: Exception) {
        Log.e("StorageRepository", "Error creating bitmap from Base64", e)
        null
    }
}
}