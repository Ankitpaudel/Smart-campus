package com.smartcampus;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory data store for the Smart Campus API.
 *
 * Because JAX-RS creates a new Resource instance per request, we cannot store
 * shared data on the resource objects themselves. Instead, we centralise all
 * mutable state here using ConcurrentHashMap, which is thread-safe for basic
 * operations without requiring explicit synchronisation for most use cases.
 *
 * For compound operations (e.g., "check-then-insert") we still use synchronized
 * blocks to maintain atomicity.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    // Primary data maps
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    private DataStore() {
        // Seed with some demo data so the Discovery endpoint shows real content
        Room r1 = new Room("LIB-301", "Library Quiet Study", 40);
        Room r2 = new Room("LAB-101", "Computer Science Lab", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        sensors.put(s1.getId(), s1);
        r1.addSensorId(s1.getId());

        sensorReadings.put(s1.getId(), new ArrayList<>());
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public Map<String, List<SensorReading>> getSensorReadings() {
        return sensorReadings;
    }
}
