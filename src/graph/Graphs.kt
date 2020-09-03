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
    abstract fun clear()
    
    protected fun checkHasVertex(x: Int): Unit = if (!hasVertex(x)) errorVertexDoesNotExist(x) else {}
    protected fun errorVertexExists(x: Int): Nothing = throw RuntimeException("Vertex $x already exists")
    protected fun errorVertexDoesNotExist(x: Int): Nothing = throw RuntimeException("Vertex $x does not exist")
    protected fun errorEdgeExists(x: Int, y: Int): Nothing = throw RuntimeException("Edge $x->$y already exists")
    protected fun errorEdgeDoesNotExist(x: Int, y: Int): Nothing = throw RuntimeException("Edge $x->$y does not exist")
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
        (adjacencyList[x] ?: errorVertexDoesNotExist(x)).find { it.first == y }?.second

    override fun getNeighbors(x: Int): List<Int> = (adjacencyList[x] ?: errorVertexDoesNotExist(x)).map { it.first }

    override fun addVertex(x: Int) {
        if (hasVertex(x)) errorVertexExists(x)

        adjacencyList[x] = ArrayList()
    }

    override fun removeVertex(x: Int): Boolean {
        adjacencyList.forEach { node ->
            if (node !== null) node.removeIf { it.first == x }
        }
        return if (!hasVertex(x)) false else { adjacencyList[x] = null; true }
    }

    override fun addEdge(x: Int, y: Int, weight: Double) {
        checkHasVertex(x)
        checkHasVertex(y)

        if (getEdge(x, y) !== null) errorEdgeExists(x, y)

        adjacencyList[x]!!.add(Pair(y, weight))
    }

    override fun setEdge(x: Int, y: Int, weight: Double) {
        checkHasVertex(x)
        checkHasVertex(y)

        if (!adjacencyList[x]!!.remove(adjacencyList[x]!!.find { it.first == y })) throw RuntimeException("Edge $x->$y does not exist")
        adjacencyList[x]!!.add(Pair(y, weight))
    }

    override fun removeEdge(x: Int, y: Int): Boolean {
        if (hasVertex(x)) errorVertexDoesNotExist(x)
        if (hasVertex(y)) errorVertexDoesNotExist(y)

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

    override fun clear() {
        adjacencyList.clear()
    }
}

/**
 * AdjacencyMatrixBidirectionalGraph class, implementing the BidirectionalGraph by using the adjacency matrix data structure
 * V represents the number of vertices, E represents the number of edges
 *
 * Memory complexity: O(V^2)
 *
 * Time complexity:
 *
 * hasVertex: O(1)
 * getEdge: O(1)
 * getNeighbors: O(V)
 * addVertex: O(1)
 * removeVertex: O(V)
 * addEdge: O(1)
 * setEdge: O(1)
 * removeEdge: O(1)
 *
 * */

class AdjacencyMatrixBidirectionalGraph: BidirectionalGraph() {
    private val adjacencyMatrix: InfiniteList<InfiniteList<Double>> = InfiniteList()

    override fun hasVertex(x: Int): Boolean = adjacencyMatrix[x] !== null

    override fun getEdge(x: Int, y: Int): Double? =
        (adjacencyMatrix[x] ?: errorVertexDoesNotExist(x))[y]

    override fun getNeighbors(x: Int): List<Int> {
        checkHasVertex(x)
        
        val result = ArrayList<Int>()
        
        for (i in 0..(adjacencyMatrix[x]!!.size - 1)) {
            if (adjacencyMatrix[x]!![i] !== null) result.add(i)
        }
        
        return result
    }

    override fun addVertex(x: Int) {
        if (hasVertex(x)) errorVertexExists(x)
        
        adjacencyMatrix[x] = InfiniteList()
    }

    override fun removeVertex(x: Int): Boolean {
        adjacencyMatrix.forEach{node ->
            if (node !== null) node[x] = null
        }

        return if (hasVertex(x)) { adjacencyMatrix[x] = null; true } else false
    }

    override fun addEdge(x: Int, y: Int, weight: Double) {
        checkHasVertex(x)
        checkHasVertex(y)
        if (adjacencyMatrix[x]!![y] !== null) errorEdgeExists(x, y)
        adjacencyMatrix[x]!![y] = weight
    }

    override fun setEdge(x: Int, y: Int, weight: Double) {
        checkHasVertex(x)
        checkHasVertex(y)
        if (adjacencyMatrix[x]!![y] === null) errorEdgeDoesNotExist(x, y)
        adjacencyMatrix[x]!![y] = weight
    }

    override fun removeEdge(x: Int, y: Int): Boolean {
        checkHasVertex(x)
        checkHasVertex(y)
        return if (getEdge(x, y) !== null) { adjacencyMatrix[x]!![y] = null; true } else false
    }

    override fun clear() {
        adjacencyMatrix.clear()
    }
}

private fun <T: BidirectionalGraph>initGraph(graph: T, vertexCount: Int, edges: List<Triple<Int, Int, Double>>): T {
    for (i in 0..(vertexCount-1)) graph.addVertex(i)

    edges.forEach { graph.addEdge(it.first, it.second, it.third) }

    return graph
}

fun createAdjacencyListGraph(vertexCount: Int, edges: List<Triple<Int, Int, Double>> = ArrayList()): AdjacencyListBidirectionalGraph =
        initGraph(AdjacencyListBidirectionalGraph(), vertexCount, edges)

fun createAdjacencyMatrixGraph(vertexCount: Int, edges: List<Triple<Int, Int, Double>> = ArrayList()): AdjacencyMatrixBidirectionalGraph =
        initGraph(AdjacencyMatrixBidirectionalGraph(), vertexCount, edges)

fun createGraph(vertexCount: Int, edges: List<Triple<Int, Int, Double>> = ArrayList()): BidirectionalGraph =
        if (vertexCount < 500 || edges.size >= vertexCount * (vertexCount - 100)) createAdjacencyMatrixGraph(vertexCount, edges) else createAdjacencyListGraph(vertexCount, edges)
