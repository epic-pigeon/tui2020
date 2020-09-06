package traffic

import graph.DirectionalGraph
import graph.InfiniteList
import util.EventEmitter
import util.forEachConcurrentSafe
import util.forEachIndexedConcurrentSafe
import java.io.PrintStream
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.system.measureNanoTime

private fun Double.format(digits: Int) = "%.${digits}f".format(this)

data class TrafficLight(var ratio: Double, val totalTime: Double, val primaryDirections: List<Int>, val autoAdjust: Boolean = true, var currentRatio: Double = 0.0, var primaryStats: Double = 0.0, var secondaryStats: Double = 0.0) {
    constructor(ratio: Double, totalTime: Double, primaryDirections: List<Int>, autoAdjust: Boolean = true): this(ratio, totalTime, primaryDirections, autoAdjust, 0.0)
    fun isOn(): Boolean = ratio > currentRatio
    fun isOn(vertex: Int) = (ratio > currentRatio) == (vertex in primaryDirections)
}

data class Car(val averageSpeed: Double, val offroadQuality: Double, val label: String?, val route: MutableList<Int>)

data class SimulatedCar(val averageSpeed: Double, val offroadQuality: Double, val label: String?,
                        var currentRoadFrom: Int, var currentRoadTo: Int, var route: MutableList<Int>,
                        var roadProgress: Double = 0.0, var finished: Boolean = false, var currentLane: Int = 0,
                        var totalDistance: Double = 0.0, var totalTime: Double = 0.0, var standingReason: Int? = null, var currentSpeed: Double = 0.0) {
    constructor(car: Car): this(car.averageSpeed, car.offroadQuality, car.label, car.route[0], car.route[1], car.route.filterIndexed {i, _ -> i >= 2}.toMutableList())
}

data class Road(val quality: Double, val laneCount: Int, val name: String?)

data class SimulatedRoad(val quality: Double, val laneCount: Int, val name: String?) {
    constructor(road: Road): this(road.quality, road.laneCount, road.name)
}

private fun roadProgressComparator() = kotlin.Comparator<SimulatedCar> { o1, o2 -> if (o1.roadProgress < o2.roadProgress) -1 else if (o1.roadProgress == o2.roadProgress) 0 else 1 }

class TrafficSimulation(val map: DirectionalGraph, val trafficLights: InfiniteList<TrafficLight>, roadParams: List<Triple<Int, Int, Road>>, val maxTPS: Double = 0.0): EventEmitter<Double>() {
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

    val roads: MutableList<Triple<Int, Int, SimulatedRoad>> = roadParams.map { Triple(it.first, it.second, SimulatedRoad(it.third)) }.toMutableList()

    var trafficLightTime: Double = 60.0

    fun getRoad(x: Int, y: Int): SimulatedRoad? = roads.find { it.first == x && it.second == y }?.third

    fun start() {
        map.getVertices().forEachConcurrentSafe { vertex ->
            val neighbors = map.getNeighbors(vertex)
            neighbors.forEachConcurrentSafe {
                if (getRoad(vertex, it) == null) {
                    roads.add(Triple(vertex, it, SimulatedRoad(Road(1.0, 1, null))))
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

    val cars: MutableList<SimulatedCar> = Collections.synchronizedList(ArrayList())
    var cycle: Int = 0
        private set

    private fun getMaxProgress(carsOnLane: List<SimulatedCar>, roadProgress: Double, roadLength: Double): Double? {
        val car = carsOnLane.sortedWith(roadProgressComparator()).reversed().find { (it.roadProgress * roadLength - 5) / roadLength >= roadProgress } ?: return null
        return (car.roadProgress * roadLength - 5) / roadLength
    }

    private fun occupiesProgress(progress: Double, carProgress: Double, roadLength: Double) =
        progress < carProgress && progress > (carProgress * roadLength - 5) / roadLength

    private fun canFitIntoLane(carsOnLane: List<SimulatedCar>, progress: Double, roadLength: Double) =
        carsOnLane.find {
            occupiesProgress(progress, it.roadProgress, roadLength) ||
            occupiesProgress((progress * roadLength - 5) / roadLength, it.roadProgress, roadLength)
        } == null

    private fun getCarsOnLane(x: Int, y: Int, lane: Int) = cars.filter { it.currentRoadFrom == x && it.currentRoadTo == y && it.currentLane == lane }
    private fun getCarsOnLaneSorted(x: Int, y: Int, lane: Int) = getCarsOnLane(x, y, lane).sortedWith(roadProgressComparator())

    private fun update(delta: Double) {
        emit("update", delta)
        cycle++
        for (i in cars.indices) {
            if (i >= cars.size) {
                System.err.println("Warning: concurrent modification detected")
                break
            }
            val car = cars[i]
            if (!car.finished) {
                val _progress = car.roadProgress
                val roadLength = map.getEdge(car.currentRoadFrom, car.currentRoadTo)!!
                val road = getRoad(car.currentRoadFrom, car.currentRoadTo)!!
                var progress =
                    car.roadProgress + delta * calculateSpeed(car.averageSpeed, road.quality, car.offroadQuality) / roadLength
                val carsOnLane = getCarsOnLaneSorted(car.currentRoadFrom, car.currentRoadTo, car.currentLane)

                if (carsOnLane.size > carsOnLane.indexOf(car) + 1) {
                    val maxProgress: Double = (carsOnLane[carsOnLane.indexOf(car)+1].roadProgress * roadLength - 5) / roadLength
                    if (maxProgress < progress) {
                        if (road.laneCount > 1) {
                            val lanes = listOf(car.currentLane - 1, car.currentLane, car.currentLane + 1)
                                .filter { it >= 0 && it < road.laneCount }
                                .filter { canFitIntoLane(getCarsOnLane(car.currentRoadFrom, car.currentRoadTo, it).filter { theCar -> theCar !== car }, car.roadProgress, roadLength) }
                                .sortedWith(Comparator.comparingDouble{ getMaxProgress(getCarsOnLane(car.currentRoadFrom, car.currentRoadTo, it).filter { theCar -> theCar !== car }, car.roadProgress, roadLength) ?: Double.POSITIVE_INFINITY })

                            if (lanes.isNotEmpty()) {
                                val newMax = getMaxProgress(getCarsOnLane(car.currentRoadFrom, car.currentRoadTo, lanes.last())
                                    .filter { theCar -> theCar !== car }, car.roadProgress, roadLength)
                                if (newMax != null) progress = min(progress, newMax)
                                car.currentLane = lanes.last()
                            } else {
                                progress = car.roadProgress
                            }
                        } else progress = maxProgress
                    }

                    if (progress - _progress == 0.0) {
                        car.standingReason = carsOnLane[carsOnLane.indexOf(car)+1].standingReason
                    } else {
                        car.standingReason = null
                    }
                }

                val transferToNextRoad = {
                    if (car.route.isEmpty()) {
                        car.finished = true
                    } else {
                        var maxProgress: Double = -1.0
                        for (i in 0 until road.laneCount) {
                            val newMax =
                                getMaxProgress(getCarsOnLane(car.currentRoadTo, car.route[0], i), 0.0, roadLength)
                            if (newMax == null) maxProgress = Double.POSITIVE_INFINITY else if (newMax > maxProgress) maxProgress = newMax
                        }
                        if (maxProgress >= 5.0 / map.getEdge(car.currentRoadTo, car.route[0])!!) {
                            car.currentRoadFrom = car.currentRoadTo
                            car.currentRoadTo = car.route[0]
                            car.route.removeAt(0)
                            car.roadProgress = 0.0
                            car.standingReason = null
                        } else {
                            car.standingReason =
                                    getCarsOnLaneSorted(car.currentRoadTo, car.route[0], 0).first().standingReason
                        }
                    }
                }

                if (progress >= 1) {
                    if (trafficLights[car.currentRoadTo] == null || trafficLights[car.currentRoadTo]!!.isOn(car.currentRoadFrom)) {
                        transferToNextRoad()
                    } else {
                        car.roadProgress = 1.0
                        car.standingReason = car.currentRoadTo
                        if (car.currentRoadFrom in trafficLights[car.currentRoadTo]!!.primaryDirections) {
                            trafficLights[car.currentRoadTo]!!.primaryStats += delta
                        } else {
                            trafficLights[car.currentRoadTo]!!.secondaryStats += delta
                        }
                    }
                } else car.roadProgress = progress

                val distanceTravelled = if (_progress < car.roadProgress) (car.roadProgress - _progress) * roadLength else 0.0
                car.totalDistance += distanceTravelled
                car.currentSpeed = distanceTravelled / delta
                car.totalTime += delta
            }
        }

        cars.removeIf { it.finished }

        trafficLights.forEachConcurrentSafe { if (it !== null) {
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

    private fun calculateSpeed(averageSpeed: Double, roadQuality: Double, offroadQuality: Double): Double =
            averageSpeed - averageSpeed * (1 - roadQuality) * (1 - offroadQuality)

    fun addCar(car: Car) {
        cars.add(SimulatedCar(car))
    }

    fun dumpCars(stream: PrintStream) {
        cars.forEachIndexedConcurrentSafe { i, it ->
            stream.println("Car #$i ${ if (it.label !== null) "'${it.label}' " else "" } on ${it.currentLane + 1} ${it.currentRoadFrom}----${(it.roadProgress * 100).toInt()}%--->${it.currentRoadTo} " +
                    "(speed: average: ${it.averageSpeed.format(2)}, " +
                    "current: ${it.currentSpeed.format(2)}${if (it.standingReason != null) " (standing reason: #${it.standingReason})" else ""})")
        }
    }

    fun dumpTrafficLights(stream: PrintStream) {
        trafficLights.forEachIndexedConcurrentSafe { i, it -> if (it !== null) {
            stream.println("Traffic light #$i ${ if(it.isOn()) "ON " else "OFF" } ${it.primaryDirections} ${(it.currentRatio*100).toInt()}%/${(it.ratio*100).toInt()}% (stats: ${it.primaryStats.format(2)}/${it.secondaryStats.format(2)})")
        }}
    }
}
