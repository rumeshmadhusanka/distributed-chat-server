package Server;

import ClientHandler.ClientHandler;
import Consensus.Consensus;
import Constants.ChatServerConstants.BullyConstants;
import Constants.ChatServerConstants.ServerConstants;
import Messaging.Messaging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Scanner;

public class ServerHandler extends Thread {
    //TODO: Implement Server-server communication (Similar to client-server)

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    private final Socket serverSocket;

    public ServerHandler(Socket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        try {
            InputStream inputFromClient = serverSocket.getInputStream();
            Scanner serverInputScanner = new Scanner(inputFromClient, String.valueOf(StandardCharsets.UTF_8));
            String line = serverInputScanner.nextLine();
            logger.debug("Received: " + line);
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonPayload = (JSONObject) jsonParser.parse(line);
            resolveServerRequest(jsonPayload, serverSocket);
        } catch (IOException | ParseException e) {
            logger.debug(e);
        }
    }

    private void resolveServerRequest(JSONObject jsonPayload, Socket serverSocket) throws IOException, ParseException {
        String type = (String) jsonPayload.get(ServerConstants.TYPE);
        String kind = (String) jsonPayload.get(ServerConstants.KIND);

        switch (type) {
            case ServerConstants.TYPE_CONSENSUS:
                switch (kind) {
                    case ServerConstants.KIND_VERIFY_UNIQUE:
                        String value;
                        String valueType;
                        if (jsonPayload.containsKey(ServerConstants.IDENTITY)) {
                            value = String.valueOf(jsonPayload.get(ServerConstants.IDENTITY));
                            valueType = ServerConstants.IDENTITY;
                        } else {
                            value = String.valueOf(jsonPayload.get(ServerConstants.ROOM_ID));
                            valueType = ServerConstants.ROOM_ID;
                        }

                        boolean isAvailable = Consensus.verifyUniqueValue(value, valueType);
                        HashMap<String, String> responseMap = new HashMap<>();
                        responseMap.put(ServerConstants.TYPE, ServerConstants.TYPE_CONSENSUS);
                        responseMap.put(ServerConstants.KIND, ServerConstants.KIND_VERIFY_UNIQUE);
                        responseMap.put(valueType, value);
                        responseMap.put(ServerConstants.UNIQUE, String.valueOf(isAvailable));
                        Messaging.respond(new JSONObject(responseMap), serverSocket);
                    case ServerConstants.KIND_REQUEST_TO_CREATE_NEW_IDENTITY:
                        String identity = String.valueOf(jsonPayload.get(ServerConstants.IDENTITY));
                        boolean isIdAvail = Consensus.verifyUniqueValue(identity, ServerConstants.IDENTITY);
                        // TODO: Respond

                    case ServerConstants.KIND_REQUEST_TO_CREATE_NEW_ROOM:
                        String roomId = String.valueOf(jsonPayload.get(ServerConstants.IDENTITY));
                        boolean isRoomAvail = Consensus.verifyUniqueValue(roomId, ServerConstants.IDENTITY);
                        // TODO: Respond
                }
            case ServerConstants.TYPE_GOSSIP:
                switch (kind) {
                    case ServerConstants.KIND_INFORM_NEW_IDENTITY:
                        // TODO: Inform servers
                    case ServerConstants.KIND_INFORM_DELETE_IDENTITY:
                        // TODO: Inform servers
                    case ServerConstants.KIND_INFORM_NEW_ROOM:
                        // TODO: Inform servers
                    case ServerConstants.KIND_INFORM_DELETE_ROOM:
                        // TODO: Inform servers
                }
            case BullyConstants.TYPE_BULLY:
                switch (kind) {
                    case BullyConstants.KIND_ELECTION:
                        // TODO:
                    case BullyConstants.KIND_OK:
                        // TODO:
                    case BullyConstants.KIND_ELECTED:
                        // TODO:
                    case BullyConstants.KIND_COORDINATOR:
                        // TODO:
                }
        }
    }
}
