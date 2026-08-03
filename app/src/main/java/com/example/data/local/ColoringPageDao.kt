package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.models.ColoringPage
import kotlinx.coroutines.flow.Flow

@Dao
interface ColoringPageDao {
    @Query("SELECT * FROM coloring_pages ORDER BY timestamp DESC")
    fun getAllPages(): Flow<List<ColoringPage>>

    @Query("SELECT * FROM coloring_pages WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoritePages(): Flow<List<ColoringPage>>

    @Query("SELECT * FROM coloring_pages WHERE id = :id LIMIT 1")
    suspend fun getPageById(id: String): ColoringPage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: ColoringPage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<ColoringPage>)

    @Query("UPDATE coloring_pages SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Delete
    suspend fun deletePage(page: ColoringPage)

    @Query("DELETE FROM coloring_pages WHERE id = :id")
    suspend fun deletePageById(id: String)
}
