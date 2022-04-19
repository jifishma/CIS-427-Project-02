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
                // If we don't have a response from the response handler
                // thread, we'll just wait
                if (!responseHandler.hasResponse.getAndSet(false) || this.shouldClose.get()) {
                    continue;
                }

                // Otherwise we begin the prompt
                System.out.print("C:\t");

                if (input.hasNextLine()) {
                    // If we have input, set our hasRequest flag
                    this.hasRequest.set(true);
                    String message = input.nextLine();
                    String parsedMessage = message.trim().toLowerCase();

                    // Submit a request or command to the Server
                    this.requests.writeUTF(message);

                    // And if we're trying to close the client, let the response handler know
                    if (parsedMessage.equals("shutdown") || parsedMessage.equals("logout")) {
                        this.shouldClose.set(true);
                    }
                }

                // And we'll clear the response handler's flag at this point.
                // It may be updated before this request handler's next loop begins
                // to indicate we have a response and should not print a prompt yet
                responseHandler.hasResponse.set(false);
            } while (!shouldClose.get() || !responseHandler.shouldClose.get());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn\'t read from the input stream.");
        }
    }
}
