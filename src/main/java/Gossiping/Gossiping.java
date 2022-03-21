package Gossiping;

import Constants.ChatServerConstants.ServerConstants;
import Messaging.Messaging;
import Server.Server;
import Server.ServerState;
import Utilities.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


public class Gossiping {
    private static final Logger logger = LogManager.getLogger(Gossiping.class);

    public static synchronized void receiveHeartBeat(JSONObject request) throws IOException {
        long receivedTimestamp = Long.parseLong((String) request.get(ServerConstants.TIMESTAMP));
        String serverId = (String) request.get(ServerConstants.SERVER_ID);
        ConcurrentHashMap<String, Long> heartBeatMap = ServerState.getServerState().getHeartbeatMap();
        if (!ServerState.getServerState().getServerId().equals(serverId)) { //if not this server's id
            if (heartBeatMap.containsKey(serverId)) {
                long currentTimeStamp = heartBeatMap.get(serverId);
                if (currentTimeStamp < receivedTimestamp) {
                    heartBeatMap.put(serverId, receivedTimestamp);
                    forwardHeartBeat(request);
                }
            } else {
                logger.debug("Discovered server through Gossiping. Server id: " + serverId);
                if (ServerState.getServerState().amITheLeader() && failedServerMapContains(serverId)) {
                    HashMap<String, String> serializedServerState = ServerState.getServerState().getCurrentServerState();
                    serializedServerState.put(ServerConstants.TYPE, ServerConstants.KIND_LEADER_STATE);
                    Collection<Server> serverAsACollection = new ArrayList<>();
                    serverAsACollection.add(ServerState.getServerState().getServerFromId(serverId));
                    Messaging.sendAndForget(new JSONObject(serializedServerState), serverAsACollection);
                }
                addServer(serverId, receivedTimestamp);
                forwardHeartBeat(request);
            }
        }
    }

    private static void forwardHeartBeat(JSONObject request) {
        Messaging.sendAndForget(request, Util.getRandomServers(ServerState.getServerState().getServers(), 2));
    }

    public static synchronized void removeServer(String serverId) {
        ServerState.getServerState().getFailedServers().add(serverId);
        ServerState.getServerState().getHeartbeatMap().remove(serverId);
    }

    public static synchronized void addServer(String serverId, long timeStamp) {
        ServerState.getServerState().getFailedServers().remove(serverId);
        ServerState.getServerState().getHeartbeatMap().put(serverId, timeStamp);
    }

    public static boolean failedServerMapContains(String serverId) {
        return ServerState.getServerState().getFailedServers().contains(serverId);
    }

}
