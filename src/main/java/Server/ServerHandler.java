package Server;

import ClientHandler.ClientHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.ServerSocket;
import java.net.Socket;

public class ServerHandler extends Thread {
    //TODO: Implement Server-server communication (Similar to client-server)

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    private final ServerSocket serverCoordSocket;

    public ServerHandler(ServerSocket serverCoordSocket) {
        this.serverCoordSocket = serverCoordSocket;
    }

    @Override
    public void run(){
        try{
            while(true){
//                Socket serverSocket = ser
            }
        } catch (Exception e) {

        }
    }
}
