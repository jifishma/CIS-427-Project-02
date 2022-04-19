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
    public ResponseHandler responseHandler;

    public AtomicBoolean hasRequest = new AtomicBoolean(false);
    public AtomicBoolean shouldClose = new AtomicBoolean(false);

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
                    this.hasRequest.set(true);
                    String message = input.nextLine();
                    String parsedMessage = message.trim().toLowerCase();

                    // Submit a request or command to the Server
                    this.requests.writeUTF(message);

                    if (parsedMessage.equals("shutdown") || parsedMessage.equals("logout")) {
                        this.shouldClose.set(true);
                    }
                }

                responseHandler.hasResponse.set(false);
            } while (!shouldClose.get());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn\'t read from the input stream.");
        }
    }
}
