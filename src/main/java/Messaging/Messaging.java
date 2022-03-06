package Messaging;

import Server.Server;
import Server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import Constants.ServerProperties;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.*;


public class Messaging {

    private static final Logger logger = LogManager.getLogger(Messaging.class);

    /**
     * JSON parse a given string.
     *
     * @param jsonString - String containing json object
     * @return - JSONObject
     * @throws ParseException
     */
    public static JSONObject jsonParseRequest(String jsonString) throws ParseException {

        JSONParser jsonParser = new JSONParser();
        return (JSONObject) jsonParser.parse(jsonString);
    }

    /**
     * Send a given json response via a given socket.
     *
     * @param obj - Response as a json object
     * @param socket - Socket of the receiver.
     * @throws IOException
     */
    public static void respond(JSONObject obj, Socket socket) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((obj.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }

    /**
     * Broadcast a json to all connected clients.
     *
     * @param obj - JSONObject to be sent to clients.
     * @throws IOException
     */
    public static void broadcastClients(JSONObject obj) throws IOException {
        //TODO: Need to handle this in the same way broadcastServers is implemented.
//        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
//        dataOutputStream.write((obj.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
//        dataOutputStream.flush();
    }

    /**
     * Broadcast a json to all servers in the cluster.
     *
     * @param obj - JSONObject to be sent to servers.
     * @throws IOException
     */
    public static void broadcastServers(JSONObject obj) throws IOException {
        Collection<Server> servers = ServerState.getServerState().getServers();
        for (Server server : servers) {
            Socket socket = new Socket(server.getAddress(), server.getPort());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write((obj.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
            dataOutputStream.flush();
            socket.close();
        }
    }

    /**
     * Only executed by the leader.
     * Asks something from each server in the servers Collection
     *
     * @param request - request Json.
     * @param servers - List of servers.
     * @return - Responses Map.
     */
    public static ConcurrentHashMap<String, JSONObject> askServers(JSONObject request, Collection<Server> servers) {
        ConcurrentHashMap<String, JSONObject> serverResponses = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(ServerProperties.THREAD_COUNT);
        for (Server server : servers) {
            executorService.submit(() -> {
                try {
                    Socket socket = new Socket(server.getAddress(), server.getPort());
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.write((request.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
                    dataOutputStream.flush();

                    InputStream inputFromClient = socket.getInputStream();
                    Scanner serverInputScanner = new Scanner(inputFromClient, String.valueOf(StandardCharsets.UTF_8));
                    String line = serverInputScanner.nextLine();
                    logger.debug("Received: " + line);
                    serverResponses.put(server.getId(), jsonParseRequest(line));
                    socket.close();
                } catch (Exception e) {
                    logger.info("Connection failed for server: " + server.getAddress() + ":" + server.getPort());
                    logger.debug(e.getMessage());
                }
            });
        }
        executorService.shutdown();
        try {
            //wait till completion or 5s or interruption of this thread
            if (!executorService.awaitTermination(ServerProperties.CONN_TIMEOUT, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        return serverResponses;
    }
}
