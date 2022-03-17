package Gossiping;

import Messaging.Messaging;
import Server.Server;
import Server.ServerState;
import Constants.ChatServerConstants.ServerConstants;
import org.json.simple.JSONObject;

import java.util.*;

public class HeartBeatSender extends TimerTask {

    private static JSONObject buildHeartBeatMessage(long timestamp) {
        HashMap<String, String> requestMap = new HashMap<>();
        requestMap.put(ServerConstants.TYPE, ServerConstants.TYPE_GOSSIP);
        requestMap.put(ServerConstants.KIND, ServerConstants.KIND_HEARTBEAT);
        requestMap.put(ServerConstants.TIMESTAMP, Long.toString(timestamp));
        requestMap.put(ServerConstants.SERVER_ID, ServerState.getServerState().getServerId());
        return new JSONObject(requestMap);
    }

    private static Collection<Server> getRandomServes(Collection<Server> servers, int n) {
        ArrayList<Server> serversList = new ArrayList<>(servers);
        Random random = new Random();
        Collection<Server> randomServers = new ArrayList<>();
        //select servers randomly
        for (int i = 0; i < n; i++) {
            randomServers.add(serversList.get(random.nextInt(serversList.size())));
        }
        return randomServers;
    }

    @Override
    public void run() {
        //increase my heartbeat timestamp
        ServerState.getServerState().setMyHeartBeat(System.currentTimeMillis());
        Collection<Server> randomServers = getRandomServes(ServerState.getServerState().getServers(), 2);
        Messaging.sendAndForget(buildHeartBeatMessage(ServerState.getServerState().getMyHeartBeat()), randomServers);

    }
}
