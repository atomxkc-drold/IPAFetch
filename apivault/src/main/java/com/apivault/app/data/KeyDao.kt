package com.apivault.app.data

import androidx.room.*
import com.apivault.app.domain.KeyEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyDao {

    @Query("SELECT * FROM key_entries ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<KeyEntryEntity>>

    @Query("SELECT * FROM key_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): KeyEntryEntity?

    @Query("SELECT * FROM key_entries WHERE service LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun search(query: String): Flow<List<KeyEntryEntity>>

    @Query("SELECT * FROM key_entries WHERE category = :category ORDER BY updatedAt DESC")
    fun byCategory(category: String): Flow<List<KeyEntryEntity>>

    @Query("SELECT * FROM key_entries WHERE expires_at IS NOT NULL AND expires_at < :now ORDER BY expires_at ASC")
    fun expiringSoon(now: Long): Flow<List<KeyEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: KeyEntryEntity): Long

    @Update
    suspend fun update(entity: KeyEntryEntity)

    @Query("DELETE FROM key_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM key_entries")
    suspend fun deleteAll()
}
