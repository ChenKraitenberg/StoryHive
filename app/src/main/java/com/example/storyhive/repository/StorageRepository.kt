
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

//    suspend fun uploadImage(context: Context, uri: Uri, collectionName: String): String? {
//        return withContext(Dispatchers.IO) {
//            try {
//                Log.d("StorageRepository", "Encoding image to Base64...")
//
//                // המר את ה-URI ל-Bitmap
//                val bitmap = if (Build.VERSION.SDK_INT < 28) {
//                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
//                } else {
//                    val source = ImageDecoder.createSource(context.contentResolver, uri) // תוקן כאן
//                    ImageDecoder.decodeBitmap(source)
//                }
//
//                // הקטן את התמונה לפני המרה
//                val resizedBitmap = getResizedBitmap(bitmap, 500)
//
//                // המרת התמונה ל-Base64
//                val imageBase64 = encodeImageToBase64(resizedBitmap)
//
//                Log.d("StorageRepository", "Image successfully encoded!")
//
//                // שמירת התמונה ב-Firestore
//                val db = FirebaseFirestore.getInstance()
//                val documentRef = db.collection(collectionName).document()
//                val data = hashMapOf("imageBase64" to imageBase64)
//
//                documentRef.set(data).await()
//
//                Log.d("StorageRepository", "Image saved to Firestore successfully!")
//
//                return@withContext imageBase64  // החזרת ה-Base64
//            } catch (e: Exception) {
//                Log.e("StorageRepository", "Failed to upload image", e)
//                return@withContext null
//            }
//        }
//    }

    suspend fun uploadImage(context: Context, uri: Uri, path: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("StorageRepository", "Encoding image to Base64...")

                // המר נתיב לנתיב תקין - במקום "profile_images/userId", השתמש ב-"images" ואז המסמך יהיה שם התמונה
                // נתיב בפיירסטור לקולקציה חייב להיות עם מספר אי-זוגי של סגמנטים
                val collection = "images"
                val documentId = if (path.contains("/")) {
                    path.replace("/", "_")
                } else {
                    path
                }

                // קרא את התמונה והמר אותה ל-Base64
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)
                    Log.d("StorageRepository", "Image successfully encoded!")

                    // שמור את התמונה בפיירסטור
                    FirebaseFirestore.getInstance()
                        .collection(collection)
                        .document(documentId)
                        .set(mapOf("imageData" to base64Image))
                        .await()

                    return@withContext base64Image
                } else {
                    throw IOException("Failed to read image data")
                }
            } catch (e: Exception) {
                Log.e("StorageRepository", "Failed to upload image", e)
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
//    fun decodeBase64ToBitmap(base64String: String): Bitmap? {
//        return try {
//            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
//            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
//        } catch (e: Exception) {
//            Log.e("StorageRepository", "Failed to decode Base64", e)
//            null
//        }
//    }
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