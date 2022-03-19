package Server;

import ClientHandler.ClientHandler;
import Constants.ChatServerConstants;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Room {
    private final String roomId;
    private final String owner;
    private final String serverId;
    private final ConcurrentLinkedQueue<ClientHandler> clientIdentityList = new ConcurrentLinkedQueue<>();

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

    public ConcurrentLinkedQueue<ClientHandler> getClientIdentityList() {
        return clientIdentityList;
    }

    public void addClient(ClientHandler clientHandler) {
        clientIdentityList.add(clientHandler);
    }

    public void removeClient(ClientHandler clientHandler) { clientIdentityList.remove(clientHandler);}
}
