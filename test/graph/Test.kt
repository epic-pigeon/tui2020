package graph

import kotlin.math.floor
import kotlin.test.asserter

fun main() {
    testGraph(AdjacencyListBidirectionalGraph(), 100)
    testGraph(AdjacencyMatrixBidirectionalGraph(), 100)
}

fun testGraph(graph: BidirectionalGraph, vertextCount: Int) {
    graph.clear()
    val edges = ArrayList<Triple<Int, Int, Double>>()

    for (i in 0..(vertextCount * vertextCount / 2)) {
        val x: Int = floor(Math.random() * vertextCount).toInt()
        var y: Int = floor(Math.random() * vertextCount).toInt()
        while (x == y) y = floor(Math.random() * vertextCount).toInt()
        if (edges.find { it.first == x && it.second == y } === null) edges.add(Triple(x, y, Math.random() * 20 - 10))
    }

    for (i in 0..(vertextCount-1)) graph.addVertex(i)

    for (edge in edges) graph.addEdge(edge.first, edge.second, edge.third)

    for (i in 0..vertextCount) {
        val edge = edges[floor(Math.random() * edges.size).toInt()]
        asserter.assertEquals("the edge weight is as expected", edge.third, graph.getEdge(edge.first, edge.second))
    }
}
