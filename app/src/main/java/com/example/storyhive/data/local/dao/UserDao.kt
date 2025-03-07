package com.example.storyhive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.storyhive.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE uid = :userId")
    suspend fun getUserByIdSync(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET displayName = :displayName WHERE uid = :userId")
    suspend fun updateDisplayName(userId: String, displayName: String)

    @Query("UPDATE users SET profileImageBase64 = :profileImageBase64 WHERE uid = :userId")
    suspend fun updateProfileImage(userId: String, profileImageBase64: String?)

    @Query("UPDATE users SET bio = :bio WHERE uid = :userId")
    suspend fun updateBio(userId: String, bio: String?)

    @Query("DELETE FROM users WHERE uid = :userId")
    suspend fun deleteUser(userId: String)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("UPDATE users SET lastUpdated = :timestamp WHERE uid = :userId")
    suspend fun updateUserTimestamp(userId: String, timestamp: Long)
}