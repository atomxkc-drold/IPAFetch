package com.omnidex.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnidex.app.domain.LintFlag
import com.omnidex.app.domain.Severity
import com.omnidex.app.domain.VaultEntry
import com.omnidex.app.ui.components.GlassSurface
import com.omnidex.app.ui.theme.OmniDexTheme

@Composable
fun HealthReportScreen(
    entries: List<VaultEntry>,
    onBack: () -> Unit,
    onEntryClick: (VaultEntry) -> Unit
) {
    val colors = OmniDexTheme.colors
    val allFlags = entries.flatMap { entry -> entry.lintFlags.map { entry to it } }
        .sortedBy { (_, flag) -> 
            when (flag.severity) {
                Severity.CRITICAL -> 0
                Severity.WARNING -> 1
                Severity.INFO -> 2
            }
        }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }
            Text(
                text = "Credential Health",
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        val critical = allFlags.count { it.second.severity == Severity.CRITICAL }
        val warning = allFlags.count { it.second.severity == Severity.WARNING }
        val info = allFlags.count { it.second.severity == Severity.INFO }
        
        Text(
            text = "$critical Critical · $warning Warning · $info Info",
            color = colors.textSecondary,
            fontSize = 13.sp
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allFlags) { (entry, flag) ->
                GlassSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = when (flag.severity) {
                                    Severity.CRITICAL -> colors.error
                                    Severity.WARNING -> colors.warning
                                    Severity.INFO -> colors.accent
                                },
                                modifier = Modifier.height(16.dp)
                            )
                            Text(
                                text = entry.title,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = flag.message,
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                        flag.remediation?.let { rem ->
                            Text(
                                text = "Remediation: $rem",
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
