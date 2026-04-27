package com.smartcampus.resource;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.smartcampus.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

/**
 * Part 2 - Room Resource
 * Manages the /api/v1/rooms collection.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final Map<String, Room> rooms = DataStore.getInstance().getRooms();
    private final Map<String, com.smartcampus.model.Sensor> sensors = DataStore.getInstance().getSensors();

    // ── GET /api/v1/rooms 
    @GET
    public Response getAllRooms() {
        Collection<Room> allRooms = rooms.values();
        return Response.ok(new ArrayList<>(allRooms)).build();
    }

    // ── POST /api/v1/rooms
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Room id is required"))
                    .build();
        }
        if (rooms.containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "A room with id '" + room.getId() + "' already exists"))
                    .build();
        }
        rooms.put(room.getId(), room);
        return Response.created(URI.create("/api/v1/rooms/" + room.getId()))
                .entity(room)
                .build();
    }

    // ── GET /api/v1/rooms/{roomId} 
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room '" + roomId + "' not found"))
                    .build();
        }
        return Response.ok(room).build();
    }

    // ── DELETE /api/v1/rooms/{roomId}
    /**
     * Deletes a room. Idempotent: repeated deletes of a non-existent room
     * return 404 (resource never existed or was already removed).
     *
     * Business rule: a room with active sensors cannot be deleted to prevent
     * orphaned sensor records. Throws RoomNotEmptyException → HTTP 409 Conflict.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room '" + roomId + "' not found"))
                    .build();
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }
        rooms.remove(roomId);
        return Response.noContent().build();
    }
}
