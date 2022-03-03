package Server;

public class Room {
    private String roomId;
    private String owner;

    public Room(String roomId) {
        this.roomId = roomId;
        this.owner = "";
    }

    public Room(String roomId, String owner) {
        this.roomId = roomId;
        this.owner = owner;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getOwner() {return owner;}
}
