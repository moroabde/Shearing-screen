package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Query("SELECT * FROM analyzed_posts ORDER BY createdAt DESC")
    fun getAllPosts(): Flow<List<AnalyzedPostEntity>>

    @Query("SELECT * FROM analyzed_posts WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoritePosts(): Flow<List<AnalyzedPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: AnalyzedPostEntity): Long

    @Update
    suspend fun updatePost(post: AnalyzedPostEntity)

    @Delete
    suspend fun deletePost(post: AnalyzedPostEntity)

    @Query("DELETE FROM analyzed_posts")
    suspend fun clearAll()

    @Query("UPDATE analyzed_posts SET copyCount = copyCount + 1 WHERE id = :id")
    suspend fun incrementCopyCount(id: Long)

    @Query("UPDATE analyzed_posts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
}
