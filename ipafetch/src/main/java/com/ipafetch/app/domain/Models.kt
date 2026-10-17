package com.ipafetch.app.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeyEntry(
    @SerialName("id")
    val id: Long = 0L,

    @SerialName("name")
    val name: String = "",

    @SerialName("service")
    val service: String = "",

    @SerialName("category")
    val category: String = Category.OTHER,

    @SerialName("username")
    val username: String = "",

    @SerialName("endpoint")
    val endpoint: String = "",

    @SerialName("notes")
    val notes: String = "",

    @SerialName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @SerialName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),

    @SerialName("expiresAt")
    val expiresAt: Long? = null,

    @SerialName("keyLength")
    val keyLength: Int = 0,

    @SerialName("prefix")
    val prefix: String = "",

    @SerialName("rotatedAt")
    val rotatedAt: Long? = null,

    @SerialName("encryptedValue")
    val encryptedValue: String = ""
)

@Serializable
data class Category(
    companion object {
        const val API = "API"
        const val DATABASE = "Database"
        const val CLOUD = "Cloud"
        const val PAYMENT = "Payment"
        const val COMMUNICATION = "Communication"
        const val OTHER = "Other"

        val ALL = listOf(API, DATABASE, CLOUD, PAYMENT, COMMUNICATION, OTHER)
    }
)

@Entity(
    tableName = "key_entries",
    indices = [Index(value = ["service"], unique = true)]
)
data class KeyEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "service")
    val service: String,

    @ColumnInfo(name = "category")
    val category: String = Category.OTHER,

    @ColumnInfo(name = "username")
    val username: String = "",

    @ColumnInfo(name = "endpoint")
    val endpoint: String = "",

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "expires_at")
    val expiresAt: Long? = null,

    @ColumnInfo(name = "key_length")
    val keyLength: Int = 0,

    @ColumnInfo(name = "prefix")
    val prefix: String = "",

    @ColumnInfo(name = "rotated_at")
    val rotatedAt: Long? = null,

    @ColumnInfo(name = "encrypted_value")
    val encryptedValue: String = ""
)

fun KeyEntryEntity.toDomain() = KeyEntry(
    id = id, name = name, service = service, category = category,
    username = username, endpoint = endpoint, notes = notes,
    createdAt = createdAt, updatedAt = updatedAt, expiresAt = expiresAt,
    keyLength = keyLength, prefix = prefix, rotatedAt = rotatedAt,
    encryptedValue = encryptedValue
)

fun KeyEntry.toEntity() = KeyEntryEntity(
    id = id, name = name, service = service, category = category,
    username = username, endpoint = endpoint, notes = notes,
    createdAt = createdAt, updatedAt = updatedAt, expiresAt = expiresAt,
    keyLength = keyLength, prefix = prefix, rotatedAt = rotatedAt,
    encryptedValue = encryptedValue
)
