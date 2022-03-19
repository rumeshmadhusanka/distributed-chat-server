package Gossiping;

import Constants.ChatServerConstants.ServerConstants;
import Messaging.Messaging;
import Server.ServerState;
import Utilities.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.concurrent.ConcurrentHashMap;


public class Gossiping {
    private static final Logger logger = LogManager.getLogger(Gossiping.class);

    public static synchronized void receiveHeartBeat(JSONObject request) {
        long receivedTimestamp = Long.parseLong((String) request.get(ServerConstants.TIMESTAMP));
        String serverId = (String) request.get(ServerConstants.SERVER_ID);
        ConcurrentHashMap<String, Long> heartBeatMap = ServerState.getServerState().getHeartbeatMap();
        if (!ServerState.getServerState().getServerId().equals(serverId)){ //if not this server's id
            if (heartBeatMap.containsKey(serverId)) {
                long currentTimeStamp = heartBeatMap.get(serverId);
                if (currentTimeStamp < receivedTimestamp) {
                    heartBeatMap.put(serverId, receivedTimestamp);
                    forwardHeartBeat(request);
                }
            } else {
                logger.debug("Discovered server through Gossiping. Server id: "+ serverId);
                heartBeatMap.put(serverId, receivedTimestamp);
                forwardHeartBeat(request);
            }
        }
    }

    private static void forwardHeartBeat(JSONObject request) {
        Messaging.sendAndForget(request, Util.getRandomServers(ServerState.getServerState().getServers(), 2));
    }

}
