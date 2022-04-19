package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.Writer;
import java.util.logging.Level;

public class Handler implements Runnable {

    @Override
    public void run() {
        // TODO Auto-generated method stub
        DataInputStream request = new DataInputStream(clientConn.getInputStream());
        DataOutputStream response = new DataOutputStream(clientConn.getOutputStream());

        File solutionsFile = null;
        Writer solutionsFileWriter = null;

        // Send the client a successful connection acknowledgement bit
        response.writeBoolean(true);

        // And log which Client has connected
        LOGGER.log(Level.INFO, "Connection established with {0}\n", clientConn.getInetAddress());

        // Then we enter the message loop
        try {
            while (true) {
                response.flush();

                // Wait for the client to send a command / request
                String message = request.readUTF();

                // After the request is collected, log it
                LOGGER.log(Level.INFO, "Client Request:\t[{0}]: {1}\n", CREDS_MANAGER.getAuthenticatedUser(), message);

                // And parse it out to an argument List
                List<String> args = new ArrayList<>();
                args.addAll(Arrays.asList(message.split(" ")));

                // The first entry is the Client's primary command, we'll remove it from our
                // remaining arguments
                String command = args.remove(0).toLowerCase();

                // We then ensure the Client is logged in before executing on a command, unless
                // the requested command is a login or logout
                if (!command.isEmpty() && !command.equals("login") && !command.equals("logout")
                        && CREDS_MANAGER.getAuthenticatedUser().equals("")) {
                    response.writeUTF("ERROR: You must be logged in to execute a command.");
                    continue;
                }

                // And if we are logged in, check for a matching case to run the primary
                // command.
                switch (command) {
                    case "login":
                        // For login, check that we have both username and password supplied,
                        // otherwise return a 301
                        try {
                            String username = args.get(0);
                            String password = args.get(1);

                            // If we have both, validate them against our available username
                            // and password pairs.
                            if (CREDS_MANAGER.authenticateUser(username, password)) {
                                // If it's valid, load up or create the user's <username>_solutions.txt file
                                solutionsFile = new File("solutions\\" + username + "_solutions.txt");
                                solutionsFile.getParentFile().mkdir();
                                solutionsFile.createNewFile();

                                // Establish the file writer
                                if (solutionsFile.exists() && solutionsFile.canWrite()) {
                                    solutionsFileWriter = new FileWriter(solutionsFile, true);
                                }

                                // And return success to to the Client.
                                response.writeUTF("SUCCESS");
                            } else {
                                // If it's not valid, return a failure and allow the Client to retry
                                response.writeUTF("FAILURE: Please provide a valid username and password");
                            }
                        } catch (IndexOutOfBoundsException e) {
                            response.writeUTF("301 message format error");
                        }
                        break;
                    case "solve":
                        solve(args, response, solutionsFileWriter);
                        break;
                    case "list":
                        list(args, response);
                        break;
                    case "shutdown":
                        // Exit the message loop and close everything.
                        LOGGER.log(Level.INFO, "Server shutting down.\n");
                        response.writeUTF("200 OK");
                        return true;
                    case "logout":
                        // Logout the authenticated user and begin listening for the next client
                        // connection.
                        CREDS_MANAGER.logoutUser();
                        response.writeUTF("200 OK");
                        return false;
                    default:
                        // If we weren't able to match the primary command to any case in the switch,
                        // return a 300 invalid command to the Client.
                        response.writeUTF("300 invalid command");
                        break;
                }
            }
        } catch (EOFException | SocketException e) {
            // If the client's connection ended suddenly, log it and let another client
            // connect instead.
            LOGGER.log(Level.WARNING, "The client's connection has dropped.\n");
            return false;
        } finally {
            // Once a client disconnects, log it and clean up the file writer, client
            // connection, and Client Data Streams.
            LOGGER.log(Level.INFO, "Connection ended with {0}\n", clientConn.getInetAddress());

            if (solutionsFileWriter != null) {
                solutionsFileWriter.close();
            }

            clientConn.close();
            request.close();
            response.close();
        }
    }

}
