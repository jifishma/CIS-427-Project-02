package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Server {
    private static final Logger LOGGER = System.getLogger(Server.class.getName());
    // Create an instance of our CredsManager singleton and cache it
    private static final CredsManager CREDS_MANAGER = CredsManager.getInstance();
    // Define how we want to format our Decimals
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");
    private static final int SERVER_PORT = 3000;

    public static void main(String[] args) {
        // Create a Server Socket with the defined port number
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            LOGGER.log(Level.INFO, "Server started.\n");

            boolean shutdown = false;
            do {
                // And wait for a Client to establish a connection over that Socket.
                // This is a blocking operation.
                Socket clientConn = serverSocket.accept();

                // Once a connection is established, run the Server's main logic
                shutdown = runServer(clientConn);
            } while (!shutdown);

            LOGGER.log(Level.INFO, "Server shut down.\n");
        } catch (Exception e) {
            LOGGER.log(Level.ERROR, "Something really bad happened and the Server unexpectedly stopped.");
            e.printStackTrace();
        }
    }

    // The Server's main logic
    private static boolean runServer(Socket clientConn) throws IOException {

        // Establish the Client Data Streams
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
                if (!command.isBlank() && !command.equals("login") && !command.equals("logout")
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

    private static void solve(List<String> args, DataOutputStream response, Writer writer) throws IOException {
        // Check if we received an appropriate operation flag to solve for
        boolean isRectangle = args.contains("-r");
        boolean isCircle = args.contains("-c");

        Double first = null;
        Double second = null;
        Double area = null;
        Double perimeter = null;

        // If we don't have any flag or too many flags, return an error to the Client
        if ((!isRectangle && !isCircle) || (isRectangle && isCircle)) {
            response.writeUTF("ERROR: Invalid operation(s) specified");

            // And exit to allow the client to retry
            return;
        } else {
            // Otherwise remove the flag from the arguments and continue
            args.remove(0);
        }

        // If we only have a flag, alert the user of missing arguments and allow the
        // Client to retry
        if (args.isEmpty()) {
            String result = "";
            if (isRectangle) {
                result = "ERROR: No sides found";
            } else if (isCircle) {
                result = "ERROR: No radius found";
            }

            // We'll log the solve operation failure to the user's solutions file.
            writer.append(result);
            writer.append("\n");
            writer.flush();

            // And return the error to the Client.
            response.writeUTF(result);

            // We'll then exit to allow the client to retry
            return;
        }

        // If we have numerical arguments, get the first argument and possibly the
        // second if it exists. If it doesn't, the second is initialized to the
        // same value as the first.
        try {
            first = Double.valueOf(args.get(0));

            if (args.size() >= 2) {
                second = Double.valueOf(args.get(1));
            } else {
                second = first;
            }
        } catch (NumberFormatException e) {
            // If the arguments provided were not numerical, raise a 301 format error and
            // return it to the Client.
            response.writeUTF("301 message format error");

            // We then exit to allow the Client to retry
            return;
        }

        // Now we can perform a solve operation. Check if it's a rectangle or circle to
        // solve
        if (isRectangle) {
            // Calculate area and perimeter
            area = first * second;
            perimeter = 2 * (first + second);

            // Format the result with the appropriate decimal places
            String solution = MessageFormat.format(
                    "Rectangle''s perimeter is {0} and area is {1}",
                    DECIMAL_FORMAT.format(perimeter), DECIMAL_FORMAT.format(area));

            // Write the results to the user's solutions file
            writer.append("sides " + first + " " + second + ":\t" + solution);
            writer.append("\n");
            writer.flush();

            // And return the result to the Client
            response.writeUTF(solution);

            // Then exit to allow the Client to submit another request
            return;
        } else if (isCircle) {
            // Calculate area and circumference
            area = Math.PI * Math.pow(first, 2);
            perimeter = 2 * Math.PI * first;

            // Format the result with the appropriate decimal places
            String solution = MessageFormat.format(
                    "Circle''s circumference is {0} and area is {1}",
                    DECIMAL_FORMAT.format(perimeter), DECIMAL_FORMAT.format(area));

            // Write the results to the user's solutions file
            writer.append("radius " + first + ":\t" + solution);
            writer.append("\n");
            writer.flush();

            // And return the result to the Client
            response.writeUTF(solution);

            // Then exit to allow the Client to submit another request
            return;
        }

        // If we ended up here, something really went wrong. Somehow, a
        // flag was found and stored, but dropped from memory before we made it to
        // solving.
        response.writeUTF("ERROR: The operation failed spectacularly, and it's unknown why! :(");
    }

    private static void list(List<String> args, DataOutputStream response) throws IOException {
        String currentUser = CREDS_MANAGER.getAuthenticatedUser();
        StringBuilder result = new StringBuilder("\n");

        // Check if the arguments contains the -all flag
        if (args.contains("-all")) {
            // If it does, check if the current user is "root"
            if (!currentUser.equalsIgnoreCase("root")) {
                // If they're not, return a failure message to the Client and have them try
                // again.
                response.writeUTF("FAILURE: This method is only accessible to the root user");
                return;
            }

            // Otherwise, collect all of the usernames
            for (String username : CREDS_MANAGER.getAllUsernames()) {
                // Write them to the results
                result.append(MessageFormat.format("\t{0}\n", username));

                // And then write the contents of that user's solutions file to results.
                result.append(getSolutions(username));
            }

            // Return the results back to the Client
            response.writeUTF(result.toString());

            // And let the Client submit another request.
            return;
        }

        // If we didn't find an -all flag, simply write the contents of the current
        // user's solutions file to results
        result.append(getSolutions(currentUser));

        // And return the results back to the Client
        response.writeUTF(result.toString());
    }

    // Get the contents of the specified user's solutions file and return it as a
    // String.
    private static String getSolutions(String username) throws FileNotFoundException {
        StringBuilder result = new StringBuilder();
        File userSolutionsFile = new File("solutions\\" + username + "_solutions.txt");

        // If the solutions file exists, read it
        if (userSolutionsFile.exists()) {
            try (Scanner reader = new Scanner(userSolutionsFile)) {
                // If the solutions file is empty, write no interactions
                if (!reader.hasNextLine()) {
                    result.append("\t\tNo interactions yet\n");
                    return result.toString();
                }

                // Otherwise write all contents to results
                while (reader.hasNextLine()) {
                    result.append(MessageFormat.format("\t\t{0}\n", reader.nextLine()));
                }
            }
        } else {
            // If the solutions file doesn't exist, write no interactions.
            result.append("\t\tNo interactions yet\n");
        }

        // Return the results back as a single String.
        return result.toString();
    }
}
