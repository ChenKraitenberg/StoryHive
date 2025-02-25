package com.example.storyhive.data.models

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val description: String = "",    // הוספנו תקציר
    val coverUrl: String = "",       // הוספנו URL לתמונת כריכה
    val genre: String,
    val rating: Float,
)
