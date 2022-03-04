package Server;

import ClientHandler.ClientHandler;
import Constants.ChatServerConstants;
import Messaging.Messaging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ServerHandler extends Thread {
    //TODO: Implement Server-server communication (Similar to client-server)

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    private final Socket serverCoordinationSocket;

    public ServerHandler(Socket serverCoordinationSocket) {
        this.serverCoordinationSocket = serverCoordinationSocket;
    }

    @Override
    public void run() {
        while (true) {
            try {
//                Socket serverSocket = serverCoordinationSocket.accept(); // moved to loop in outer thread
                InputStream inputFromClient = serverCoordinationSocket.getInputStream();
                Scanner serverInputScanner = new Scanner(inputFromClient, String.valueOf(StandardCharsets.UTF_8));
                String line = serverInputScanner.nextLine();
                logger.debug("Received: " + line);
                JSONParser jsonParser = new JSONParser();
                JSONObject jsonPayload = (JSONObject) jsonParser.parse(line);
                resolveServerRequest(jsonPayload, serverCoordinationSocket);
            } catch (IOException | ParseException e) {
                logger.debug(e);
            }
        }
    }

    private void resolveServerRequest(JSONObject jsonPayload, Socket serverSocket) throws IOException, ParseException {
        String type = (String) jsonPayload.get("type");
        JSONObject response;

        switch (type) {
            case "Create new identity":
                boolean isAvailable = Framework.askServers(String.valueOf(jsonPayload.get("identity")), ChatServerConstants.ServerConstants.IDENTITY);
            case "ASK":
                logger.debug("Responding to ASK.");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("REPLY", "test-reply");
                Messaging.respondClient(jsonObject, serverSocket);
                // Receives a question.
                // Respond.
        }
    }
}
