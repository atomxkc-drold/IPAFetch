package com.omnidex.app.security

import com.omnidex.app.domain.EntryType
import com.omnidex.app.domain.LintFlag
import com.omnidex.app.domain.Severity
import com.omnidex.app.domain.VaultEntry
import java.util.concurrent.TimeUnit
import kotlin.math.ln

object CredentialLinter {
    private const val ENTROPY_WARNING = 3.5
    private const val ROTATION_WARNING_DAYS = 90L
    private const val ROTATION_CRITICAL_DAYS = 180L

    fun lint(entry: VaultEntry, decryptedSecret: String): List<LintFlag> {
        val flags = mutableListOf<LintFlag>()
        val type = EntryType.from(entry.type)
        
        if (type.isSecret || type == EntryType.TOKEN || type == EntryType.API || type == EntryType.SECRET) {
            // Entropy check
            val entropy = shannonEntropy(decryptedSecret)
            if (entropy < ENTROPY_WARNING && decryptedSecret.length >= 16) {
                flags.add(
                    LintFlag(
                        ruleId = "entropy_low",
                        severity = Severity.WARNING,
                        message = "Low entropy detected. Consider generating a stronger secret.",
                        remediation = "Rotate this credential with a cryptographically random generator."
                    )
                )
            }
            
            // Rotation age check
            val now = System.currentTimeMillis()
            val ageMs = now - entry.updatedAt
            val ageDays = TimeUnit.MILLISECONDS.toDays(ageMs)
            if (ageDays > ROTATION_CRITICAL_DAYS) {
                flags.add(
                    LintFlag(
                        ruleId = "rotation_old_critical",
                        severity = Severity.CRITICAL,
                        message = "Credential not rotated in $ageDays days.",
                        remediation = "Rotate this credential immediately."
                    )
                )
            } else if (ageDays > ROTATION_WARNING_DAYS) {
                flags.add(
                    LintFlag(
                        ruleId = "rotation_old_warning",
                        severity = Severity.WARNING,
                        message = "Credential not rotated in $ageDays days.",
                        remediation = "Consider rotating this credential soon."
                    )
                )
            }
            
            // Format checks
            if (decryptedSecret.startsWith("AKIA") && decryptedSecret.length == 20) {
                // AWS access key format looks ok
            } else if (decryptedSecret.contains(".") && decryptedSecret.split(".").size == 3) {
                // Possible JWT
                flags.add(
                    LintFlag(
                        ruleId = "jwt_format",
                        severity = Severity.INFO,
                        message = "JWT detected. Ensure proper expiration and rotation.",
                        remediation = "Use short-lived tokens where possible."
                    )
                )
            }
            
            // Common weak patterns
            if (decryptedSecret == decryptedSecret.lowercase() && decryptedSecret.all { it.isLetterOrDigit() } && decryptedSecret.length < 32) {
                flags.add(
                    LintFlag(
                        ruleId = "weak_pattern",
                        severity = Severity.WARNING,
                        message = "Potentially weak credential pattern detected.",
                        remediation = "Use a stronger, randomly generated credential."
                    )
                )
            }
        }
        
        return flags
    }
    
    private fun shannonEntropy(s: String): Double {
        if (s.isEmpty()) return 0.0
        val freq = mutableMapOf<Char, Int>()
        for (c in s) {
            freq[c] = (freq[c] ?: 0) + 1
        }
        var entropy = 0.0
        val len = s.length.toDouble()
        for (count in freq.values) {
            val p = count.toDouble() / len
            entropy -= p * ln(p) / ln(2.0)
        }
        return entropy
    }
}
