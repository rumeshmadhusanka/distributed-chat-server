package Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Server {

    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
    }

    public void startServer() {
        try {
            // Start server and wait for client to connect.
            ServerSocket serverSocket = new ServerSocket(9991);
            System.out.println("Waiting for a client to connect");
            Socket connectionSocket = serverSocket.accept();
            System.out.println("Connection Established");

            //Create Input for the connection
            InputStream inputToServer = connectionSocket.getInputStream();
            Scanner scanner = new Scanner(inputToServer, String.valueOf(StandardCharsets.UTF_8));

//            OutputStream output = connectionSocket.getOutputStream();
//            PrintWriter writer = new PrintWriter(output, true);
//            writer.println("This is a message sent to the server");

            while(connectionSocket.isConnected()) {
                String line = scanner.nextLine();
                JSONParser jsonParser = new JSONParser();
                JSONObject request = (JSONObject) jsonParser.parse(line);
                System.out.println(request.get("type"));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        System.out.println("Connection has ended");
    }
}
