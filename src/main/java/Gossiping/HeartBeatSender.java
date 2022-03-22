package Gossiping;

import Constants.ChatServerConstants.ServerConstants;
import Server.Server;
import Server.ServerState;
import Utilities.Messaging;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.TimerTask;

import static Utilities.Util.getRandomServers;

public class HeartBeatSender extends TimerTask {

    private static JSONObject buildHeartBeatMessage(long timestamp) {
        HashMap<String, String> requestMap = new HashMap<>();
        requestMap.put(ServerConstants.TYPE, ServerConstants.TYPE_GOSSIP);
        requestMap.put(ServerConstants.KIND, ServerConstants.KIND_HEARTBEAT);
        requestMap.put(ServerConstants.TIMESTAMP, Long.toString(timestamp));
        requestMap.put(ServerConstants.SERVER_ID, ServerState.getServerState().getServerId());
        return new JSONObject(requestMap);
    }

    @Override
    public void run() {
        //increase my heartbeat timestamp
        ServerState.getServerState().setMyHeartBeat(System.currentTimeMillis());
        Collection<Server> randomServers = getRandomServers(ServerState.getServerState().getServers(), 2);
        Messaging.sendAndForget(buildHeartBeatMessage(ServerState.getServerState().getMyHeartBeat()), randomServers);

    }
}
