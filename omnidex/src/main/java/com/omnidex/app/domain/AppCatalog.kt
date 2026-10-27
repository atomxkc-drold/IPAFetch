package com.omnidex.app.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** An app installed on this phone that consumes a stored credential. */
data class InstalledApp(
    val packageName: String,
    val label: String
)

/**
 * Lists launchable apps so an entry can record which apps use it. Icons are
 * resolved lazily by the UI to keep this list cheap.
 */
object AppCatalog {

    fun installedApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }.getOrDefault(emptyList())

        return resolved
            .map { info ->
                InstalledApp(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(pm).toString()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun labelFor(context: Context, packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName.substringAfterLast('.'))
}
