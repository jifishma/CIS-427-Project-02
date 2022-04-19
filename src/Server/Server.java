package Server;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private static final int SERVER_PORT = 3000;

    public static void main(String[] args) {
        // Create a Server Socket with the defined port number
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            LOGGER.log(Level.INFO, "Server started.\n");

            do {
                // And wait for a Client to establish a connection over that Socket.
                // This is a blocking operation.
                Socket clientConn = null;

                try {
                    clientConn = serverSocket.accept();
                } catch (Exception e) {
                    // Consume the exception
                }

                if (clientConn != null) {
                    // Once a connection is established, run the Server's main logic for each
                    // client:
                    Handler h = new Handler(clientConn, serverSocket);
                    Thread t = new Thread(h);
                    t.start();
                }
            } while (!serverSocket.isClosed());
        } catch (SocketException e) {

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Something really bad happened and the Server unexpectedly stopped.");
            e.printStackTrace();
        } finally {
            LOGGER.log(Level.INFO, "Trying to close all clients.\n");

            Handler.closeAllClients();

            LOGGER.log(Level.INFO, "Server shut down.\n");
        }
    }
}
