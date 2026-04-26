package com.smartcampus;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application entry point.
 *
 * The @ApplicationPath annotation sets the versioned base path for the entire API.
 * All resource endpoints will be accessible under /api/v1.
 *
 * JAX-RS Lifecycle Note:
 * By default, JAX-RS creates a new instance of each Resource class per HTTP request
 * (request-scoped). This means each request gets a fresh object with no shared mutable
 * state on the instance itself. To safely share in-memory data (e.g., our HashMap stores)
 * across requests, we use static fields on a dedicated DataStore singleton, which is
 * accessed by all resource instances. Critical sections are protected with
 * ConcurrentHashMap and synchronized blocks to prevent race conditions.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // Jersey auto-discovers resources via package scanning configured in Main.java
}
