package graph

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.lang.RuntimeException

private data class MutablePair<A, B>(
    var first: A,
    var second: B
) {
    override fun toString(): String = "($first, $second)"
}

private data class MutableTriple<A, B, C>(
    var first: A,
    var second: B,
    var third: C
) {
    override fun toString(): String = "($first, $second, $third)"
}

abstract class BidirectionalGraph {
    abstract fun hasVertex(x: Int): Boolean
    abstract fun getEdge(x: Int, y: Int): Double?
    abstract fun getVertices(): List<Int>
    abstract fun getNeighbors(x: Int): List<Int>
    abstract fun addVertex(x: Int)
    abstract fun removeVertex(x: Int): Boolean
    abstract fun addEdge(x: Int, y: Int, value: Double)
    abstract fun setEdge(x: Int, y: Int, value: Double)
    abstract fun removeEdge(x: Int, y: Int): Boolean
    abstract fun clear()


    protected fun checkHasVertex(x: Int): Unit = if (!hasVertex(x)) errorVertexDoesNotExist(x) else {}
    protected fun errorVertexExists(x: Int): Nothing = throw RuntimeException("Vertex $x already exists")
    protected fun errorVertexDoesNotExist(x: Int): Nothing = throw RuntimeException("Vertex $x does not exist")
    protected fun errorEdgeExists(x: Int, y: Int): Nothing = throw RuntimeException("Edge $x->$y already exists")
    protected fun errorEdgeDoesNotExist(x: Int, y: Int): Nothing = throw RuntimeException("Edge $x->$y does not exist")

    override fun toString(): String {
        val result = StringBuilder()
        val vertices = getVertices()
        for (i in 0..(vertices.size-1)) {
            result.append("$i\n")
            getNeighbors(i).forEach { result.append(" ---${getEdge(i, it)}--->$it\n") }
        }
        return result.toString()
    }

    fun serialize(): ByteArray {
        val result = ByteArrayOutputStream()
        val outputStream = DataOutputStream(result)
        outputStream.writeByte('k'.toInt())
        outputStream.writeByte('a'.toInt())
        outputStream.writeByte('r'.toInt())
        outputStream.writeByte(VERSION.toInt())
        val vertices = getVertices()
        outputStream.writeInt(vertices.size)
        vertices.forEach { x ->
            outputStream.writeInt(x)
            val neighbors = getNeighbors(x)
            outputStream.writeInt(neighbors.size)
            neighbors.forEach {
                outputStream.writeInt(it)
                outputStream.writeDouble(getEdge(x, it)!!)
            }
        }
        outputStream.flush()
        return result.toByteArray()
    }

    companion object {
        const val VERSION: Byte = 0
    }
}

inline fun <reified T: BidirectionalGraph>deserialize(data: ByteArray): T = deserialize(T::class.java.newInstance(), data) as T

fun deserialize(graph: BidirectionalGraph, data: ByteArray): BidirectionalGraph {
    graph.clear()

    val vertices = ArrayList<Int>()
    val edges = ArrayList<Triple<Int, Int, Double>>()

    val inputStream = DataInputStream(ByteArrayInputStream(data))

    if (inputStream.readByte().toChar() != 'k') throw RuntimeException("Invalid file format: 'k' expected")
    if (inputStream.readByte().toChar() != 'a') throw RuntimeException("Invalid file format: 'a' expected")
    if (inputStream.readByte().toChar() != 'r') throw RuntimeException("Invalid file format: 'r' expected")

    val version = inputStream.readByte()
    if (version != BidirectionalGraph.VERSION) throw RuntimeException("Wrong version $version (${BidirectionalGraph.VERSION} expected)")

    val verticesCount = inputStream.readInt()

    for (i in 0..(verticesCount-1)) {
        val vertex = inputStream.readInt()
        vertices.add(vertex)
        val edgeCount = inputStream.readInt()
        for (j in 0..(edgeCount-1)) {
            val y = inputStream.readInt()
            val weight = inputStream.readDouble()
            edges.add(Triple(vertex, y, weight))
        }
    }

    initGraph(graph, vertices, edges)
    return graph
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
 * hasVertex: O(1)
 * getEdge: O(V)
 * getVertices: O(V)
 * getNeighbors: O(1)
 * addVertex: O(1)
 * removeVertex: O(E)
 * addEdge: O(1)
 * setEdge: O(V)
 * removeEdge: O(V)
 *
 * */

class AdjacencyListBidirectionalGraph: BidirectionalGraph() {
    private val adjacencyList: InfiniteList<MutableList<MutablePair<Int, Double>>> = InfiniteList()

    override fun hasVertex(x: Int): Boolean = adjacencyList[x] !== null

    override fun getEdge(x: Int, y: Int): Double? =
        (adjacencyList[x] ?: errorVertexDoesNotExist(x)).find { it.first == y }?.second

    override fun getVertices(): List<Int> {
        val result = ArrayList<Int>()
        for (i in 0..(adjacencyList.size-1)) if (adjacencyList[i] !== null) result.add(i)
        return result
    }

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

    override fun addEdge(x: Int, y: Int, value: Double) {
        checkHasVertex(x)
        checkHasVertex(y)

        if (getEdge(x, y) !== null) errorEdgeExists(x, y)

        adjacencyList[x]!!.add(MutablePair(y, value))
    }

    override fun setEdge(x: Int, y: Int, value: Double) {
        checkHasVertex(x)
        checkHasVertex(y)

        (adjacencyList[x]!!.find { it.first == y } ?: errorEdgeDoesNotExist(x, y)).second = value
    }

    override fun removeEdge(x: Int, y: Int): Boolean {
        if (hasVertex(x)) errorVertexDoesNotExist(x)
        if (hasVertex(y)) errorVertexDoesNotExist(y)

        return adjacencyList[x]!!.remove(adjacencyList[x]!!.find { it.first == y })
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
 * getVertices: O(V)
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

    override fun getVertices(): List<Int> {
        val result = ArrayList<Int>()
        for (i in 0..(adjacencyMatrix.size-1)) if (adjacencyMatrix[i] !== null) result.add(i)
        return result
    }

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

    override fun addEdge(x: Int, y: Int, value: Double) {
        checkHasVertex(x)
        checkHasVertex(y)
        if (adjacencyMatrix[x]!![y] !== null) errorEdgeExists(x, y)
        adjacencyMatrix[x]!![y] = value
    }

    override fun setEdge(x: Int, y: Int, value: Double) {
        checkHasVertex(x)
        checkHasVertex(y)
        if (adjacencyMatrix[x]!![y] === null) errorEdgeDoesNotExist(x, y)
        adjacencyMatrix[x]!![y] = value
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

private fun <T: BidirectionalGraph>initGraph(graph: T, vertices: List<Int>, edges: List<Triple<Int, Int, Double>>): T {
    vertices.forEach { graph.addVertex(it) }

    edges.forEach { graph.addEdge(it.first, it.second, it.third) }

    return graph
}

fun createAdjacencyListGraph(vertexCount: Int, edges: List<Triple<Int, Int, Double>> = ArrayList()): AdjacencyListBidirectionalGraph =
        initGraph(AdjacencyListBidirectionalGraph(), vertexCount, edges)

fun createAdjacencyMatrixGraph(vertexCount: Int, edges: List<Triple<Int, Int, Double>> = ArrayList()): AdjacencyMatrixBidirectionalGraph =
        initGraph(AdjacencyMatrixBidirectionalGraph(), vertexCount, edges)

/**
 * Creates a graph using an *intelligent* algorithm to determine the most efficient graph possible
 *
 * The main disadvantage of the adjacency matrix implementation is its memory complexity (it's better in almost everything else),
 * but it doesn't really matter if 1) the vertex count is too small or 2) an adjacency list graph would also consume about that much memory.
 *
 * Thus if either vertex count is smaller than 500 or the amount of edges is close to V^2, then we use the adjacency matrix graph.
 * */

fun createGraph(vertexCount: Int, edges: List<Triple<Int, Int, Double>> = ArrayList()): BidirectionalGraph =
        if (vertexCount < 500 || edges.size >= vertexCount * (vertexCount - 100)) createAdjacencyMatrixGraph(vertexCount, edges) else createAdjacencyListGraph(vertexCount, edges)

/**
 * Finds an optimal path using the Dijkstra algorithm
 *
 * Returns (pathLength, path) if a path was found, null otherwise
 * */

fun minPath(graph: BidirectionalGraph, x: Int, y: Int, getLength: (Int, Int) -> Double = { x, y -> graph.getEdge(x, y)!! }): Pair<Double, List<Int>>? {
    if (!graph.hasVertex(x)) throw RuntimeException("The graph doesn't have a vertex $x")
    if (!graph.hasVertex(y)) throw RuntimeException("The graph doesn't have a vertex $y")
    if (x == y) return Pair(0.0, ArrayList())

    val vertexData = InfiniteList<MutableTriple<Double?, Boolean, List<Int>?>>()

    graph.getVertices().forEachIndexed { i, it ->
        vertexData[i] = MutableTriple(if (it == x) 0.0 else null, false, if (it == x) ArrayList() else null) }

    while (true) {
        var minId: Int? = null

        vertexData.forEachIndexed { i, it ->
            if (it !== null && !it.second && it.first !== null && (minId === null || it.first!! < vertexData[minId!!]!!.first!!)) {
                minId = i
            }
        }

        if (minId == null) break

        val a = minId!!

        if (a == y) break

        vertexData[a]!!.second = true

        graph.getNeighbors(a).forEach {
            if (!vertexData[it]!!.second) {
                val newDistance = vertexData[a]!!.first!! + getLength(a, it)
                if (vertexData[it]!!.first == null || vertexData[it]!!.first!! > newDistance) {
                    vertexData[it]!!.first = newDistance
                    val newRoute = ArrayList<Int>()
                    newRoute.addAll(vertexData[a]!!.third!!)
                    newRoute.add(it)
                    vertexData[it]!!.third = newRoute
                }
            }
        }
    }
    return if (vertexData[y]!!.first == null) null else Pair(vertexData[y]!!.first!!, vertexData[y]!!.third!!)
}
