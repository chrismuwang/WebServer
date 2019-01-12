/* [SimpleWebServerDemo.java]
 * Description: This is an example of a web server.
 * The program  waits for a client and accepts a message.
 * It then responds to the message and quits.
 * This server demonstrates how to employ multithreading to accepts multiple clients
 * @author Mangat
 * @version 1.0a
 */

//imports for network communication

import java.io.*;
import java.net.*;
import java.sql.*;

class WebServer {

    ServerSocket serverSock;// server socket for connection
    static Boolean running = true;  // controls if the server is accepting clients
    static Boolean accepting = true;

    /**
     * Main
     *
     * @param args parameters from command line
     */
    public static void main(String[] args) {
        //Create database and table
        try {
            Connection c = null;
            Statement stmt = null;

            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:information.db");
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = "CREATE TABLE INFORMATION " +
                    "(USERNAME           TEXT    NOT NULL, " +
                    " PASSWORD            TEXT     NOT NULL, " +
                    " NUMBER              TEXT      NOT NULL)";

            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Table created successfully");

        //Start the server
        new WebServer().go();
    }


    /**
     * Go
     * Starts the server
     */
    public void go() {
        System.out.println("Waiting for a client connection..");

        //Hold the client connection
        Socket client = null;

        try {
            //Assigns a port to the server
            serverSock = new ServerSocket(8080);

            //Loop to accept multiple clients
            while (accepting) {
                client = serverSock.accept();
                System.out.println("Client connected");

                //Create a thread for the new client and pass in the socket
                Thread t = new Thread(new ConnectionHandler(client));

                //Start the new thread
                t.start();
                accepting = false;
            }
        } catch (Exception e) {
             System.out.println("Error accepting connection");

            //Close all and quit
            try {
                client.close();
            } catch (Exception e1) {
                System.out.println("Failed to close socket");
                e.printStackTrace();
            }
            System.exit(-1);
        }
    }

    //***** Inner class - thread for client connection
    class ConnectionHandler implements Runnable {
        private DataOutputStream output; //assign printwriter to network stream
        private BufferedReader input; //Stream for network input
        private Socket client;  //keeps track of the client socket
        private boolean running;

        //Call client header reader class
        HTTPReader clientHeader = new HTTPReader();

        /* ConnectionHandler
         * Constructor
         * @param the socket belonging to this client connection
         */
        ConnectionHandler(Socket s) {
            this.client = s;  //constructor assigns client to this
            try {  //assign all connections to client
                this.output = new DataOutputStream(client.getOutputStream());
                InputStreamReader stream = new InputStreamReader(client.getInputStream());
                this.input = new BufferedReader(stream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            running = true;
        } //end of constructor


        /* run
         * executed on start of thread
         * reads and stores client header
         * calls method to handle get and post requests
         */
        public void run() {

            //Get a message from the client
            String msg;

            while (running) {  // loop until a message is received
                try {
                    if (input.ready()) { //check for an incoming message
                        //get a message from the client
                        while (true) {
                            //reads the header from client by line
                            msg = input.readLine();
                            //checks to see if there is more to read
                            if (msg == null || msg.equals("")) break;

                            //store line of header
                            clientHeader.add(msg.substring(0, msg.indexOf(" ")), msg.substring(msg.indexOf(" "), msg.length()));
                        }

                        //print client header (testing purposes)
                        clientHeader.print();

                        //call get method to handle get requests
                        if (clientHeader.contains("GET")) {
                            getMethod();
                        //call post method to handle post requests
                        }else if (clientHeader.contains("POST")){
                            postMethod();
                        }
                    }

                } catch (IOException e) {
                    System.out.println("Failed to receive msg from the client");
                    e.printStackTrace();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            //close the socket
            try {
                input.close();
                output.close();
                client.close();
            } catch (Exception e) {
                System.out.println("Failed to close socket");
                e.printStackTrace();
            }
        }


        /* getMethod
         * executed upon get request from client
         * responds with requested page/image
         * sends back server response
         */

        public void getMethod() {
            File file = null;
            BufferedInputStream in;

            //retrieve client header
            String path = clientHeader.getValue("GET");
            //initialize variables
            String contentType = "";
            String httpCode = "200 OK";

            //define file requested based on path
            if (path.substring(0, 3).equals(" / ") || path.contains("home")) {
                file = new File("WebPage.html");
            } else if (path.contains("login")) {
                file = new File("LoggedIn.html");
            } else if (path.contains("signup")) {
                file = new File("SignedUp.html");
            }else{
                file = new File("403.html");
                httpCode = "403 Forbidden";
            }

            //check type of file
            if (path.contains(".jpg")) {
                contentType = "image/jpg";
                file = new File("img.jpg");
            }

            if (path.contains(".html")){
                contentType = "text/html";
            }

            if (path.contains(".ico")){
                contentType = "image/x-icon";
                file = new File("img.jpg");
            }

            //write back to client
            try {
                in = new BufferedInputStream(new FileInputStream(file));

                System.out.println("File Size: " + file.length());
                byte[] d = new byte[(int) file.length()];

                output.writeBytes("HTTP/1.1 " + httpCode + "\n");
                output.writeBytes("Content-Type: " + contentType + "\n");
                output.writeBytes("Content-Length: " + file.length() + "\n\n");
                output.flush();


                in.read(d, 0, (int) file.length());
                System.out.println("read: " + file.length() + " bytes");
                output.write(d, 0, d.length);
                System.out.println("sent: " + file.length() + " bytes");

                output.flush();

                //reset client header for next request
                clientHeader.reset();
                run();

            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                input.close();
            } catch (Exception e) {
                System.out.println("Failed to close socket");
                e.printStackTrace();
            }
        }


        /* postMethod
         * executed upon post request from client
         * responds to sign up/login attempts with corresponding pages
         * saves and reads user information to sql database
         * sends back server response
         */
        public void postMethod() {
            //retrieve client header
            String path = clientHeader.getValue("POST");
            String httpCode = "200 OK";

            //initialize sql variables
            Connection c = null;
            Statement stmt = null;

            //initialize variables
            String sql = "";
            String data = "";
            String username = "";
            String password = "";
            String page = "";
            String number = "";
            Boolean usernameFound = false;

            //get length of client header to read
            int contentLength = Integer.parseInt(clientHeader.getValue("Content-Length:").trim());

            //read and store client header
            try {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < contentLength; i++) {
                    sb.append((char) input.read());
                }
                data = sb.toString();
            }catch (IOException e){
                e.printStackTrace();
            }

            //handle login requests
           if (path.contains("login")) {
                //read through data received from client
               if(data != "" && data != null){

                   username = data.substring(data.indexOf("=") + 1, data.indexOf("&"));
                   data = data.substring(data.indexOf("&")+1);
                   password = data.substring(data.indexOf("=") + 1, data.indexOf("&"));
               }
               //search database for matching information
                try {
                    c = DriverManager.getConnection("jdbc:sqlite:information.db");
                    System.out.println("Opened database successfully");

                    stmt = c.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM INFORMATION;");

                    while (rs.next()) {
                        String usernameLine = rs.getString("USERNAME");
                        String passwordLine = rs.getString("PASSWORD");
                        String numberLine = rs.getString("NUMBER");

                        if (usernameLine.equals(username)) {
                            usernameFound = true;

                            if (passwordLine.equals(password)) {
                                number = numberLine;

                                //page for successful login attempts
                                page = "<!DOCTYPE html>\n" +
                                        "<html>\n" +
                                        "<head><title>Web Server Page</title></head>\n" +
                                        "<body>\n" +
                                        "<h2 align=\"left\">LOGIN SUCCESSFUL</h2>\n" +
                                        "<form method=\"GET\" action=\"home\">\n" +
                                        "    Login successful. Hi " + username +
                                        ", your favourite number is " + number + ".<br /><br />\n" +
                                        "    <input type=\"submit\" value=\"Logout\" />\n" +
                                        "</form>\n" +
                                        "</form>\n" +
                                        "<br /><br />\n" +
                                        "<form method=\"POST\" action=\"changepassword\">\n" +
                                        "    <input type=\"hidden\" name=\"username\" value=\"" + username + "\" />"+
                                        "   Change password: <input type=\"text\" name=\"password\"><br /><br />\n" +
                                        "    <input type=\"submit\" value=\"Change password\" />\n" +
                                        "</form>"+
                                        "</form>\n" +
                                        "<br /><br />\n" +
                                        "<form method=\"POST\" action=\"changenumber\">\n" +
                                        "    <input type=\"hidden\" name=\"username\" value=\"" + username + "\" />"+
                                        "   Change favourite number: <input type=\"text\" name=\"password\"><br /><br />\n" +
                                        "    <input type=\"submit\" value=\"Change number\" />\n" +
                                        "</form>"+
                                        "</body>\n" +
                                        "</html>";
                                break;
                                }
                        }
                    }
                    //close database access points
                    rs.close();
                    stmt.close();
                    c.close();
                }catch (SQLException e){
                    e.printStackTrace();
                }

                    if(page.equals("")) {
                        //page for incorrect password login attempts
                        if (usernameFound == true) {
                            page = "<!DOCTYPE html>\n" +
                                    "<html>\n" +
                                    "<head><title>Web Server Page</title></head>\n" +
                                    "<body>\n" +
                                    "<h2 align=\"left\">LOGIN FAILED</h2>\n" +
                                    "<form method=\"GET\" action=\"home\">\n" +
                                    "    Login failed. Incorrect password. <br /><br />\n" +
                                    "    <input type=\"submit\" value=\"Return to home\" />\n" +
                                    "</form>\n" +
                                    "</body>\n" +
                                    "</html>";
                        //page for no username entered - 401 unauthorized login attempts
                        }else if(username.equals("")){
                            httpCode = "401 Unauthorized";
                            page = "<!DOCTYPE html>\n" +
                                    "<html>\n" +
                                    "<head><title>Web Server Page</title></head>\n" +
                                    "<body>\n" +
                                    "<h2 align=\"left\">401 Unauthorized</h2>\n" +
                                    "<form method=\"GET\" action=\"home\">\n" +
                                    "    You do not have access to this page.<br /><br />\n" +
                                    "    <input type=\"submit\" value=\"Return Home\" />\n" +
                                    "</form>\n" +
                                    "</body>\n" +
                                    "</html>";
                        //page for username not found
                        }else{
                            page = "<!DOCTYPE html>\n" +
                                    "<html>\n" +
                                    "<head><title>Web Server Page</title></head>\n" +
                                    "<body>\n" +
                                    "<h2 align=\"left\">LOGIN FAILED</h2>\n" +
                                    "<form method=\"GET\" action=\"home\">\n" +
                                    "    Login failed. Username not found. <br /><br />\n" +
                                    "    <input type=\"submit\" value=\"Return Home\" />\n" +
                                    "</form>\n" +
                                    "</body>\n" +
                                    "</html>";
                        }
                    }

            //handle sign up requests
            } else if (path.contains("signup")) {
                //read incoming data
               if(data != "" && data != null){
                   username = data.substring(data.indexOf("=") + 1, data.indexOf("&"));
                   data = data.substring(data.indexOf("&")+1);
                   password = data.substring(data.indexOf("=") + 1, data.indexOf("&"));
                   data = data.substring(data.indexOf("&")+1);
                   number = data.substring(data.indexOf("=") + 1, data.indexOf("&"));
               }
               //open and read from database
               try {
                   c = DriverManager.getConnection("jdbc:sqlite:information.db");
                   System.out.println("Opened database successfully");

                   stmt = c.createStatement();
                   ResultSet rs = stmt.executeQuery("SELECT * FROM INFORMATION;");

                   //read database
                   while (rs.next()) {
                       String usernameLine = rs.getString("USERNAME");

                       //check if username already exists
                       if (usernameLine.equals(username)) {
                           page = "<!DOCTYPE html>\n" +
                                   "<html>\n" +
                                   "<head><title>Web Server Page</title></head>\n" +
                                   "<body>\n" +
                                   "<h2 align=\"left\">SIGN UP FAILED</h2>\n" +
                                   "<form method=\"GET\" action=\"home\">\n" +
                                   "    Sign up unsuccessful. Username already exists.<br /><br />\n" +
                                   "    <input type=\"submit\" value=\"Return Home\" />\n" +
                                   "</form>\n" +
                                   "</body>\n" +
                                   "</html>";
                           break;
                       }
                   }
                   rs.close();
                   stmt.close();
                   c.close();

               } catch (SQLException e) {
                   e.printStackTrace();
               }

               //page if username does not exist in database
               if (page.equals("")) {
                   //page if no username entered
                   if (username.equals("")) {
                       httpCode = "401 Unauthorized";
                       page = "<!DOCTYPE html>\n" +
                               "<html>\n" +
                               "<head><title>Web Server Page</title></head>\n" +
                               "<body>\n" +
                               "<h2 align=\"left\">401 Unauthorized</h2>\n" +
                               "<form method=\"GET\" action=\"home\">\n" +
                               "    You do not have access to this page.<br /><br />\n" +
                               "    <input type=\"submit\" value=\"Return Home\" />\n" +
                               "</form>\n" +
                               "</body>\n" +
                               "</html>";
                   //page if sign up successful and stores information in database
                   } else {
                       try {
                           c = DriverManager.getConnection("jdbc:sqlite:information.db");
                           System.out.println("Opened database successfully");

                           stmt = c.createStatement();
                           sql = "INSERT INTO INFORMATION (USERNAME, PASSWORD, NUMBER) " +
                                   "VALUES ( '" + username + "' , '" + password + "', '" + number + "' );";
                           stmt.executeUpdate(sql);

                           stmt.close();
                           c.close();

                       } catch (SQLException e) {
                           e.printStackTrace();
                       }

                       page = "<!DOCTYPE html>\n" +
                               "<html>\n" +
                               "<head><title>Web Server Page</title></head>\n" +
                               "<body>\n" +
                               "<h2 align=\"left\">SIGN UP SUCCESSFUL</h2>\n" +
                               "<form method=\"GET\" action=\"home\">\n" +
                               "    Sign up successful.<br /><br />\n" +
                               "    <input type=\"submit\" value=\"Return Home\" />\n" +
                               "</form>\n" +
                               "</body>\n" +
                               "</html>";
                   }
               }

           //handle password changes
           }else if(path.contains("changepassword")) {

                //read data from client header
               if(data != "" && data != null){

                   //store new password and username
                   username = data.substring(data.indexOf("=") + 1, data.indexOf("&"));
                   data = data.substring(data.indexOf("&")+1);
                   password = data.substring(data.indexOf("=") + 1);
               }

               //open database and update user information
               try {
                   c = DriverManager.getConnection("jdbc:sqlite:information.db");
                   c.setAutoCommit(false);
                   System.out.println("Opened database successfully");

                   stmt = c.createStatement();
                   sql = "UPDATE INFORMATION set PASSWORD = '" + password + "' where USERNAME = '" + username+"';";
                   stmt.executeUpdate(sql);
                   c.commit();
               }catch(SQLException e){
                   e.printStackTrace();
               }

               //page for successful password change
               page = "<!DOCTYPE html>\n" +
                       "<html>\n" +
                       "<head><title>Web Server Page</title></head>\n" +
                       "<body>\n" +
                       "<h2 align=\"left\">PASSWORD CHANGE SUCCESSFUL</h2>\n" +
                       "<form method=\"GET\" action=\"home\">\n" +
                       "    Password change successful.<br /><br />\n" +
                       "    <input type=\"submit\" value=\"Return Home\" />\n" +
                       "</form>\n" +
                       "</body>\n" +
                       "</html>";
            //change favourite number
           }else if(path.contains("changenumber")) {
                //read data from client header
               if(data != "" && data != null){
                   //store username and new number
                   username = data.substring(data.indexOf("=") + 1, data.indexOf("&"));
                   data = data.substring(data.indexOf("&")+1);
                   number = data.substring(data.indexOf("=") + 1);
               }

               //open and update database with new favourite number
               try {
                   c = DriverManager.getConnection("jdbc:sqlite:information.db");
                   c.setAutoCommit(false);
                   System.out.println("Opened database successfully");

                   stmt = c.createStatement();
                   sql = "UPDATE INFORMATION set NUMBER = '" + number + "' where USERNAME = '" + username+"';";
                   stmt.executeUpdate(sql);
                   c.commit();
               }catch(SQLException e){
                   e.printStackTrace();
               }

               //page for successful number changes
               page = "<!DOCTYPE html>\n" +
                       "<html>\n" +
                       "<head><title>Web Server Page</title></head>\n" +
                       "<body>\n" +
                       "<h2 align=\"left\">NUMBER CHANGE SUCCESSFUL</h2>\n" +
                       "<form method=\"GET\" action=\"home\">\n" +
                       "    Number change successful.<br /><br />\n" +
                       "    <input type=\"submit\" value=\"Return Home\" />\n" +
                       "</form>\n" +
                       "</body>\n" +
                       "</html>";
           }

           //write response back to client
            try {
                output.writeBytes("HTTP/1.1 " + httpCode + "\n");
                output.flush();

                output.writeBytes("Content-Type: text/html \n");
                output.writeBytes("Content-Length: " + (page.length() + 1) + "\n\n");
                output.writeBytes("\n");
                output.writeBytes(page);
                output.flush();

                clientHeader.reset();
                run();

            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                input.close();
            } catch (Exception e) {
                System.out.println("Failed to close socket");
                e.printStackTrace();
            }
        }
    }
}
