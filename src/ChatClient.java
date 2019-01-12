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
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

class WebServer2 {

    ServerSocket serverSock;// server socket for connection
    static Boolean running = true;  // controls if the server is accepting clients
    static Boolean accepting = true;
    /** Main
     * @param args parameters from command line
     */
    public static void main(String[] args) {
        new WebServer().go(); //start the server
    }

    /** Go
     * Starts the server
     */
    public void go() {
        System.out.println("Waiting for a client connection..");

        Socket client = null;//hold the client connection

        try {
            serverSock = new ServerSocket(8080);  //assigns an port to the server
            // serverSock.setSoTimeout(15000);  //5 second timeout
            while(accepting) {  //this loops to accept multiple clients
                client = serverSock.accept();  //wait for connection
                System.out.println("Client connected");
                //Note: you might want to keep references to all clients if you plan to broadcast messages
                //Also: Queues are good tools to buffer incoming/outgoing messages
                Thread t = new Thread(new ConnectionHandler(client)); //create a thread for the new client and pass in the socket
                t.start(); //start the new thread
                accepting=false;
            }
        }catch(Exception e) {
            // System.out.println("Error accepting connection");
            //close all and quit
            try {
                client.close();
            }catch (Exception e1) {
                System.out.println("Failed to close socket");
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

        //GUI Stuff
        private JButton sendButton, htmlButton;
        private JTextField typeField;
        private JTextArea msgArea;
        private JPanel southPanel;

        /* ConnectionHandler
         * Constructor
         * @param the socket belonging to this client connection
         */
        ConnectionHandler(Socket s) {
            this.client = s;  //constructor assigns client to this
            try {  //assign all connections to client
                this.output =  new DataOutputStream(client.getOutputStream());
                InputStreamReader stream = new InputStreamReader(client.getInputStream());
                this.input = new BufferedReader(stream);
            }catch(IOException e) {
                e.printStackTrace();
            }
            running=true;
            //initGUI(); //start the GUI
        } //end of constructor


//        public void initGUI() {
//            JFrame window = new JFrame("Web Server");
//            southPanel = new JPanel();
//            southPanel.setLayout(new GridLayout(2,0));
//
//            sendButton = new JButton("SEND HTML W/IMAGE");
//            htmlButton = new JButton("SEND HTML ");
//
//            sendButton.addActionListener(new SendButtonListener());
//            htmlButton.addActionListener(new htmlButtonListener());
//
//            JLabel errorLabel = new JLabel("");
//
//            typeField = new JTextField(10);
//
//            msgArea = new JTextArea();
//
//            southPanel.add(typeField);
//            southPanel.add(sendButton);
//            southPanel.add(errorLabel);
//            southPanel.add(htmlButton);
//
//            window.add(BorderLayout.CENTER,msgArea);
//            window.add(BorderLayout.SOUTH,southPanel);
//
//            window.setSize(400,400);
//            window.setVisible(true);
//
//            // call a method that connects to the server
//            // after connecting loop and keep appending[.append()] to the JTextArea
//        }


        /* run
         * executed on start of thread
         */
        public void run() {

            //Get a message from the client
            String msg="";

            //Get a message from the client
            while(running) {  // loop unit a message is received
                try {
                    if (input.ready()) { //check for an incoming message
                        msg = input.readLine();  //get a message from the client
                        msgArea.append(msg + "\n");
                    }
                }catch (IOException e) {
                    System.out.println("Failed to receive msg from the client");
                    e.printStackTrace();
                }
            }

            //close the socket
            try {
                input.close();
                output.close();
                client.close();
            }catch (Exception e) {
                System.out.println("Failed to close socket");
            }
        } // end of run()


        //****** Inner Classes for Action Listeners ****

        //To complete this you will need to add action listeners to both buttons
        // clear - clears the textfield
        // send - send msg to server (also flush), then clear the JTextField
        class SendButtonListener implements ActionListener {
            public void actionPerformed(ActionEvent event)  {

                File imgFile = new File("img.jpg");
                BufferedInputStream in = null;

                try {
                    in = new BufferedInputStream(new FileInputStream(imgFile));
                    int data;

                    System.out.println("File Size: " + imgFile.length());
                    byte[] d = new byte[(int)imgFile.length()];

                    output.writeBytes("HTTP/1.1 200 OK" + "\n");
                    output.flush();

                    output.writeBytes("Content-Type: image/jpg"+"\n");
                    output.flush();
                    output.writeBytes("Content-Length: " + imgFile.length() + "\n\n");
                    output.flush();

                    String t = "";


                    in.read(d,0,(int)imgFile.length());
                    System.out.println("read: " + imgFile.length()+" bytes");
                    output.write(d,0,d.length);
                    System.out.println("sent: " + imgFile.length()+" bytes");


                    output.flush();

                } catch (IOException e) {
                    e.printStackTrace();}


                msgArea.append("SENT HTML W IMAGE RESPONSE!");


                running=false; //end the server
                try {
                    input.close();
                    output.close();
                    client.close();
                }catch (Exception e) {
                    System.out.println("Failed to close socket");
                }

            }
        }

        class htmlButtonListener implements ActionListener {
            public void actionPerformed(ActionEvent event)  {

                File webFile = new File("WebPage.html");
                BufferedInputStream in = null;

                try {
                    in = new BufferedInputStream(new FileInputStream(webFile));
                    int data;

                    System.out.println("File Size: " +webFile.length());
                    byte[] d = new byte[(int)webFile.length()];

                    output.writeBytes("HTTP/1.1 200 OK" + "\n");
                    output.flush();

                    output.writeBytes("Content-Type: text/html"+"\n");
                    output.flush();
                    output.writeBytes("Content-Length: " + webFile.length() + "\n\n");
                    output.flush();

                    String t = "";


                    in.read(d,0,(int)webFile.length());
                    System.out.println("read: " +webFile.length()+" bytes");
                    output.write(d,0,d.length);
                    System.out.println("sent: " +webFile.length()+" bytes");


                    output.flush();

                } catch (IOException e) {
                    e.printStackTrace();}


                msgArea.append("SENT HTML REPONSE!");


                running=false; //end the server
                try {
                    input.close();
                    output.close();
                    client.close();
                }catch (Exception e) {
                    System.out.println("Failed to close socket");
                }

            }

        }



    } //end of inner class


} //end of SillyServer class
