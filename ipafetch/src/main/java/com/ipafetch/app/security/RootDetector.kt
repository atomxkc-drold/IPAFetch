package com.ipafetch.app.security

import android.content.Context
import android.os.Build
import java.io.File

/**
 * Multi-signal root/bootloader detection. No single check is reliable, so we
 * combine several: known root binaries, build tags, test-flags, and
 * Magisk/EdXposed marker files.
 */
object RootDetector {

    private val rootBinaries = listOf(
        "/system/bin/su", "/system/xbin/su", "/system/bin/.ext/su",
        "/system/etc/su", "/system/etc/init.d/su", "/system/etc/su.conf",
        "/vendor/bin/su", "/vendor/etc/su", "/vendor/etc/su.conf",
        "/data/local/su", "/data/local/bin/su", "/data/local/tmp/su",
        "/sbin/su", "/magisk/.core/bin/su", "/data/property/su"
    )

    private val suspiciousApps = listOf(
        "com.nougatglobal.nougat", "com.tsanpo.android.mirror",
        "com.android.vending.b70", "com.google.android.apps.genie",
        "com.koushikdutta.superuser", "com.koushikdutta.rommanager",
        "com.dimon.paysandu", "com.jrummyapps.busybox",
        "com.topjohnwu.magisk", "com.tsanpo.android.mirror"
    )

    fun isRooted(context: Context): Boolean {
        if (checkRootBinaries()) return true
        if (checkBuildTags()) return true
        if (checkTestFlags()) return true
        if (checkMagisk()) return true
        if (checkSuspiciousApps(context)) return true
        return false
    }

    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("emulator")
                || Build.MODEL.contains("Android SDK")
                || Build.BRAND.contains("generic")
                || Build.DEVICE.contains("generic")
                || Build.PRODUCT.contains("sdk")
                || Build.TAGS == null
                || Build.TAGS.contains("test-keys"))
    }

    private fun checkRootBinaries(): Boolean {
        for (path in rootBinaries) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun checkBuildTags(): Boolean {
        val tags = Build.TAGS ?: return false
        // test-keys indicate an unofficial/eng build; release-keys is the norm
        return tags.contains("test-keys")
    }

    private fun checkTestFlags(): Boolean {
        // production builds are "user"; eng/userdebug are suspicious
        return Build.TYPE != "user"
    }

    private fun checkMagisk(): Boolean {
        val magiskPaths = listOf(
            "/data/local/tmp/magisk", "/data/local/bin/magisk",
            "/sbin/.magisk", "/dev/.magisk", "/data/property/magisk"
        )
        for (p in magiskPaths) if (File(p).exists()) return true
        val magiskProps = File("/data/local.prop")
        if (magiskProps.exists() && magiskProps.readText().contains("magisk")) {
            return true
        }
        return false
    }

    private fun checkSuspiciousApps(context: Context): Boolean {
        val pm = context.packageManager
        return try {
            val packages = pm.getInstalledPackages(0)
            packages.any { it.packageName in suspiciousApps }
        } catch (e: Exception) {
            false
        }
    }
}
