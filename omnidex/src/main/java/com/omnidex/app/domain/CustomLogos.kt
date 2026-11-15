package com.omnidex.app.domain

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * User-supplied provider logos: copied into app-private storage and referenced
 * by a "custom:<file>" brand id, so backups and entries stay self-contained.
 */
object CustomLogos {

    private const val PREFIX = "custom:"
    private const val DIR = "custom_logos"

    fun isCustom(brandId: String): Boolean = brandId.startsWith(PREFIX)

    fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { mkdirs() }

    /** Copies the picked image into private storage; returns the new brand id. */
    fun save(context: Context, uri: Uri): String? = runCatching {
        val name = UUID.randomUUID().toString() + ".img"
        val target = File(dir(context), name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        PREFIX + name
    }.getOrNull()

    fun fileFor(context: Context, brandId: String): File? {
        if (!isCustom(brandId)) return null
        val file = File(dir(context), brandId.removePrefix(PREFIX))
        return if (file.exists()) file else null
    }

    fun delete(context: Context, brandId: String) {
        runCatching { fileFor(context, brandId)?.delete() }
    }
}
