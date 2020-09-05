import graph.*;
import kotlin.Pair;
import kotlin.Triple;
import kotlin.Unit;
import traffic.Car;
import traffic.Road;
import traffic.TrafficLight;
import traffic.TrafficSimulation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {
    public static void main(String[] args) throws IOException {
        DirectionalGraph map = new AdjacencyMatrixDirectionalGraph();
        map.addVertex(0);
        map.addVertex(1);
        map.addVertex(2);
        map.addVertex(3);
        map.addEdge(0, 1, 1000);
        map.addEdge(0, 2, 1400);
        map.addEdge(1, 2, 1000);
        map.addEdge(2, 3, 1000);
        map.addEdge(3, 0, 1000);
        List<Integer> route1 = new ArrayList<>();
        route1.add(0);
        route1.add(1);
        route1.add(2);
        route1.add(3);
        route1.add(0);
        route1.add(1);
        route1.add(2);
        route1.add(3);
        Car car1 = new Car(100, 0.5, route1, "car1");
        List<Integer> route2 = new ArrayList<>();
        route2.add(0);
        route2.add(2);
        route2.add(3);
        route2.add(0);
        route2.add(2);
        route2.add(3);
        Car car2 = new Car(90, 0.7, route2, "car2");
        InfiniteList<TrafficLight> trafficLights = new InfiniteList<>();
        trafficLights.set(2, new TrafficLight(0.5, 90.0, Arrays.asList(1), true));
        List<Triple<Integer, Integer, Road>> roads = new ArrayList<>();
        roads.add(new Triple<>(0, 2, new Road(0.5, "kar")));
        TrafficSimulation simulation = new TrafficSimulation(map, trafficLights, roads, 200.0);
        simulation.addCar(car1);
        simulation.addCar(car2);
        simulation.on("update", delta -> {
            System.out.println();
            System.out.println("Delta " + delta);
            simulation.dumpCars(System.out);
            simulation.dumpTrafficLights(System.out);
            return Unit.INSTANCE;
        });
        simulation.start();
    }
}
