package com.smartcampus.resource;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Part 1.2 - Discovery / Root Endpoint
 * GET /api/v1
 *
 * Returns API metadata including versioning, contact info, and a HATEOAS-style
 * map of primary resource collections. This self-describing response allows
 * clients to discover all available endpoints without consulting external docs.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("api", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("description", "RESTful API for managing campus rooms and IoT sensors.");
        response.put("contact", Map.of(
                "name", "Smart Campus Admin",
                "email", "admin@smartcampus.ac.uk",
                "module", "5COSC022W - Client-Server Architectures"
        ));
        response.put("resources", Map.of(
                "rooms", "/api/v1/rooms",
                "sensors", "/api/v1/sensors"
        ));
        response.put("links", Map.of(
                "self", "/api/v1",
                "rooms", "/api/v1/rooms",
                "sensors", "/api/v1/sensors"
        ));
        return Response.ok(response).build();
    }
}
