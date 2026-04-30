package com.smartcampus.exception;

public class RoomNotEmptyException extends RuntimeException {
    private final String roomId;

    public RoomNotEmptyException(String roomId) {
        super("Room '" + roomId + "' cannot be deleted because it still has active sensors assigned.");
        this.roomId = roomId;
    }

    public String getRoomId() { return roomId; }
}
