package Server;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Room {
    private final String roomId;
    private final String owner;
    private final String serverId;
    private ConcurrentLinkedQueue<String> clientIdentity;

    public Room(String serverId, String roomId) {
        this.serverId = serverId;
        this.roomId = roomId;
        this.owner = "";
    }

    public Room(String serverId, String roomId, String owner) {
        this.serverId = serverId;
        this.roomId = roomId;
        this.owner = owner;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getOwner() {return owner;}

    public String getServerId() {return serverId;}

    public ConcurrentLinkedQueue<String> getClientIdentity() {
        return clientIdentity;
    }

    public void addClientIdentity(String identity) {
        this.clientIdentity.add(identity);
    }
}
