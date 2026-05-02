package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Cross-cutting API observability filter.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter in a single
 * class to capture the full request/response lifecycle for every API call.
 *
 * By using a JAX-RS filter rather than inserting Logger.info() into every resource
 * method, we achieve a clean separation of concerns: business logic stays in resource
 * classes, and infrastructure concerns (logging, auth, CORS) live here. This also
 * guarantees consistent logging coverage even if a developer forgets to add logging
 * to a new endpoint.
 */
@Provider
public class ApiLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(ApiLoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String uri = requestContext.getUriInfo().getRequestUri().toString();
        LOGGER.info(String.format("[REQUEST]  %s %s", method, uri));
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        int status = responseContext.getStatus();
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();
        LOGGER.info(String.format("[RESPONSE] %s %s -> HTTP %d", method, path, status));
    }
}
