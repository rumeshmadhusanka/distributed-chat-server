package Server;

import ClientHandler.ClientHandler;
import Consensus.Leader;
import Consensus.LeaderElection;
import Constants.ChatServerConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerState {

    private static final Logger logger = LogManager.getLogger(ServerState.class);
    private static ServerState serverState;
    private final ConcurrentHashMap<Long, ClientHandler> clientHandlerHashMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Room> roomsHashMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Server> serversHashmap = new ConcurrentHashMap<>(); // has all the Servers; dead and alive; except this
    private final ConcurrentLinkedQueue<String> identityList = new ConcurrentLinkedQueue<>(); // unique client identifies
    private String serverId;
    private String serverAddress;
    private int coordinationPort;
    private int clientsPort;
    private Leader currentLeader = null;

    private ServerState() {
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

    public static String getServerIdFromName(String serverName) {
        // server name:  s1
        // server id: 1
        return serverName.substring(1);
    }

    public void initialize(String serverId, String serverConf) {
        serverId = getServerIdFromName(serverId);
        this.serverId = serverId;
        try {
            File conf = new File(serverConf);
            Scanner reader = new Scanner(conf);
            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                String[] params = line.split("\t");
                params[0] = getServerIdFromName(params[0]);
                if (params[0].equals(serverId)) {
                    this.serverAddress = params[1];
                    this.clientsPort = Integer.parseInt(params[2]);
                    this.coordinationPort = Integer.parseInt(params[3]);
                    logger.trace("Server created: " + serverId + " " + serverAddress + " " + clientsPort + " " + coordinationPort);
                } else {
                    Server server = new Server(params[0], params[1], Integer.parseInt(params[3]));
                    serversHashmap.put(server.getId(), server);
                }
                addRoomToMap(new Room(serverId, ChatServerConstants.ServerConstants.MAIN_HALL + "-" + serverId));
            }
            //create main hall chatroom


            //TODO remove hardcoded Leader value
            LeaderElection.startElection();
//            this.currentLeader = new Leader("1", "0.0.0.0", 5555);

        } catch (FileNotFoundException e) {
            logger.debug(e.getMessage());
        }

    }

    public Server getServerFromId(String serverId) {
        Collection<Server> servers = getServers();
        for (Server s : servers) {
            if (s.getId().equals(serverId)) {
                return s;
            }
        }
        if (serverId.equals(this.serverId)) {
            return new Server(this.serverId, this.serverAddress, this.coordinationPort); //todo check coordination port or clients port
        }
        return null;
    }

    public String getServerId() {
        return serverId;
    }

    public ConcurrentHashMap<String, Room> getRoomsHashMap() {
        return roomsHashMap;
    }

    public Room getRoom(String roomId) {
        return roomsHashMap.get(roomId);
    }

    public void updateRoom(Room room) {
        String roomId = room.getRoomId();
        if (roomsHashMap.containsKey(roomId)) {
            roomsHashMap.put(roomId, room);
        }
    }

    public ConcurrentLinkedQueue<ClientHandler> getClientsInRoom(String roomId) {
        Room room = roomsHashMap.get(roomId);
        return room.getClientIdentityList();
    }

    public Room getMainHall() {
        return roomsHashMap.get(ChatServerConstants.ServerConstants.MAIN_HALL + "-" + serverId);
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


    public Collection<Server> getServers() {
        return serversHashmap.values();
    }

    public Collection<Server> getServersHigherThanMyId() {
        Collection<Server> higherServers = new ArrayList<>();
        for (Server server : getServers()) {
            if (Integer.parseInt(server.getId()) > Integer.parseInt(getServerId())) {
                higherServers.add(server);
            }
        }
        return higherServers;
    }

    public void addRoomToMap(Room room) {
        roomsHashMap.put(room.getRoomId(), room);
    }

    public String getRoomByOwner(String owner) {
        for (Map.Entry<String, Room> mapEntry : roomsHashMap.entrySet()) {
            if (owner.equals(mapEntry.getValue().getOwner())) {
                return mapEntry.getValue().getRoomId();
            }
        }
        return null;
    }

    public void removeRoom(Room room) {
        roomsHashMap.remove(room.getRoomId());
    }

    public boolean hasRoomId(String identity) {
        return roomsHashMap.containsKey(identity);
    }

    public Leader getCurrentLeader() {
        return currentLeader;
    }

    public synchronized void setCurrentLeader(Leader currentLeader) {
        this.currentLeader = currentLeader;
    }

    public void addIdentity(String identity) {
        this.identityList.add(identity);
    }

    public ConcurrentLinkedQueue<String> getIdentityList() {
        return identityList;
    }

    public boolean hasIdentity(String identity) {
        return identityList.contains(identity);
    }
}
