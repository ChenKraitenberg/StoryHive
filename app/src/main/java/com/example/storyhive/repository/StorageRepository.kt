
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

class StorageRepository {

    suspend fun uploadImage(context: Context, uri: Uri, collectionName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("StorageRepository", "Encoding image to Base64...")

                // המר את ה-URI ל-Bitmap
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri) // תוקן כאן
                    ImageDecoder.decodeBitmap(source)
                }

                // הקטן את התמונה לפני המרה
                val resizedBitmap = getResizedBitmap(bitmap, 500)

                // המרת התמונה ל-Base64
                val imageBase64 = encodeImageToBase64(resizedBitmap)

                Log.d("StorageRepository", "Image successfully encoded!")

                // שמירת התמונה ב-Firestore
                val db = FirebaseFirestore.getInstance()
                val documentRef = db.collection(collectionName).document()
                val data = hashMapOf("imageBase64" to imageBase64)

                documentRef.set(data).await()

                Log.d("StorageRepository", "Image saved to Firestore successfully!")

                return@withContext imageBase64  // החזרת ה-Base64
            } catch (e: Exception) {
                Log.e("StorageRepository", "Failed to upload image", e)
                return@withContext null
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
    fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("StorageRepository", "Failed to decode Base64", e)
            null
        }
    }

}