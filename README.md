https://github.com/jifishma/CIS-427-Project-02

## Getting Started

### Building the project

This project was built using RedHat OpenJDK, version 11.0.10-1.
To build the project, navigate to the 'src' directory and run the following commands:

For the client:

> `javac -d .\bin\ .\src\Client\*.java`

For the server:

> `javac -d .\bin\ .\src\Server\*.java`

### Running the project

To run the project, you can run the commands below. The Server must be started before the Client. Please note that only one Client can connect to the Server at a time.

If for some reason you're seeing the below error message with only one Client:

> `SEVERE: Could not establish a connection with <host>`

Try increasing the CONNECTION_TIMEOUT value at the very top of the Client's source code.

For the client:

> `java -cp Client.Client`

For the server:

> `java -cp Server.Server`

## Available commands

-   Terminology:
    -   \<\> = required argument
    -   [ ] = optional argument
-   LOGIN \<username\> \<password\>
    -   Attempt to log in to a user account with the provided credentials
-   SOLVE \<-c | -r\> \<radius | side length\> [side length 2]
    -   If -c is specified, solve for a circle, if -r is specified, solve for a rectangle
    -   For a circle:
        -   Define a radius to solve Area and Cicumference for
    -   For a rectangle:
        -   Define one or two side lengths to solve Area and Perimeter for
-   LIST [-all]
    -   Print the current user's requested SOLVE operations and results
    -   If logged in as "root", print all of the users' requested SOLVE operations and results
-   MESSAGE \<-all | user's name\> \<message content\>
    -   Directly message another connected user
    -   If logged in as "root", optionally broadcast your message to all connected users
-   LOGOUT
    -   Log the current user out and exit
-   SHUTDOWN
    -   Shut down the Server, log the current user out, and exit

## Example of commands and results

### LOGIN

```
Conection established with localhost/127.0.0.1
C:      login root root22
S:      SUCCESS
```

### SOLVE

Valid requests:

```
C:      solve -r 22
S:      Rectangle's perimeter is 88.00 and area is 484.00

C:      solve -r 22 33
S:      Rectangle's perimeter is 110.00 and area is 726.00

C:      solve -c 10
S:      Circle's circumference is 62.83 and area is 314.16

C:      solve -r 23.13
S:      Rectangle's perimeter is 92.52 and area is 535.00

C:      solve -c 80.23
S:      Circle's circumference is 504.10 and area is 20221.97
```

Invalid requests:

```
C:      solve -r
S:      ERROR: No sides found

C:      solve -c
S:      ERROR: No radius found

C:      solve -r abc
S:      301 message format error

C:      solve -c abc
S:      301 message format error
```

### LIST

Valid request:

```
C:      list
S:
                sides 22.0 22.0:        Rectangle's perimeter is 88.00 and area is 484.00
                sides 22.0 33.0:        Rectangle's perimeter is 110.00 and area is 726.00
                radius 10.0:    Circle's circumference is 62.83 and area is 314.16
                sides 23.13 23.13:      Rectangle's perimeter is 92.52 and area is 535.00
                radius 80.23:   Circle's circumference is 504.10 and area is 20221.97
                ERROR: No sides found
                ERROR: No radius found
```

Requesting '-all' without root:

```
C:      list -all
S:      FAILURE: This method is only accessible to the root user
```

Requesting '-all' as root:

```
C:      list -all
S:
        root
                No interactions yet
        sally
                No interactions yet
        john
                sides 22.0 22.0:        Rectangle's perimeter is 88.00 and area is 484.00
                sides 22.0 33.0:        Rectangle's perimeter is 110.00 and area is 726.00
                radius 10.0:    Circle's circumference is 62.83 and area is 314.16
                sides 23.13 23.13:      Rectangle's perimeter is 92.52 and area is 535.00
                radius 80.23:   Circle's circumference is 504.10 and area is 20221.97
                ERROR: No sides found
                ERROR: No radius found
        qiang
                No interactions yet
```

### MESSAGE

Simple messaging:

```
INFO: Connection established with localhost/127.0.0.1

C:      login root root22
S:      SUCCESS

C:      message -all Hey everyone, what's new??
S:      SUCCESS

C:
S:      Message from john: Just got a car!

C:
S:      Message from qiang: Reading my favorite book.
```

```
C:      login john john22
S:      SUCCESS

C:
S:      Message from root: Hey everyone, what's new??

C:      message root Just got a car!
S:      SUCCESS
```

```
INFO: Connection established with localhost/127.0.0.1

C:      login qiang qiang22
S:      SUCCESS

C:
S:      Message from root: Hey everyone, what's new??

C:      message root Reading my favorite book.
S:      SUCCESS
```

If a user isn't logged in:

```
INFO: Connection established with localhost/127.0.0.1

C:      login root root22
S:      SUCCESS

C:      message sally Are you on today?
S:      User sally is not logged in
```

If a user isn't found:

```
INFO: Connection established with localhost/127.0.0.1

C:      login root root22
S:      SUCCESS

C:      message jacky Yo what's up?
S:      User jacky does not exist
```

### LOGOUT

```
Conection established with localhost/127.0.0.1
C:      login root root22
S:      SUCCESS

C:      logout
S:      200 OK
```

### SHUTDOWN

```
Conection established with localhost/127.0.0.1
C:      login john john22
S:      SUCCESS

C:      shutdown
S:      200 OK
```
