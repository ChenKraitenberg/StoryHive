package com.example.storyhive.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.storyhive.data.models.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String,
    val bio: String? = null,
    val profileImageBase64: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

// Converter functions
fun UserEntity.toDomain(): User {
    return User(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl
    )
}

fun User.toEntity(profileImageBase64: String? = null, bio: String? = null): UserEntity {
    return UserEntity(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl,
        profileImageBase64 = profileImageBase64,
        bio = bio
    )
}