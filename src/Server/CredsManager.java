package Server;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.InvalidPathException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CredsManager {
    private static final HashMap<String, String> credentials = new HashMap<>();
    private static final Pattern r = Pattern.compile("(?<username>^\\w*)\\s+(?<password>\\w*$)");
    private static CredsManager instance = null;

    private String authenticatedUser = "";

    public static CredsManager getInstance() {
        if (instance == null)
            instance = new CredsManager();

        return instance;
    }

    private CredsManager() {
        // Assuming a logins.txt file at the program root
        File credsFile = new File("logins.txt");

        // Verify that we can access and read the logins.txt file
        if (!credsFile.exists() || !credsFile.canRead()) {
            // If the file doesn't exist, raise an exception.
            throw new InvalidPathException(credsFile.getAbsolutePath(), "File could not be accessed.");
        }

        // If the file exists, open an Scanner and read the entries line-by-line
        try (Scanner fileScanner = new Scanner(credsFile)) {
            while (fileScanner.hasNextLine()) {
                String entry = fileScanner.nextLine();
                Matcher m = r.matcher(entry);

                // And check the entries against a regular expression with groups to separate
                // usernames and passwords
                if (m.find()) {
                    String username = m.group("username");
                    String password = m.group("password");

                    // Once we validate both exists and are valid, cache them in a HashMap to ensure
                    // only one set of these credentials can exist.
                    credentials.put(username, password);
                } else {
                    // If we get here, we weren't able to validate the entry against our expression
                    // and therefore hold an invalid username-password pair.
                    throw new InvalidParameterException("Regex could not match a username or password group.");
                }
            }
        } catch (FileNotFoundException e) {
            // If we get here, we couldn't find our logins.txt file on disk.
            e.printStackTrace();
        }
    }

    // Attempt to log in to the Server with the given credentials
    public boolean authenticateUser(String username, String password) {
        if (credentials.containsKey(username) && credentials.get(username).equals(password)) {
            authenticatedUser = username;
            return true;
        }

        return false;
    }

    // Log out the current user from the Server
    public void logoutUser() {
        authenticatedUser = "";
    }

    // Get the current user's username if authenticated
    public String getAuthenticatedUser() {
        return authenticatedUser;
    }

    // Get all available usernames
    public String[] getAllUsernames() {
        return credentials.keySet()
                .toArray(new String[credentials.keySet().size()]);
    }
}
