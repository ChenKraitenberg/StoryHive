package com.example.storyhive.data.models

//data class Post(
//    val postId: String = "",
//    val userId: String = "",         // uid של המשתמש שיצר את הפוסט
//    val userProfileImage: String = "", // תמונת פרופיל של המשתמש
//
//    val userDisplayName: String = "",// נוח לשמור כדי לא לחפש כל פעם
//    val userProfileImageUrl: String = "",
//    val bookAuthor: String = "",      // מחבר הספר
//    val bookTitle: String = "",
//    val bookDescription: String = "", // טקסט חופשי שהמשתמש רושם
//    val imageUrl: String? = null,     // URL של תמונת הספר
//    val rating: Float = 0f,           // דירוג הספר
//    val likes: Int = 0,               // מספר הלייקים
//    val likedBy: List<String> = emptyList(), // רשימת המשתמשים שלייקו את הפוסט
//    val timestamp: Long = System.currentTimeMillis(),
//    val review: String = ""           // תגובה על הספר )
//        )
// וודא שמודל ה-Post שלך נראה כך:
data class Post(
    val postId: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val userProfileImage: String = "",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val bookDescription: String = "",
    var imageBase64: String? = null, // יכול להיות null
    var rating: Float = 0.0f,
    val review: String = "",
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
){

}