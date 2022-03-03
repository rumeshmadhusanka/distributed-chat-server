package Messaging;

import Server.Server;
import Server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Messaging {

    private static final Logger logger = LogManager.getLogger(Messaging.class);

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
        ConcurrentHashMap<String, Server> servers = ServerState.getServerState().getServerHashmap();
        for (Server server : servers.values()) {
            Socket socket = new Socket(server.getAddress(), server.getPort());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write((obj.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
            dataOutputStream.flush();
            socket.close();
        }

    }

    public static void askServers(JSONObject obj) throws IOException, ParseException {
        ConcurrentHashMap<String, Server> servers = ServerState.getServerState().getServerHashmap();

        for (Server server : servers.values()) {
            Socket socket = new Socket(server.getAddress(), server.getPort());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write((obj.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
            dataOutputStream.flush();

            // TODO: Handle errors
            InputStream inputFromClient = socket.getInputStream();
            Scanner serverInputScanner = new Scanner(inputFromClient, String.valueOf(StandardCharsets.UTF_8));
            String line = serverInputScanner.nextLine();
            logger.debug("Received: " +line);
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonPayload = (JSONObject) jsonParser.parse(line);
            socket.close();
        }

    }
}
