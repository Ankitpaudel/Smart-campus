package com.smartcampus.exception;

public class LinkedResourceNotFoundException extends RuntimeException {
    private final String referencedId;
    private final String resourceType;

    public LinkedResourceNotFoundException(String resourceType, String referencedId) {
        super("The referenced " + resourceType + " with id '" + referencedId + "' does not exist in the system.");
        this.resourceType = resourceType;
        this.referencedId = referencedId;
    }

    public String getReferencedId() { return referencedId; }
    public String getResourceType() { return resourceType; }
}
