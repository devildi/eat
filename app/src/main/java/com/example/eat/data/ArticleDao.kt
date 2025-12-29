package com.example.eat.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY timestamp DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles ORDER BY timestamp DESC")
    suspend fun getAllArticlesSync(): List<ArticleEntity>

    @Insert
    suspend fun insertArticle(article: ArticleEntity)

    @Delete
    suspend fun deleteArticle(article: ArticleEntity)
    
    @Query("DELETE FROM articles")
    suspend fun clearAll()

    @Query("SELECT * FROM articles WHERE title = :title LIMIT 1")
    suspend fun getArticleByTitle(title: String): ArticleEntity?

    @Query("SELECT COUNT(*) FROM articles WHERE timestamp >= :startTime AND timestamp <= :endTime")
    fun getArticleCountByDateRange(startTime: Long, endTime: Long): Flow<Int>
}
