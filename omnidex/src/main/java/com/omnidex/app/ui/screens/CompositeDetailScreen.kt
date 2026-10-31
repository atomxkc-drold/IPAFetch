package com.omnidex.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnidex.app.domain.VaultEntry
import com.omnidex.app.ui.components.PrimaryButton
import com.omnidex.app.ui.theme.OmniDexTheme

@Composable
fun CompositeDetailScreen(
    entry: VaultEntry?,
    onBack: () -> Unit,
    onGenerate: () -> Unit
) {
    val colors = OmniDexTheme.colors
    
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
                text = "Composite Template",
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.Code, contentDescription = null, tint = colors.accent)
            Text(
                text = entry?.title ?: "Unknown",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Text(
            text = "Generate composite artifacts from multiple vault entries",
            color = colors.textSecondary,
            fontSize = 13.sp
        )
        
        PrimaryButton(
            text = "Generate & Copy",
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth()
        )
        
        PrimaryButton(
            text = "Share",
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
