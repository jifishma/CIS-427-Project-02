package Client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    private DataOutputStream requests;

    public AtomicBoolean shouldClose = new AtomicBoolean(false);
    public ResponseHandler responseHandler;

    public RequestHandler(Socket socket) throws IOException {
        this.requests = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        // Initialize the user's input first!
        try (Scanner input = new Scanner(System.in)) {
            do {
                if (!responseHandler.hasResponse.getAndSet(false)) {
                    continue;
                }

                System.out.print("C:\t");

                if (input.hasNextLine()) {
                    String message = input.nextLine();
                    String parsedMessage = message.trim().toLowerCase();

                    // Submit a request or command to the Server
                    this.requests.writeUTF(message);

                    
                    if (parsedMessage.equals("shutdown") || parsedMessage.equals("logout")) {
                        this.shouldClose.set(true);
                    }
                }
            } while (!shouldClose.get());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn\'t read from the input stream.");
        }
    }
}
