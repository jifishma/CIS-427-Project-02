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
        try {
            while (responses != null) {
                // Collect the Server's reply back and print it out for the user,
                // if we have one available
                if (responses.available() > 0) {
                    String reply = responses.readUTF();

                    if (!reply.trim().equals("")) {
                        // If the request handler has a request queued and was ready for the 
                        // response, we'll unset the flag here and just print the result
                        if (requestHandler.hasRequest.getAndSet(false)) {
                            System.out.println("S:\t" + reply + "\n");
                        } else {
                            // Otherwise, we'll break to a new line, print the response, 
                            // and reprint the prompt.

                            System.out.print("\n");
                            System.out.println("S:\t" + reply + "\n");
                            System.out.print("C:\t");
                        }

                        // And flag that we have a response at this point for the request 
                        // handler
                        hasResponse.set(true);
                    }

                    // If the request handler was trying to close the client, we'll check
                    // for a valid exit condition here (we tried to leave, and the server said OK.)
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
