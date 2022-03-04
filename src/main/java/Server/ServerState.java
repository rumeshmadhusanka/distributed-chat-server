package Server;

import ClientHandler.ClientHandler;
import Consensus.Leader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerState {

    private static final Logger logger = LogManager.getLogger(ServerState.class);

    private String serverId;
    private String serverAddress;

    private int coordinationPort;
    private int clientsPort;

    private String serverConfFilePath;

    private final ConcurrentHashMap<Long, ClientHandler> clientHandlerHashMap = new ConcurrentHashMap<Long, ClientHandler>();
    private final ConcurrentHashMap<String, Room> roomsHashMap = new ConcurrentHashMap<String, Room>();
    private final ConcurrentHashMap<String, Server> serversHashmap = new ConcurrentHashMap<String, Server>();
    private final ConcurrentLinkedQueue<String> identityList = new ConcurrentLinkedQueue<String>();

    private Leader currentLeader;

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
        this.serverConfFilePath = serverConf;
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
                else {
                    Server server = new Server(params[0], params[1], Integer.parseInt(params[3]));
                    serversHashmap.put(server.getId(), server);
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

    public void addClientHandler(ClientHandler clientHandler) {
        clientHandlerHashMap.put(clientHandler.getId(), clientHandler);
    }

    public ConcurrentHashMap<Long, ClientHandler> getClientHandlerHashMap() {
        return clientHandlerHashMap;
    }

//    public void addServers() {
//        try{
//            File conf = new File(serverConfFilePath);
//            Scanner reader = new Scanner(conf);
//            while (reader.hasNextLine()){
//                String line = reader.nextLine();
//                String[] params = line.split("\t");
//                Server server = new Server(params[0], params[1], Integer.parseInt(params[3]));
//                serversHashmap.put(server.getId(), server);
//            }
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//    }

    public Collection<Server> getServers() {
        return serversHashmap.values();
    }

    public void addRoomToMap(Room room) {
        roomsHashMap.put(room.getRoomId(), room);
    }

    public String getRoomByOwner(String owner) {
        for (Map.Entry<String, Room> mapEntry: roomsHashMap.entrySet()) {
            if(owner.equals(mapEntry.getValue().getOwner())){
                return mapEntry.getValue().getRoomId();
            }
        }
        return null;
    }

    public void removeRoom(Room room) {roomsHashMap.remove(room.getRoomId());}

    public Leader getCurrentLeader() {
        return currentLeader;
    }

    public void setCurrentLeader(Leader currentLeader) {
        this.currentLeader = currentLeader;
    }

    public void addIdentity(String identity){
        this.identityList.add(identity);
    }

    public ConcurrentLinkedQueue<String> getIdentityList() {
        return identityList;
    }
}
