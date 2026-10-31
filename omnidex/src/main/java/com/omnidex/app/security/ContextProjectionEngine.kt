package com.omnidex.app.security

import android.app.ActivityManager
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import com.omnidex.app.domain.VaultEntry

object ContextProjectionEngine {
    fun getForegroundPackage(context: Context): String? {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val tasks = am.appTasks
                if (tasks.isNotEmpty()) {
                    val taskInfo = tasks[0].taskInfo
                    return taskInfo.topActivity?.packageName ?: taskInfo.baseActivity?.packageName
                }
            }
            val processes = am.runningAppProcesses
            val foreground = processes?.firstOrNull { 
                it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND 
            }
            foreground?.pkgList?.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun getClipboardMimeTypes(context: Context): List<String> {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val desc = cm.primaryClipDescription ?: return emptyList()
            desc.mimeTypes?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun scoreEntries(
        entries: List<VaultEntry>,
        foregroundPackage: String?,
        clipboardMimes: List<String>
    ): List<Pair<VaultEntry, Float>> {
        return entries.map { entry ->
            var score = 0f
            // Package match
            if (!foregroundPackage.isNullOrBlank()) {
                if (entry.usedByApps.any { it.equals(foregroundPackage, ignoreCase = true) }) {
                    score += 10f
                }
                if (entry.usedBy.equals(foregroundPackage, ignoreCase = true)) {
                    score += 5f
                }
            }
            // Favorites get boost
            if (entry.favorite) score += 2f
            // Recency boost (more recent = higher score)
            val ageHours = (System.currentTimeMillis() - entry.updatedAt) / (1000f * 60f * 60f)
            if (ageHours < 24f) score += 1f
            if (ageHours < 1f) score += 1f
            Pair(entry, score)
        }.filter { it.second > 0f }
            .sortedByDescending { it.second }
    }
}
