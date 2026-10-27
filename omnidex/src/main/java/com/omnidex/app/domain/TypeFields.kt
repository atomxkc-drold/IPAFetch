package com.omnidex.app.domain

/**
 * Extras that belong to the entry *type* rather than the provider, e.g. an SSH
 * host or the package an adb command targets. Stored encrypted alongside the
 * provider fields.
 */
object TypeFields {

    fun forType(type: EntryType): List<ExtraField> = when (type) {
        EntryType.SSH -> listOf(
            ExtraField("host", "Host", "192.168.1.10 or host.example.com"),
            ExtraField("port", "Port", "22"),
            ExtraField("user", "User", "root"),
            ExtraField("keyType", "Key type", "ed25519 / rsa / password")
        )
        EntryType.ADB -> listOf(
            ExtraField("package", "Target package", "com.example.app"),
            ExtraField("serial", "Device serial (optional)", "emulator-5554"),
            ExtraField("transport", "Transport", "USB / wireless")
        )
        EntryType.MODULE -> listOf(
            ExtraField("repo", "Repository", "https://github.com/owner/repo"),
            ExtraField("version", "Version", "v1.2.3"),
            ExtraField("source", "Source", "Magisk / KernelSU / other")
        )
        EntryType.NUMBER -> listOf(ExtraField("unit", "Unit", "MB, px, ms…"))
        EntryType.COLOR -> listOf(ExtraField("name2", "Second swatch (optional)", "#RRGGBB"))
        else -> emptyList()
    }

    private val hex = Regex("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

    /** Returns a user-facing problem, or null when the entry is valid. */
    fun validate(type: EntryType, value: String, extras: Map<String, String>): String? {
        val trimmed = value.trim()
        return when (type) {
            EntryType.COLOR -> if (!hex.matches(trimmed)) "Use a hex colour like #1B47A6" else null
            EntryType.NUMBER -> if (trimmed.isEmpty() || trimmed.toDoubleOrNull() == null) {
                "Enter a number"
            } else null
            EntryType.SSH -> when {
                extras["host"].isNullOrBlank() -> "Add a host"
                extras["user"].isNullOrBlank() -> "Add a user"
                !extras["port"].isNullOrBlank() && extras["port"]!!.toIntOrNull() == null -> "Port must be a number"
                else -> null
            }
            EntryType.ADB -> if (trimmed.isBlank()) "Add the command" else null
            EntryType.MODULE -> if (!trimmed.startsWith("http")) "Paste the release or repo URL" else null
            EntryType.LINK -> if (!trimmed.startsWith("http")) "Paste a full https link" else null
            else -> null
        }
    }

    /** Command that the SSH entry represents, for copy/show. */
    fun sshCommand(host: String, user: String, port: String): String {
        val userPart = if (user.isBlank()) "" else "$user@"
        val portPart = if (port.isBlank() || port == "22") "" else " -p $port"
        return "ssh $userPart$host$portPart"
    }

    /** Parses #RGB / #RRGGBB / #AARRGGBB into a Compose colour, or null. */
    fun colorOf(hexValue: String): androidx.compose.ui.graphics.Color? {
        val v = hexValue.trim().removePrefix("#")
        return runCatching {
            when (v.length) {
                3 -> androidx.compose.ui.graphics.Color(
                    (v[0].digitToInt(16) * 17),
                    (v[1].digitToInt(16) * 17),
                    (v[2].digitToInt(16) * 17)
                )
                6 -> androidx.compose.ui.graphics.Color(
                    v.substring(0, 2).toInt(16),
                    v.substring(2, 4).toInt(16),
                    v.substring(4, 6).toInt(16)
                )
                8 -> androidx.compose.ui.graphics.Color(
                    v.substring(2, 4).toInt(16),
                    v.substring(4, 6).toInt(16),
                    v.substring(6, 8).toInt(16),
                    v.substring(0, 2).toInt(16)
                )
                else -> null
            }
        }.getOrNull()
    }
}
