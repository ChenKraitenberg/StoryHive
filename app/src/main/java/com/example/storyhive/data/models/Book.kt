package com.example.storyhive.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val description: String = "",
    val coverUrl: String = "",
    val genre: String,
    val rating: Float,
    val pageCount: Int,
    val publishedDate: String
) : Parcelable
