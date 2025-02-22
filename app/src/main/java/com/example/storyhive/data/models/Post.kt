package com.example.storyhive.data.models

data class Post(
    val id: String = "",              // מזהה ייחודי של הפוסט
    val userId: String = "",          // מזהה המשתמש שיצר את הפוסט
    val userName: String = "",        // שם המשתמש שיצר את הפוסט
    val bookTitle: String = "",       // שם הספר
    val bookAuthor: String = "",      // מחבר הספר
    val content: String = "",         // תוכן הפוסט/ביקורת
    val imageUrl: String? = null,     // URL של תמונת הספר (אופציונלי)
    val likes: Int = 0,              // מספר לייקים
    val timestamp: Long = 0,          // זמן יצירת הפוסט
    val likedBy: List<String> = listOf() // רשימת משתמשים שעשו לייק
)
