package com.omnidex.app.security

import com.omnidex.app.data.VaultRepository

object ComposerEngine {
    data class CompositeTemplate(
        val type: String,
        val bindings: Map<String, Long>,
        val template: String
    )
    
    fun render(template: CompositeTemplate, repo: VaultRepository): String {
        var result = template.template
        // Safe string interpolation {{entry:id:field}}
        val regex = Regex("\\{\\{entry:(\\d+):(\\w+)\\}\\}")
        result = regex.replace(result) { matchResult ->
            val entryId = matchResult.groupValues[1].toLongOrNull()
            val field = matchResult.groupValues[2]
            if (entryId == null) return@replace matchResult.value
            val entry = repo.getById(entryId) ?: return@replace matchResult.value
            when (field) {
                "secret" -> {
                    // Note: This would need decrypted value - simplified
                    "[DECRYPTED_VALUE]"
                }
                "title" -> entry.title
                "username" -> entry.account
                else -> matchResult.value
            }
        }
        return result
    }
    
    fun generateKubeconfig(bindings: Map<String, Long>, repo: VaultRepository): String {
        return """
apiVersion: v1
kind: Config
clusters:
- cluster:
    server: {{entry:${bindings["server"]}:secret}}
    certificate-authority-data: {{entry:${bindings["ca"]}:secret}}
  name: kube-cluster
contexts:
- context:
    cluster: kube-cluster
    user: kube-user
  name: kube-context
current-context: kube-context
users:
- name: kube-user
  user:
    client-certificate-data: {{entry:${bindings["cert"]}:secret}}
    client-key-data: {{entry:${bindings["key"]}:secret}}
        """.trimIndent()
    }
    
    fun generateDotEnv(bindings: Map<String, Long>, repo: VaultRepository): String {
        val sb = StringBuilder()
        bindings.forEach { (key, entryId) ->
            val entry = repo.getById(entryId)
            if (entry != null) {
                sb.append("$key={{entry:$entryId:secret}}\n")
            }
        }
        return sb.toString()
    }
    
    fun generateAwsCredentials(bindings: Map<String, Long>, repo: VaultRepository): String {
        return """
[default]
aws_access_key_id = {{entry:${bindings["accessKey"]}:secret}}
aws_secret_access_key = {{entry:${bindings["secretKey"]}:secret}}
region = {{entry:${bindings["region"]}:secret}}
        """.trimIndent()
    }
}
