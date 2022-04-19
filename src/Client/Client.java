package Client;

import java.io.DataInputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 3000;
    private static final int CONNECTION_TIMEOUT = 2000;

    public static void main(String[] args) {
        // We attempt to establish a connection to the Server socket using a
        // try-with-resources. This ensures that the socket is closed after
        // the block exist.
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            // Once we connect, establish the Server Data Streams
            DataInputStream response = new DataInputStream(socket.getInputStream());

            // And set the Socket's timeout value
            socket.setSoTimeout(CONNECTION_TIMEOUT);

            // At this point, we expect the Server to send a connection acknowledgement bit.
            // If we didn't get one, the socket connection was not accepted!
            response.readBoolean();

            // If the connection was accepted, alert the user of which Server they connected
            // to.
            LOGGER.log(Level.INFO, "Connection established with {0}\n", socket.getInetAddress());

            RequestHandler requestHandler = new RequestHandler(socket);
            ResponseHandler responseHandler = new ResponseHandler(socket);
            requestHandler.responseHandler = responseHandler;
            responseHandler.requestHandler = requestHandler;

            Thread requestThread = new Thread(requestHandler);
            Thread responseThread = new Thread(responseHandler);

            requestThread.start();
            responseThread.start();

            requestThread.join();
            responseThread.join();
        } catch (ConnectException | SocketTimeoutException e) {
            LOGGER.log(Level.SEVERE, "Could not establish a connection with {0}\n", SERVER_HOST);
        } catch (SocketException e) {
            LOGGER.log(Level.SEVERE, "Could not use the socket: {0}\n", e.getMessage());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Something really bad happened and the Client unexpectedly stopped.");
            ex.printStackTrace();
        } finally {
        }
    }
}
