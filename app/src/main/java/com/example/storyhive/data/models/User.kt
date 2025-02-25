package com.example.storyhive.data.models

data class User(
    val uid: String = "",
    val displayName: String = "",
    //val profileImageUrl: String = "" // לינק לתמונת פרופיל ב-Firebase Storage
    val email : String = "",
    val photoUrl : String = ""
)
