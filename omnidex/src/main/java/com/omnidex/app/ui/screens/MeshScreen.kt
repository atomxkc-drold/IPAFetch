package com.omnidex.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnidex.app.domain.VaultEntry
import com.omnidex.app.security.MeshGraphEngine
import com.omnidex.app.ui.theme.OmniDexTheme

@Composable
fun MeshScreen(
    entries: List<VaultEntry>,
    onBack: () -> Unit,
    onEntryClick: (Long) -> Unit
) {
    val colors = OmniDexTheme.colors
    val graph = remember(entries) { MeshGraphEngine.buildGraph(entries) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
                text = "Credential Mesh",
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                // Draw edges
                graph.edges.forEach { edge ->
                    val fromNode = graph.nodes.find { it.entry.id == edge.fromId }
                    val toNode = graph.nodes.find { it.entry.id == edge.toId }
                    if (fromNode != null && toNode != null) {
                        drawLine(
                            color = colors.accent.copy(alpha = 0.3f * edge.weight / 10f),
                            start = Offset(centerX + fromNode.x * scale + offset.x, centerY + fromNode.y * scale + offset.y),
                            end = Offset(centerX + toNode.x * scale + offset.x, centerY + toNode.y * scale + offset.y),
                            strokeWidth = edge.weight
                        )
                    }
                }
                
                // Draw nodes
                graph.nodes.forEach { node ->
                    val nodeColor = when (node.degree) {
                        0 -> colors.textSecondary
                        in 1..2 -> colors.accent
                        else -> colors.warning
                    }
                    drawCircle(
                        color = nodeColor,
                        radius = 8f + node.degree * 2f,
                        center = Offset(centerX + node.x * scale + offset.x, centerY + node.y * scale + offset.y)
                    )
                }
            }
        }
    }
}
