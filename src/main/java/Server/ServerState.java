package Server;

import ClientHandler.ClientHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ServerState {
    private String serverId;
    private String serverAddress;

    private int coordinationPort;
    private int clientsPort;

    private final ConcurrentHashMap<Long, ClientHandler> clientHandlerHashMap = new ConcurrentHashMap<Long, ClientHandler>();
    private final ConcurrentHashMap<String, Room> roomHashMap = new ConcurrentHashMap<String, Room>();

    private static ServerState serverState;

    private ServerState(){
    }

    public static ServerState getServerState() {
        if (serverState == null) {
            synchronized (ServerState.class) {
                if (serverState == null) {
                    serverState = new ServerState();
                }
            }
        }
        return serverState;
    }

    public void initialize(String serverId, String serverConf) {
        this.serverId = serverId;
        try{
            File conf = new File(serverConf);
            Scanner reader = new Scanner(conf);
            while (reader.hasNextLine()){
                String line = reader.nextLine();
                String[] params = line.split("\t");
                if(params[0].equals(serverId)) {
                    this.serverAddress = params[1];
                    this.clientsPort = Integer.parseInt(params[2]);
                    this.coordinationPort = Integer.parseInt(params[3]);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public String getServerId() {
        return serverId;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getClientsPort() {
        return clientsPort;
    }

    public int getCoordinationPort() {
        return coordinationPort;
    }

    public void addClientHandlerThreadToMap(ClientHandler clientHandler) {
        clientHandlerHashMap.put(clientHandler.getId(), clientHandler);
    }

    public void addRoomToMap(Room room) {
        roomHashMap.put(room.getRoomId(), room);
    }

    public ConcurrentHashMap<Long, ClientHandler> getClientHandlerHashMap() {
        return clientHandlerHashMap;
    }


}
