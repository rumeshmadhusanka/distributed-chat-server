package ClientHandler;

import java.net.Socket;

public class Client {

    private final String identity;
    private final Socket clientSocket;
    private String currentRoom;
    private String serverId;

    public Client(String identity, Socket clientSocket) {
        this.identity = identity;
        this.clientSocket = clientSocket;
    }

    public String getIdentity() {
        return identity;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String currentRoom) {
        this.currentRoom = currentRoom;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
}
