package Client;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResponseHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    private DataInputStream responses;
    public RequestHandler requestHandler;
    public AtomicBoolean hasResponse = new AtomicBoolean(true);

    public ResponseHandler(Socket socket) throws IOException {
        this.responses = new DataInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        // Collect the Server's reply back and print it out for the user
        try {
            while (responses != null) {
                if (responses.available() > 0) {
                    String reply = responses.readUTF();

                    if (!reply.trim().equals("")) {
                        System.out.println("S:\t" + reply + "\n");
                        hasResponse.set(true);
                    }

                    if (requestHandler.shouldClose.get() && reply.equals("200 OK")) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn\'t read from the output stream.");
        }
    }
}
