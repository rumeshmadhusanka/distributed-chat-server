package Gossiping;

import Constants.ChatServerConstants.ServerConstants;
import Messaging.Messaging;
import Server.ServerState;
import Utilities.Util;
import org.json.simple.JSONObject;

import java.util.concurrent.ConcurrentHashMap;


public class Gossiping {

    public static synchronized void receiveHeartBeat(JSONObject request) {
        long receivedTimestamp = Long.parseLong((String) request.get(ServerConstants.TIMESTAMP));
        String serverId = (String) request.get(ServerConstants.SERVER_ID);
        ConcurrentHashMap<String, Long> heartBeatMap = ServerState.getServerState().getHeartbeatMap();
        if (heartBeatMap.containsKey(serverId)) {
            long currentTimeStamp = heartBeatMap.get(serverId);
            if (currentTimeStamp < receivedTimestamp) {
                heartBeatMap.put(serverId, receivedTimestamp);
                forwardHeartBeat(request);
            }
        } else {
            heartBeatMap.put(serverId, receivedTimestamp);
            forwardHeartBeat(request);
        }
    }

    private static void forwardHeartBeat(JSONObject request) {
        Messaging.sendAndForget(request, Util.getRandomServers(ServerState.getServerState().getServers(), 2));
    }

}
