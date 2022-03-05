package Server;

public class Room {
    private String roomId;
    private String owner;
    private String serverId;

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
}
