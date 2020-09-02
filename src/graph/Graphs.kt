package graph

import java.lang.RuntimeException

abstract class BidirectionalGraph {
    abstract fun hasVertex(x: Int): Boolean
    abstract fun getEdge(x: Int, y: Int): Double?
    abstract fun getNeighbors(x: Int): List<Int>
    abstract fun addVertex(x: Int)
    abstract fun removeVertex(x: Int): Boolean
    abstract fun addEdge(x: Int, y: Int, weight: Double)
    abstract fun setEdge(x: Int, y: Int, weight: Double)
    abstract fun removeEdge(x: Int, y: Int): Boolean
}

class InfiniteList<T>: ArrayList<T?>() {
    override fun get(index: Int): T? {
        if (index >= size) return null
        return super.get(index)
    }

    override fun set(index: Int, element: T?): T? {
        while (index >= size) add(null)
        return super.set(index, element)
    }
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
    private val adjacencyList: InfiniteList<MutableList<Pair<Int, Double>>> = InfiniteList()

    override fun hasVertex(x: Int): Boolean = adjacencyList[x] !== null

    override fun getEdge(x: Int, y: Int): Double? =
        (adjacencyList[x] ?: throw RuntimeException("$x is not present in the graph")).find { it.first == y }?.second

    override fun getNeighbors(x: Int): List<Int> = (adjacencyList[x] ?: throw RuntimeException("$x is not present in the graph")).map { it.first }

    override fun addVertex(x: Int) {
        if (hasVertex(x)) throw RuntimeException("$x is already present in the graph")

        adjacencyList[x] = ArrayList()
    }

    override fun removeVertex(x: Int): Boolean {
        adjacencyList.forEach { node ->
            if (node !== null) node.removeIf { it.first == x }
        }
        return if (!hasVertex(x)) false else { adjacencyList[x] = null; true }
    }

    override fun addEdge(x: Int, y: Int, weight: Double) {
        if (!hasVertex(x)) throw RuntimeException("$x is not present in the graph")
        if (!hasVertex(y)) throw RuntimeException("$y is not present in the graph")

        if (getEdge(x, y) !== null) throw RuntimeException("Edge $x->$y already exists")

        adjacencyList[x]!!.add(Pair(y, weight))
    }

    override fun setEdge(x: Int, y: Int, weight: Double) {
        if (!hasVertex(x)) throw RuntimeException("$x is not present in the graph")
        if (!hasVertex(y)) throw RuntimeException("$y is not present in the graph")

        if (!adjacencyList[x]!!.remove(adjacencyList[x]!!.find { it.first == y })) throw RuntimeException("Edge $x->$y does not exist")
        adjacencyList[x]!!.add(Pair(y, weight))
    }

    override fun removeEdge(x: Int, y: Int): Boolean {
        if (hasVertex(x)) throw RuntimeException("$x is not present in the graph")
        if (hasVertex(y)) throw RuntimeException("$y is not present in the graph")

        return adjacencyList[x]!!.remove(adjacencyList[x]!!.find { it.first == y })
    }

    override fun toString(): String {
        val result = StringBuilder()
        for (i in 0..(adjacencyList.size-1)) {
            val node = adjacencyList[i]
            if (node !== null) {
                result.append("$i\n")
                node.forEach { result.append(" ---${it.second}--->${it.first}\n") }
            }
        }
        return result.toString()
    }
}


class AdjacencyMatrixBidirectionalGraph: BidirectionalGraph() {
    private val adjacencyMatrix: InfiniteList<InfiniteList<Double>> = InfiniteList()

    override fun hasVertex(x: Int): Boolean = adjacencyMatrix[x] !== null

    override fun getEdge(x: Int, y: Int): Double? =
        (adjacencyMatrix[x] ?: throw RuntimeException("$x is not present in the graph"))[y]

    override fun getNeighbors(x: Int): List<Int> {
        TODO("not implemented")
    }

    override fun addVertex(x: Int) {
        TODO("not implemented")
    }

    override fun removeVertex(x: Int): Boolean {
        TODO("not implemented")
    }

    override fun addEdge(x: Int, y: Int, weight: Double) {
        TODO("not implemented")
    }

    override fun setEdge(x: Int, y: Int, weight: Double) {
        TODO("not implemented")
    }

    override fun removeEdge(x: Int, y: Int): Boolean {
        TODO("not implemented")
    }

}
