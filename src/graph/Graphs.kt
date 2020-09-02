package graph

import java.lang.RuntimeException

class Vertex

abstract class BidirectionalGraph {
    abstract fun hasVertex(x: Vertex): Boolean
    abstract fun getEdge(x: Vertex, y: Vertex): Double?
    abstract fun getNeighbors(x: Vertex): List<Vertex>
    abstract fun addVertex(x: Vertex)
    abstract fun removeVertex(x: Vertex): Boolean
    abstract fun addEdge(x: Vertex, y: Vertex, weight: Double)
    abstract fun setEdge(x: Vertex, y: Vertex, weight: Double)
    abstract fun removeEdge(x: Vertex, y: Vertex): Boolean
}


/**
 * AdjacencyListBidirectionalGraph class, implementing the BidirectionalGraph by using the adjacency list data structure
 * V represents the number of vertices, E represents the number of edges
 *
 * Memory complexity: O(V + E)
 *
 * Time complexity:
 *
 * hasVertex: O(V)
 * getEdge: O(V)
 * getNeighbors: O(V)
 * addVertex: O(1)
 * removeVertex: O(E)
 * addEdge: O(1)
 * setEdge: O(V)
 * removeEdge: O(V)
 *
 * */

class AdjacencyListBidirectionalGraph: BidirectionalGraph() {
    private val adjacencyList: MutableMap<Vertex, MutableMap<Vertex, Double>> = HashMap()

    override fun hasVertex(x: Vertex): Boolean = x in adjacencyList

    override fun getEdge(x: Vertex, y: Vertex): Double? =
        (adjacencyList[x] ?: throw RuntimeException("$x is not present in the graph"))[y]

    override fun getNeighbors(x: Vertex): List<Vertex> = (adjacencyList[x] ?: throw RuntimeException("$x is not present in the graph")).map { it.key }

    override fun addVertex(x: Vertex) {
        if (x in adjacencyList) throw RuntimeException("$x is already present in the graph")

        adjacencyList[x] = HashMap()
    }

    override fun removeVertex(x: Vertex): Boolean {
        adjacencyList.forEach { node ->
            node.value.remove(x)
        }
        return adjacencyList.remove(x) !== null
    }

    override fun addEdge(x: Vertex, y: Vertex, weight: Double) {
        if (x !in adjacencyList) throw RuntimeException("$x is not present in the graph")
        if (y !in adjacencyList) throw RuntimeException("$y is not present in the graph")

        adjacencyList[x]!![y] = weight
    }

    override fun setEdge(x: Vertex, y: Vertex, weight: Double) {
        if (x !in adjacencyList) throw RuntimeException("$x is not present in the graph")
        if (y !in adjacencyList) throw RuntimeException("$y is not present in the graph")

        if (adjacencyList[x]!!.remove(y) === null) throw RuntimeException("Edge $x->$y does not exist")
        adjacencyList[x]!![y] = weight
    }

    override fun removeEdge(x: Vertex, y: Vertex): Boolean {
        if (x !in adjacencyList) throw RuntimeException("$x is not present in the graph")
        if (y !in adjacencyList) throw RuntimeException("$y is not present in the graph")

        return adjacencyList[x]!!.remove(y) !== null
    }
}


class AdjacencyMatrixBidirectionalGraph: BidirectionalGraph() {
    private val adjacencyMatrix: MutableMap<Vertex, MutableMap<Vertex, Double?>> = HashMap()

    override fun hasVertex(x: Vertex): Boolean = x in adjacencyMatrix

    override fun getEdge(x: Vertex, y: Vertex): Double? =
        (adjacencyMatrix[x] ?: throw RuntimeException("$x is not present in the graph"))[y]

    override fun getNeighbors(x: Vertex): List<Vertex> = (adjacencyMatrix[x] ?: throw RuntimeException("$x is not present in the graph")).filter { it.value !== null }.map { it.key }

    override fun addVertex(x: Vertex) {
        TODO("not implemented")
    }

    override fun removeVertex(x: Vertex): Boolean {
        TODO("not implemented")
    }

    override fun addEdge(x: Vertex, y: Vertex, weight: Double) {
        TODO("not implemented")
    }

    override fun setEdge(x: Vertex, y: Vertex, weight: Double) {
        TODO("not implemented")
    }

    override fun removeEdge(x: Vertex, y: Vertex): Boolean {
        TODO("not implemented")
    }

}
