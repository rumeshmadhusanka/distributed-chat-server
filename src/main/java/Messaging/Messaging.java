package Messaging;

import Server.Server;
import Server.ServerState;
import org.json.simple.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class Messaging {

    public static void respondClient(JSONObject obj, Socket socket) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((obj.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }

    public static void broadcastClients(JSONObject obj) throws IOException {
        //TODO: Need to handle this in the same way broadcastServers is implemented.
//        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
//        dataOutputStream.write((obj.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
//        dataOutputStream.flush();
    }

    public static void broadcastServers(JSONObject obj) throws IOException {
        //TODO: Should we get the current socket list as a parameter instead of creating new sockets?
        ConcurrentHashMap<String, Server> servers = ServerState.getServerState().getServerHashmap();
        for (Server server : servers.values()) {
            Socket socket = new Socket(server.getAddress(), server.getPort());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write((obj.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
            dataOutputStream.flush();
        }

    }
}
