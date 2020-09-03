package traffic

import graph.DirectionalGraph
import graph.InfiniteList
import util.EventEmitter
import java.io.PrintStream
import java.util.*
import kotlin.math.abs
import kotlin.system.measureNanoTime

private fun Double.format(digits: Int) = "%.${digits}f".format(this)

data class TrafficLight(var ratio: Double, val totalTime: Double, val primaryDirections: List<Int>, val autoAdjust: Boolean = true, var currentRatio: Double = 0.0, var primaryStats: Double = 0.0, var secondaryStats: Double = 0.0) {
    constructor(ratio: Double, totalTime: Double, primaryDirections: List<Int>, autoAdjust: Boolean = true): this(ratio, totalTime, primaryDirections, autoAdjust, 0.0)
    fun isOn(): Boolean = ratio > currentRatio
    fun isOn(vertex: Int) = (ratio > currentRatio) == (vertex in primaryDirections)
}

data class Car(val averageSpeed: Double, val offroadQuality: Double, val label: String?,
               var currentRoadFrom: Int, var currentRoadTo: Int, var route: MutableList<Int>,
               var roadProgress: Double = 0.0, var finished: Boolean = false) {
    constructor(averageSpeed: Double, offroadQuality: Double, route: MutableList<Int>, label: String? = null) : this(averageSpeed, offroadQuality, label, route[0], route[1], route) {
        route.removeAt(0)
        route.removeAt(0)
    }
}

data class Road(val quality: Double, val name: String?)

class TrafficSimulation(val map: DirectionalGraph, val trafficLights: InfiniteList<TrafficLight>, val roads: MutableList<Triple<Int, Int, Road>>, val maxTPS: Double = 0.0): EventEmitter<Double>() {
    private val thread = Thread {
        var delay = 0.0
        while (true) {
            delay = measureNanoTime {
                update(delay)
            }.toDouble() / 1_000_000_000
            if (maxTPS > 0.0) {
                if (delay < 1.0 / maxTPS) {
                    Thread.sleep(((1.0 / maxTPS - delay) * 1000).toLong())
                    delay = 1.0 / maxTPS
                }
            }
        }
    }

    var trafficLightTime: Double = 60.0

    fun getRoad(x: Int, y: Int): Road? = roads.find { it.first == x && it.second == y }?.third

    fun start() {
        map.getVertices().forEach { vertex ->
            val neighbors = map.getNeighbors(vertex)
            neighbors.forEach {
                if (getRoad(vertex, it) == null) {
                    roads.add(Triple(vertex, it, Road(1.0, null)))
                }
            }
            if (neighbors.size >= 3 && trafficLights[vertex] == null)
                trafficLights[vertex] = TrafficLight(0.5, trafficLightTime, neighbors.subList(0, neighbors.size/2))
        }
        thread.start()
    }

    fun stop() {
        thread.stop()
    }

    val cars: MutableList<Car> = Collections.synchronizedList(ArrayList())
    var cycle: Int = 0
        private set

    private fun update(delta: Double) {
        emit("update", delta)
        cycle++
        for (i in cars.indices) {
            if (i >= cars.size) {
                System.err.println("Warning: concurrent modification detected")
                break
            }
            val it = cars[i]
            if (!it.finished) {
                val progress =
                    it.roadProgress + delta * it.averageSpeed * getRoad(it.currentRoadFrom, it.currentRoadTo)!!.quality / map.getEdge(it.currentRoadFrom, it.currentRoadTo)!!
                if (progress >= 1) {
                    if (trafficLights[it.currentRoadTo] == null || trafficLights[it.currentRoadTo]!!.isOn(it.currentRoadFrom)) {
                        if (it.route.isEmpty()) {
                            it.finished = true
                        } else {
                            it.currentRoadFrom = it.currentRoadTo
                            it.currentRoadTo = it.route[0]
                            it.route.removeAt(0)
                            it.roadProgress = 0.0
                        }
                    } else {
                        it.roadProgress = 1.0
                        if (it.currentRoadFrom in trafficLights[it.currentRoadTo]!!.primaryDirections) {
                            trafficLights[it.currentRoadTo]!!.primaryStats += delta
                        } else {
                            trafficLights[it.currentRoadTo]!!.secondaryStats += delta
                        }
                    }
                } else it.roadProgress = progress
            }
        }

        cars.removeIf { it.finished }

        trafficLights.forEach { if (it !== null) {
            it.currentRatio = (it.currentRatio + delta / it.totalTime) % 1.0
            if (it.autoAdjust && it.primaryStats + it.secondaryStats >= 300.0
                    && abs((it.primaryStats - it.secondaryStats) / (it.primaryStats + it.secondaryStats)) > 0.1) {
                if ((it.ratio == 0.0 && it.primaryStats > 0.0) || (it.ratio == 1.0 && it.secondaryStats > 0.0)) {
                    it.ratio = it.primaryStats / (it.primaryStats + it.secondaryStats)
                } else {
                    it.ratio = (it.primaryStats / (1 - it.ratio)) / (it.primaryStats / (1 - it.ratio) + it.secondaryStats / it.ratio)
                }
                it.primaryStats = 0.0
                it.secondaryStats = 0.0
            }
        }}
    }

    fun addCar(car: Car) {
        cars.add(car)
    }

    fun dumpCars(stream: PrintStream) {
        cars.forEachIndexed { i, it ->
            stream.println("Car #$i ${ if (it.label !== null) "'${it.label}' " else "" }${it.currentRoadFrom}----${(it.roadProgress * 100).toInt()}%--->${it.currentRoadTo} " +
                    "(speed: average: ${it.averageSpeed.format(2)}, current: ${it.averageSpeed.times(it.offroadQuality).times(getRoad(it.currentRoadFrom, it.currentRoadTo)!!.quality).format(2)})")
        }
    }

    fun dumpTrafficLights(stream: PrintStream) {
        trafficLights.forEachIndexed { i, it -> if (it !== null) {
            stream.println("Traffic light #$i ${ if(it.isOn()) "ON " else "OFF" } ${it.primaryDirections} ${(it.currentRatio*100).toInt()}%/${(it.ratio*100).toInt()}% (stats: ${it.primaryStats.format(2)}/${it.secondaryStats.format(2)})")
        }}
    }
}
