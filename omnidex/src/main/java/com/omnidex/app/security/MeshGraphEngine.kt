package com.omnidex.app.security

import com.omnidex.app.domain.MeshEdgeType
import com.omnidex.app.domain.VaultEntry
import kotlin.math.sqrt

object MeshGraphEngine {
    data class MeshNode(
        val entry: VaultEntry,
        val degree: Int,
        var x: Float = 0f,
        var y: Float = 0f
    )
    
    data class MeshEdge(
        val fromId: Long,
        val toId: Long,
        val type: MeshEdgeType,
        val weight: Float
    )
    
    data class MeshGraph(
        val nodes: List<MeshNode>,
        val edges: List<MeshEdge>
    )
    
    fun buildGraph(entries: List<VaultEntry>): MeshGraph {
        val nodes = mutableListOf<MeshNode>()
        val edges = mutableListOf<MeshEdge>()
        
        // Create nodes
        entries.forEachIndexed { index, entry ->
            val angle = (2 * Math.PI * index / entries.size).toFloat()
            val radius = 200f
            nodes.add(
                MeshNode(
                    entry = entry,
                    degree = 0,
                    x = radius * kotlin.math.cos(angle),
                    y = radius * kotlin.math.sin(angle)
                )
            )
        }
        
        // Build edges
        for (i in entries.indices) {
            for (j in i + 1 until entries.size) {
                val entry1 = entries[i]
                val entry2 = entries[j]
                var weight = 0f
                var edgeType: MeshEdgeType? = null
                
                // Check derived relationship
                if (entry1.derivedFromId == entry2.id || entry2.derivedFromId == entry1.id) {
                    weight = 10f
                    edgeType = MeshEdgeType.DERIVES_FROM
                }
                // Check used by apps overlap
                else if (entry1.usedByApps.isNotEmpty() && entry2.usedByApps.isNotEmpty()) {
                    val overlap = entry1.usedByApps.intersect(entry2.usedByApps.toSet())
                    if (overlap.isNotEmpty()) {
                        weight = overlap.size * 3f
                        edgeType = MeshEdgeType.USED_TOGETHER
                    }
                }
                // Check same category
                else if (entry1.category == entry2.category && entry1.category.isNotEmpty()) {
                    weight = 2f
                    edgeType = MeshEdgeType.SAME_CATEGORY
                }
                // Check recent updates (rotates together)
                else {
                    val timeDiff = kotlin.math.abs(entry1.updatedAt - entry2.updatedAt)
                    val oneHour = 60 * 60 * 1000L
                    if (timeDiff < oneHour) {
                        weight = 1.5f
                        edgeType = MeshEdgeType.ROTATES_WITH
                    }
                }
                
                if (edgeType != null && weight > 0f) {
                    edges.add(MeshEdge(entry1.id, entry2.id, edgeType, weight))
                }
            }
        }
        
        // Update degrees
        val degreeMap = mutableMapOf<Long, Int>()
        edges.forEach { edge ->
            degreeMap[edge.fromId] = (degreeMap[edge.fromId] ?: 0) + 1
            degreeMap[edge.toId] = (degreeMap[edge.toId] ?: 0) + 1
        }
        
        val updatedNodes = nodes.map { node ->
            node.copy(degree = degreeMap[node.entry.id] ?: 0)
        }
        
        return MeshGraph(updatedNodes, edges)
    }
}
