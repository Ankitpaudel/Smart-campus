package com.smartcampus.resource;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.smartcampus.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

/**
 * Part 4.1 & 4.2 - Sub-Resource: Sensor Readings
 *
 * This class is NOT annotated with @Path at the class level because it is
 * instantiated via a sub-resource locator in SensorResource, not by Jersey's
 * direct path matching. The locator injects the sensorId at construction time.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final Map<String, Sensor> sensors = DataStore.getInstance().getSensors();
    private final Map<String, List<SensorReading>> readingsStore = DataStore.getInstance().getSensorReadings();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ── GET /api/v1/sensors/{sensorId}/readings
    @GET
    public Response getReadings() {
        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor '" + sensorId + "' not found"))
                    .build();
        }
        List<SensorReading> readings = readingsStore.getOrDefault(sensorId, new ArrayList<>());
        return Response.ok(readings).build();
    }

    // ─ POST /api/v1/sensors/{sensorId}/readings
    /**
     * Appends a new reading and updates the parent sensor's currentValue.
     * Throws SensorUnavailableException (→ 403) if the sensor is in MAINTENANCE state.
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor '" + sensorId + "' not found"))
                    .build();
        }
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }
        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Reading body is required"))
                    .build();
        }

        // Assign an ID and timestamp if not provided by the client
        SensorReading newReading = new SensorReading(reading.getValue());

        // Append to history
        readingsStore.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(newReading);

        // Side-effect: update the parent sensor's currentValue
        sensor.setCurrentValue(newReading.getValue());

        return Response.created(URI.create("/api/v1/sensors/" + sensorId + "/readings/" + newReading.getId()))
                .entity(newReading)
                .build();
    }
}
