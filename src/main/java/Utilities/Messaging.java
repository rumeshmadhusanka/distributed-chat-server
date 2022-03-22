package Utilities;

import ClientHandler.ClientHandler;
import Consensus.Leader;
import Constants.ChatServerConstants;
import Constants.ServerProperties;
import Exception.ServerException;
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
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


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
     * @param obj    - Response as a json object
     * @param socket - Socket of the receiver.
     */
    public static void respond(JSONObject obj, Socket socket) {
        try {
            logger.debug("Sending: " + obj.toJSONString() + " to :" + socket.getLocalPort());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write((obj.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
            dataOutputStream.flush();
        } catch (IOException exception) {
            logger.debug("Socket closed for: " + socket.getInetAddress());
        }
    }

    /**
     * Only executed by the leader.
     * Asks something from each server in the servers Collection, then get a reply.
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
                    String line = sendRequest(request, socket);
                    serverResponses.put(server.getId(), jsonParseRequest(line));
                    socket.close();
                } catch (Exception e) {
                    logger.debug("Connection failed for server: " + server.getAddress() + ":" + server.getPort() + " msg: " + request.toJSONString());
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

    /**
     * Send and forget a message. Unidirectional; Doesn't wait for the other party to respond.
     *
     * @param request JSON request to send
     * @param servers Collection of servers to send the message
     */
    public static void sendAndForget(JSONObject request, Collection<Server> servers) {
        ExecutorService executorService = Executors.newFixedThreadPool(ServerProperties.THREAD_COUNT);
        for (Server server : servers) {
            executorService.submit(() -> {
                try {
                    Socket socket = new Socket(server.getAddress(), server.getPort());
                    sendOnly(request, socket);
                } catch (Exception e) {
                    logger.trace("Connection failed for server: " + server.getAddress() + ":" + server.getPort() + " msg: " + request.toJSONString());
                }
            });
        }
        executorService.shutdown();
    }

    public static JSONObject contactLeader(JSONObject request, Leader leader) throws ServerException, IOException, ParseException {

        try {
            logger.debug("Sending request: " + request.toJSONString());
            Socket socket = new Socket(leader.getAddress(), leader.getPort());
            socket.setSoTimeout((int) ServerProperties.CONN_TIMEOUT);
            String line = sendRequest(request, socket);
            socket.close();
            if (line != null) {
                return jsonParseRequest(line);
            } else {
                logger.debug("No response received from the leader.");
                throw new ServerException(
                        ChatServerConstants.ServerExceptionConstants.LEADER_FAILED_MSG,
                        ChatServerConstants.ServerExceptionConstants.LEADER_FAILED_CODE);
            }
        } catch (SocketTimeoutException | ConnectException e) {
            // Throw an error if connection to leader fails.
            logger.info("Connection to leader timed out.");
            throw new ServerException(
                    ChatServerConstants.ServerExceptionConstants.LEADER_FAILED_MSG,
                    ChatServerConstants.ServerExceptionConstants.LEADER_FAILED_CODE);
        }
    }

    private static String sendRequest(JSONObject request, Socket socket) throws IOException {
        try {
            logger.debug("Sending request: " + request.toJSONString());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write((request.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
            dataOutputStream.flush();

            InputStream inputFromClient = socket.getInputStream();
            Scanner serverInputScanner = new Scanner(inputFromClient, String.valueOf(StandardCharsets.UTF_8));
            String line = serverInputScanner.nextLine();
            logger.debug("Received -: " + line);
            return line;
        } catch (NoSuchElementException | IllegalStateException e) {
            return null;
        }
    }

    private static void sendOnly(JSONObject request, Socket socket) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((request.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
        dataOutputStream.flush();
        socket.close();
    }

    /**
     * Inform a set of clients.
     *
     * @param clients Collection clients.
     * @param roomId  New joining room id.
     */
    public static void broadcastClientChangeRoom(Collection<ClientHandler> clients, String clientIdentity, String prevRoom, String roomId) {
        for (ClientHandler client : clients) {
            if (client.getCurrentRoom().equals(roomId)) {
                logger.debug("Informing about room change of " + clientIdentity + " to " + client.getCurrentIdentity());
                JSONObject roomChangeRequest = Util.buildRoomChangeJSON(clientIdentity, prevRoom, roomId);
                respond(roomChangeRequest, client.getClientSocket());
            }
        }
    }

    /**
     * Inform other servers about client moving into a new server.
     *
     * @param identity - Identity.
     */
    public static void informServersAboutIdentityMove(String identity) {
        HashMap<String, String> request = new HashMap<>();
        request.put(ChatServerConstants.ServerConstants.TYPE, ChatServerConstants.ServerConstants.IDENTITY_SERVER_CHANGE);
        request.put(ChatServerConstants.ServerConstants.SERVER_ID, ServerState.getServerState().getServerId());
        request.put(ChatServerConstants.ServerConstants.IDENTITY, identity);
        Collection<Server> servers = ServerState.getServerState().getServers();
        sendAndForget(new JSONObject(request), servers);
    }

    /**
     * Inform servers about identity creation deletion.
     *
     * @param kind     - Kind.
     * @param identity - Identity.
     */
    public static void informServersIdentity(String kind, String identity) {
        HashMap<String, String> request = new HashMap<>();
        request.put(ChatServerConstants.ServerConstants.TYPE, ChatServerConstants.ServerConstants.TYPE_GOSSIP);
        request.put(ChatServerConstants.ServerConstants.KIND, kind);
        request.put(ChatServerConstants.ServerConstants.SERVER_ID, ServerState.getServerState().getServerId());
        request.put(ChatServerConstants.ServerConstants.IDENTITY, identity);
        Collection<Server> servers = ServerState.getServerState().getServers();
        sendAndForget(new JSONObject(request), servers);
    }

    /**
     * Inform servers about room creation/ deletion.
     *
     * @param kind   - Kind.
     * @param roomId - Room Id.
     * @param owner
     */
    public static void informServersRoom(String kind, String roomId, String owner) {
        HashMap<String, String> request = new HashMap<>();
        request.put(ChatServerConstants.ServerConstants.TYPE, ChatServerConstants.ServerConstants.TYPE_GOSSIP);
        request.put(ChatServerConstants.ServerConstants.KIND, kind);
        request.put(ChatServerConstants.ServerConstants.SERVER_ID, ServerState.getServerState().getServerId());
        request.put(ChatServerConstants.ServerConstants.ROOM_ID, roomId);
        request.put(ChatServerConstants.ServerConstants.ROOM_OWNER, owner);
        Collection<Server> servers = ServerState.getServerState().getServers();
        sendAndForget(new JSONObject(request), servers);
    }
}
