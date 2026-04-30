package com.smartcampus.exception;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.smartcampus.model.ApiError;

public class ExceptionMappers {

    // ─────────────────────────────────────────────────────────────
    // 409 Conflict – Room cannot be deleted while sensors are assigned
    // ─────────────────────────────────────────────────────────────
    @Provider
    public static class RoomNotEmptyExceptionMapper
            implements ExceptionMapper<RoomNotEmptyException> {

        @Override
        public Response toResponse(RoomNotEmptyException ex) {
            ApiError error = new ApiError(
                    409,
                    "Conflict",
                    ex.getMessage()
            );
            return Response.status(Response.Status.CONFLICT)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(error)
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 422 Unprocessable Entity – roomId references non-existent room
    // ─────────────────────────────────────────────────────────────
    @Provider
    public static class LinkedResourceNotFoundExceptionMapper
            implements ExceptionMapper<LinkedResourceNotFoundException> {

        @Override
        public Response toResponse(LinkedResourceNotFoundException ex) {
            ApiError error = new ApiError(
                    422,
                    "Unprocessable Entity",
                    ex.getMessage()
            );
            return Response.status(422)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(error)
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 403 Forbidden – Sensor in MAINTENANCE/OFFLINE
    // ─────────────────────────────────────────────────────────────
    @Provider
    public static class SensorUnavailableExceptionMapper
            implements ExceptionMapper<SensorUnavailableException> {

        @Override
        public Response toResponse(SensorUnavailableException ex) {
            ApiError error = new ApiError(
                    403,
                    "Forbidden",
                    ex.getMessage()
            );
            return Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(error)
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 404 Not Found – JAX-RS built-in NotFoundException
    // ─────────────────────────────────────────────────────────────
    @Provider
    public static class NotFoundExceptionMapper
            implements ExceptionMapper<NotFoundException> {

        @Override
        public Response toResponse(NotFoundException ex) {
            String message = (ex.getMessage() != null)
                    ? ex.getMessage()
                    : "The requested resource was not found.";

            ApiError error = new ApiError(
                    404,
                    "Not Found",
                    message
            );

            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(error)
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 500 Internal Server Error – Global safety net
    // ─────────────────────────────────────────────────────────────
    @Provider
    public static class GlobalExceptionMapper
            implements ExceptionMapper<Throwable> {

        private static final Logger LOGGER =
                Logger.getLogger(GlobalExceptionMapper.class.getName());

        @Override
        public Response toResponse(Throwable ex) {
            LOGGER.log(Level.SEVERE,
                    "Unhandled exception caught by global safety net", ex);

            ApiError error = new ApiError(
                    500,
                    "Internal Server Error",
                    "An unexpected error occurred. Please contact the API administrator."
            );

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(error)
                    .build();
        }
    }
}