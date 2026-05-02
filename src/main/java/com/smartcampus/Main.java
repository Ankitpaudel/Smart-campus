package com.smartcampus;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.smartcampus.exception.ExceptionMappers;
import com.smartcampus.filter.ApiLoggingFilter;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static final String BASE_URI = "http://0.0.0.0:8080/api/v1/";

    public static HttpServer startServer() {
        final ResourceConfig rc = new ResourceConfig()
                .register(DiscoveryResource.class)
                .register(RoomResource.class)
                .register(SensorResource.class)
                .register(ApiLoggingFilter.class)
                .register(ExceptionMappers.RoomNotEmptyExceptionMapper.class)
                .register(ExceptionMappers.LinkedResourceNotFoundExceptionMapper.class)
                .register(ExceptionMappers.SensorUnavailableExceptionMapper.class)
                .register(ExceptionMappers.NotFoundExceptionMapper.class)
                .register(ExceptionMappers.GlobalExceptionMapper.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();
        LOGGER.info("Smart Campus API started at: http://localhost:8080/api/v1");
        LOGGER.info("Press ENTER to stop the server...");
        System.in.read();
        server.stop();
    }
}