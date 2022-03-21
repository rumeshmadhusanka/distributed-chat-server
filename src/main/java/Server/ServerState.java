package Server;

import ClientHandler.ClientHandler;
import Consensus.Leader;
import Constants.ChatServerConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerState {

    private static final Logger logger = LogManager.getLogger(ServerState.class);
    private static ServerState serverState;
    private final ConcurrentHashMap<Long, ClientHandler> clientHandlerHashMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Room> roomsHashMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Server> serversHashmap = new ConcurrentHashMap<>(); // has all the Servers; dead and alive; except this
    // TODO: Change identityList into a HashMap to keep the serverIds.
    private final ConcurrentLinkedQueue<String> identityList = new ConcurrentLinkedQueue<>(); // unique client identifies
    private final ConcurrentHashMap<String, Long> heartBeatMap = new ConcurrentHashMap<>(); //store heartbeats of servers
    private final ConcurrentLinkedQueue<String> failedServers = new ConcurrentLinkedQueue<>(); // store failed servers
    private boolean smallPartitionFormed = false;
    private String serverId;
    private String serverAddress;
    private int coordinationPort;
    private int clientsPort;
    private long myHeartBeat = 0;
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
                    Server server = new Server(params[0], params[1], Integer.parseInt(params[3]), Integer.parseInt(params[2]));
                    serversHashmap.put(server.getId(), server);
                }
                String mainHallId = ChatServerConstants.ServerConstants.MAIN_HALL + serverId;
                addRoomToMap(new Room(serverId, mainHallId));
            }
            //create main hall chatroom

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
            return new Server(this.serverId, this.serverAddress, this.coordinationPort, this.clientsPort); //todo check coordination port or clients port
        }
        return null;
    }

    public String getServerId() {
        return serverId;
    }

    public Enumeration<String> getRoomsIds() {
        return roomsHashMap.keys();
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
        return roomsHashMap.get(ChatServerConstants.ServerConstants.MAIN_HALL + serverId);
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

    public void removeClientHandler(ClientHandler clientHandler) {
        clientHandlerHashMap.remove(clientHandler.getId(), clientHandler);
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

    public void deleteIdentity(String identity) {
        identityList.remove(identity);
    }

    public ConcurrentHashMap<String, Long> getHeartbeatMap() {
        return heartBeatMap;
    }

    public long getMyHeartBeat() {
        return myHeartBeat;
    }

    public void setMyHeartBeat(long myHeartBeat) {
        this.myHeartBeat = myHeartBeat;
    }

    public Collection<Server> getActiveServers() {
        // active servers are maintained at the heartbeat map
        ArrayList<String> serverIds = new ArrayList<>(getHeartbeatMap().keySet());
        Collection<Server> output = new ArrayList<>();
        for (Server server : getServers()) {
            for (String id : serverIds) {
                if (server.getId().equals(id)) {
                    output.add(server);
                }
            }
        }
        return output;
    }

    public ConcurrentLinkedQueue<String> getFailedServers() {
        return failedServers;
    }

    public boolean isSmallPartitionFormed() {
        return getServerState().smallPartitionFormed;
    }

    public void setSmallPartitionFormed(boolean smallPartitionFormed) {
        getServerState().smallPartitionFormed = smallPartitionFormed;
    }

    public boolean amITheLeader() {
        if (currentLeader != null) {
            return serverId.equals(currentLeader.getId());
        }
        return false;
    }

    /**
     * Returns the current ServerState.
     *
     * @return - Current ServerState containing identities and rooms.
     * @throws IOException
     */
    public HashMap<String, String> getCurrentServerState() throws IOException {
        HashMap<String, String> serverState = new HashMap<>();

        // TODO: change the code to reflect the datatype change in identityList.
        // Serialize identity list.
        serverState.put("IdentityList", serialize(new ArrayList<>(identityList)));

        // Rooms belonging to current server contains the ClientHandler list. Need to remove that before serializing.
        ArrayList<Room> tempRooms = new ArrayList<>();
        for (Room room : roomsHashMap.values()) {
            tempRooms.add(new Room(room.getServerId(), room.getRoomId(), room.getOwner()));
        }
        serverState.put("RoomList", serialize(tempRooms));

        return serverState;
    }

    /**
     * Restores the ServerState using a jsonObject sent by the leader.
     *
     * @param jsonObject - JSONObject received from the leader.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void restoreServerState(JSONObject jsonObject) throws IOException, ClassNotFoundException {

        logger.info("Restoring ServerState using data sent by the leader.");
        String identityString = (String) jsonObject.get("IdentityList");
        String roomString = (String) jsonObject.get("RoomList");

        Collection<String> tempIdList = (Collection<String>) deserialize(identityString);
        if (identityList.isEmpty()) {
            identityList.addAll(tempIdList);
        }

        ArrayList<Room> tempRoomList = (ArrayList<Room>) deserialize(roomString);
        for (Room room : tempRoomList) {
            // Prevent MainHall duplicates.
            if (!roomsHashMap.containsKey(room.getRoomId())) {
                roomsHashMap.put(room.getRoomId(), room);
            }
        }

        setSmallPartitionFormed(false);
    }


    /**
     * Serialize a given object.
     *
     * @param obj - Object
     * @return - Serialized object.
     * @throws IOException
     */
    private String serialize(Serializable obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Deserialize a given string.
     *
     * @param objectString - Serialized string.
     * @return - Deserialized object.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static Object deserialize(String objectString) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(objectString);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object obj = ois.readObject();
        ois.close();
        return obj;
    }

    /**
     * Purge the ServerState when a partition is formed.
     *
     * @throws IOException
     */
    public void purgeServerState() throws IOException {
        if (smallPartitionFormed) {
            logger.info("Purging ServerState due to formation of a small partition.");
            currentLeader = null;
            disconnectClients();
            clientHandlerHashMap.clear();
            roomsHashMap.clear();
            serversHashmap.clear();
            identityList.clear();
            heartBeatMap.clear();
            failedServers.clear();
            // Confirm resetting heartbeat
            myHeartBeat = 0;
        }
    }

    /**
     * Disconnect all the client present in the clientHandlerHashMap.
     *
     * @throws IOException
     */
    private void disconnectClients() throws IOException {

        //TODO:
        //Closing the socket here might throw an SocketException in either ClientHandler or main.
        //Need to handle that scenario after testing.

        for (ClientHandler clientHandler : clientHandlerHashMap.values()) {
            clientHandler.getClientSocket().close();
        }
    }

    public void addMainHallOfDetectedServer(String sId) {
        String mainHallId = ChatServerConstants.ServerConstants.MAIN_HALL + sId;
        roomsHashMap.put(mainHallId, new Room(sId, mainHallId));
    }
}
