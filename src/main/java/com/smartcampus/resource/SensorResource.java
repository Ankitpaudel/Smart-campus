package com.smartcampus.resource;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.smartcampus.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;

/**
 * Part 3 & 4 - Sensor Resource
 * Manages the /api/v1/sensors collection and delegates to SensorReadingResource.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final Map<String, Sensor> sensors = DataStore.getInstance().getSensors();
    private final Map<String, Room> rooms = DataStore.getInstance().getRooms();

    // ── GET /api/v1/sensors 
    /**
     * Part 3.2 - Optional @QueryParam filtering.
     * Using a query parameter (not a path segment) is the correct REST idiom for
     * filtering a collection. Path params identify resources; query params refine them.
     */
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> result = new ArrayList<>(sensors.values());
        if (type != null && !type.isBlank()) {
            result = result.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        return Response.ok(result).build();
    }

    // ── POST /api/v1/sensors 
    /**
     * Part 3.1 - Sensor registration with roomId validation.
     * @Consumes(APPLICATION_JSON) means JAX-RS will return 415 Unsupported Media Type
     * automatically if the client sends text/plain or application/xml.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Sensor id is required"))
                    .build();
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "roomId is required"))
                    .build();
        }
        // Foreign key validation: roomId must reference an existing room
        Room room = rooms.get(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException("Room", sensor.getRoomId());
        }
        if (sensors.containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Sensor '" + sensor.getId() + "' already exists"))
                    .build();
        }
        // Ensure a default status
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }
        sensors.put(sensor.getId(), sensor);

        // Maintain the bi-directional link: add sensorId to the room
        room.addSensorId(sensor.getId());

        // Initialise the reading history list for this sensor
        DataStore.getInstance().getSensorReadings().put(sensor.getId(), new ArrayList<>());

        return Response.created(URI.create("/api/v1/sensors/" + sensor.getId()))
                .entity(sensor)
                .build();
    }

    // ── GET /api/v1/sensors/{sensorId}
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor '" + sensorId + "' not found"))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // ── DELETE /api/v1/sensors/{sensorId} 
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor '" + sensorId + "' not found"))
                    .build();
        }
        // Remove back-reference from the room
        Room room = rooms.get(sensor.getRoomId());
        if (room != null) {
            room.removeSensorId(sensorId);
        }
        sensors.remove(sensorId);
        DataStore.getInstance().getSensorReadings().remove(sensorId);
        return Response.noContent().build();
    }

    // ── Part 4.1: Sub-Resource Locator 
    /**
     * This method is a Sub-Resource Locator. It does NOT handle the HTTP request
     * itself — instead it returns an object (SensorReadingResource) that Jersey will
     * then use to service the remainder of the path (/readings, /readings/{id}).
     *
     * This pattern allows large APIs to delegate nested paths to dedicated classes,
     * keeping controllers focused and maintainable.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
