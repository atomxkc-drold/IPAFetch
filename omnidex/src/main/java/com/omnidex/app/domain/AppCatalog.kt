package com.omnidex.app.domain

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/** An app installed on this phone that consumes a stored credential. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    /** True for apps shipped in the system image (Settings, Camera, Chrome…). */
    val isSystem: Boolean = false
)

/**
 * Lists launchable apps so an entry can record which apps use it. Icons are
 * resolved lazily by the UI to keep this list cheap.
 *
 * User-installed apps come first and are what the picker shows by default;
 * system apps are available behind a toggle.
 */
object AppCatalog {

    fun installedApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }.getOrDefault(emptyList())

        return resolved
            .mapNotNull { info ->
                val appInfo = info.activityInfo?.applicationInfo ?: return@mapNotNull null
                val system = appInfo.flags and (
                    ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
                    ) != 0
                InstalledApp(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(pm).toString(),
                    isSystem = system
                )
            }
            // System order first, then alphabetical: user apps stay on top.
            .distinctBy { it.packageName }
            .sortedWith(compareBy({ it.isSystem }, { it.label.lowercase() }))
    }

    /** Only the apps the user installed themselves. */
    fun userApps(context: Context): List<InstalledApp> =
        installedApps(context).filterNot { it.isSystem }

    fun labelFor(context: Context, packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName.substringAfterLast('.'))
}
