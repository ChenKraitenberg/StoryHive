
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

class StorageRepository {

    /**
     * Uploads an image by converting it to a Base64 string.
     * @param context Application context.
     * @param uri URI of the selected image.
     * @param path The intended storage path (not used in this implementation).
     * @return A Base64 encoded string of the image.
     */

    suspend fun uploadImage(context: Context, uri: Uri, path: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("StorageRepository", "Starting image encoding...")

                // Open an input stream to read the image data
                val inputStream = context.contentResolver.openInputStream(uri)

                if (inputStream == null) {
                    Log.e("StorageRepository", "Failed to open input stream for image")
                    throw IOException("Failed to read image data")
                }

                // Convert image to a Bitmap object for resizing
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }

                // Resize the bitmap to reduce file size while maintaining quality
                val resizedBitmap = getResizedBitmap(bitmap, 800) // Larger size for better quality

                // Convert the resized image to a Base64 string
                val imageBase64 = encodeImageToBase64(resizedBitmap)
                Log.d("StorageRepository", "Image successfully encoded, length: ${imageBase64.length}")

                // Return the Base64 string without storing it separately
                return@withContext imageBase64
            } catch (e: Exception) {
                Log.e("StorageRepository", "Failed to upload image: ${e.message}", e)
                throw e
            }
        }
    }


    /**
     * Encodes a Bitmap image into a Base64 string.
     * @param bitmap The bitmap image to encode.
     * @return A Base64 encoded string of the image.
     */
    fun encodeImageToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Resizes a bitmap while maintaining the aspect ratio.
     * @param bitmap The original bitmap image.
     * @param maxSize The maximum width or height.
     * @return A resized bitmap.
     */
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

    /**
     * Decodes a Base64 string into a Bitmap image with validation checks.
     * @param base64String The Base64 encoded image string.
     * @return A Bitmap object or null if decoding fails.
     */
    fun decodeBase64ToBitmap(base64String: String?): Bitmap? {
        if (base64String.isNullOrBlank()) {
            Log.e("StorageRepository", "Empty or null base64 string")
            return null
        }

        return try {
            // Clean the Base64 string by removing unwanted characters
            val cleanedBase64 = base64String.trim()
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "")

            // Ensure the string is long enough to be a valid Base64 encoding
            if (cleanedBase64.length < 10) {
                Log.e("StorageRepository", "Base64 string is too short")
                return null
            }

            // Remove prefix if it exists (e.g., "data:image/png;base64,")
            val base64Cleaned = cleanedBase64.replace("data:image/.*;base64,", "")

            // Decode the Base64 string into a byte array
            val decodedBytes = Base64.decode(base64Cleaned, Base64.DEFAULT)

            // Ensure the decoded byte array is not empty
            if (decodedBytes.isEmpty()) {
                Log.e("StorageRepository", "Decoded bytes are empty")
                return null
            }

            // Convert the byte array into a Bitmap image
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            Log.e("StorageRepository", "Invalid Base64 decoding: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e("StorageRepository", "Error creating bitmap: ${e.message}")
            null
        }
    }
}