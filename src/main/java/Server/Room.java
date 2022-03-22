package Server;

import ClientHandler.ClientHandler;
import Utilities.Messaging;
import Utilities.Util;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Room implements Serializable {
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

    public String getOwner() {
        return owner;
    }

    public String getServerId() {
        return serverId;
    }

    public ConcurrentLinkedQueue<ClientHandler> getClientIdentityList() {
        return clientIdentityList;
    }

    public void addClient(ClientHandler clientHandler) {
        clientIdentityList.add(clientHandler);
    }

    public void removeClient(ClientHandler clientHandler) {
        clientIdentityList.remove(clientHandler);
    }

    public void removeClientsFromRoom() {
        // Get clients in the room.
        Collection<ClientHandler> roomClients = getClientIdentityList();

        // Get mainHall from the ServerState.
        Room mainHall = ServerState.getServerState().getMainHall();
        Collection<ClientHandler> mainHallClients = mainHall.getClientIdentityList();
        // Duplicate client list of main hall.
        Collection<ClientHandler> tempRoomClients = new ArrayList<>(mainHallClients);

        // Move all the client in the room to main hall.
        for (ClientHandler client : roomClients) {
            String prevRoom = client.getCurrentRoom();
            mainHall.addClient(client);
            client.setCurrentRoom(mainHall.getRoomId());
            // Send room change request to client.
            JSONObject roomChangeRequest = Util.buildRoomChangeJSON(
                    client.getCurrentIdentity(), getRoomId(), mainHall.getRoomId());
            Messaging.respond(roomChangeRequest, client.getClientSocket());
            // Inform MainHall members about room change of clients.
            Messaging.broadcastClientChangeRoom(tempRoomClients, client.getCurrentIdentity(), prevRoom, mainHall.getRoomId());
        }
        // Update mainHall in ServerState.
        ServerState.getServerState().updateRoom(mainHall);
    }
}
