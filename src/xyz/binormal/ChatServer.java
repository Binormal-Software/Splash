package xyz.binormal;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


/**
 * Server to Splash! can have infinite simultaneous connections!!!1!
 */
public class ChatServer extends Application{
	
	private final Client SERVER = new Client("Server");
	private final Client ADMIN = new Client("Admin");
	private List <Client> connectedClients;
	private List <String> bannedUsers;
	
	private Socket tcpSocket;
	private ServerSocket serverSocket;
	private DatagramSocket udpSocket;
	
	private ChatAI ai;
	private ChatServerUI ui;
	protected boolean serverStopped;
	private Stage mainWindow;
	
	/**
	 * Boring stuff 
	 */
	public static void main(String[] args){
		launch(args);
	}
	
	public void start(Stage primaryStage) {
		

		try{
			mainWindow = primaryStage;
			loadUI();}
		catch(IOException e){
			e.printStackTrace();
			System.err.println("Failed to load interface!");
		}

		startServer();
		
	}
	
	private void loadUI() throws IOException{
		
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("res/Server.fxml"));
		Parent root = (Parent)fxmlLoader.load();
		
		if(mainWindow!=null){
			ui = (ChatServerUI) fxmlLoader.getController();
			ui.setWindow(mainWindow);
			ui.setOwner(this);
		}
		
		Scene mainScene = new Scene(root, 600, 400);
		mainScene.getStylesheets().add(getClass().getResource("res/Styles.css").toExternalForm());
		
		mainWindow.setTitle("Splash - Chat Server");
		mainWindow.setScene(mainScene); 
		mainWindow.setOnCloseRequest((e) -> {
			printText("Stopping server...");
	    	System.exit(0);
	    });
		mainWindow.show();
	}
	
	/**
	 * Wait for udpBroadcast
	 */
    private void waitForPing(){
        
    	
        DatagramPacket packet;
    	
    	try{
            udpSocket = new DatagramSocket(Globals.DEFAULT_PORT);
            printText("Listening for udp broadcasts (port " + Globals.DEFAULT_PORT + ")");
        }
        catch( Exception ex ){
        	printText("Problem creating socket on port " + Globals.DEFAULT_PORT );
        	printText("Make sure you don't have another instance of this server running, or another program which uses this port.");
        }

        packet = new DatagramPacket (new byte[1], 1);

        while (!serverStopped){
            try{
            	
                udpSocket.receive (packet); // wait for udp packets
                
                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();
                
                printText("Received ping from: " + clientAddress + ":" + clientPort);
                
                packet.setData (Utils.getFingerprint(Globals.HANDSHAKE_MSG, Globals.APP_VERSION, Globals.DEFAULT_SERVER_NAME)); // respond to broadcast
                udpSocket.send (packet);
                
            }
            catch (IOException ie){
                ie.printStackTrace();
            }
        }
    }
    /**
	 * Wait for tcp connection from client
	 */
    private void waitForConnection(){
    	
 
		try {
		
			serverSocket = new ServerSocket(Globals.DEFAULT_PORT);
			printText("Listening for tcp connections (port " + Globals.DEFAULT_PORT + ")");
			
			while(!serverStopped){
				
				tcpSocket = serverSocket.accept(); // wait for connection attempt
				Client newClient = new Client(tcpSocket); // create new client             
				new Thread(() -> {connectToClient(newClient); }).start(); // start new thread with new client
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        
    	
    }
    /**
	 * Initiate connection with client
	 */
    private void connectToClient(Client client){
    	
    	String username = "Unknown User";
    	byte[] buffer = new byte[128];
    	
    	try {
    		
    		int usernameStatus;
    		
    		do{
   
    			client.getInputStream().read(buffer);
    			username = new String(buffer).trim();
    			
    			usernameStatus = Globals.USERNAME_AVAILABLE;
    			if(usernameReserved(username.toLowerCase())) usernameStatus = Globals.USERNAME_TAKEN;
    			if(bannedUsers.contains(username.toLowerCase())) usernameStatus = Globals.USERNAME_BANNED;
    			
    			client.getOutputStream().writeInt(usernameStatus); // report to client status of username
    			buffer = new byte[128];
    			
    		}while(usernameStatus!=Globals.USERNAME_AVAILABLE);

    		client.setUsername(username);
			connectedClients.add(client);
			
			if(connectedClients.size() > 1){
				broadcastMessage(client.getUsername() + " jumped in! (" + connectedClients.size() + " currently online)", SERVER);
			}else{
				broadcastMessage("Welcome to the server, " + client.getUsername() + "! (There is no one else currently online)", SERVER);
			}
			
			while(client.getInputStream().read(buffer)!=-1){  // main listen loop

				String message = new String(buffer).trim();
				broadcastMessage(message, client);
				processClientCommand(message);

				if(ai.engaged()){
					this.broadcastMessage(ai.getResponse(message), SERVER);
				}
				
				buffer = new byte[128];

			}

		} catch (SocketException e) {
			printText("Connection reset.");
		} catch (Exception e){
			printText("Connection error! " + client.getUsername() + " disconnected.");
			e.printStackTrace();
		} finally{
			if(client.getUsername()!=null)
				broadcastMessage(client.getUsername() + " left.", SERVER);
			connectedClients.remove(client);
		}
    	
    	
    }
    /**
	 * Send a message to all clients
	 */
    private void broadcastMessage(String message, Client sender){
    	
    	printText("" + sender.getUsername() + ": " + message);

    	for(Client c : connectedClients){
    		c.sendMessage(message, sender);
    	}
    	
    }
   
    
    /**
	 * Client being a turd? No problem! 
	 */
    private void kickUser(String user){
    	if(getClientByUsername(user)!=null){
    		getClientByUsername(user).disconnect();
    		printText("'" + user + "' was kicked.");
    	}else{
    		printText("No user named '" + user + "' is online.");
    	}
    	
    }
    /**
	 * Ban user 4ever.exe
	 */
    private void banUser(String user){
    	bannedUsers.add(user.toLowerCase());
    	printText("'" + user + "' banned from server.");
    	try{getClientByUsername(user).disconnect();}
    	catch(NoSuchElementException ne){}
    }
    /**
	 * Having second thoughts?
	 */
    private void unbanUser(String user){
    	if(bannedUsers.contains(user.toLowerCase())){
    		bannedUsers.remove(user.toLowerCase());
    		printText("'" + user + "' unbanned from server.");
    	}else{
    		printText("'" + user + "' is not currently banned!");
    	}
    	
    	
    }
    /**
	 * Get client instance by username 
     * @throws NoSuchElementException 
	 */
    private Client getClientByUsername(String username) throws NoSuchElementException{
    	for(Client c : connectedClients){
    		if(c.getUsername().toLowerCase().equals(username.toLowerCase())){
				return c;
    		}
    	}
    	throw new NoSuchElementException("The specified client could not be found");
    }
    
    /**
	 * Shut down threads and attempt to disconnect from clients
	 */
    protected void stopServer(){
    	broadcastMessage("Server is shutting down.", SERVER);
    	serverStopped = true;
    	
    	try {

    		for(Client c: connectedClients){
    			c.disconnect();
    		}
    		
    		udpSocket.close();
    		serverSocket.close();

    	} catch (Exception e1) {
    		e1.printStackTrace();
    	}
    	
    	
    	
    }
    /**
	 * Start threads
	 */
    protected void startServer(){

    	serverStopped = false;
    	
    	ai = new ChatAI();
		connectedClients = new ArrayList<Client>();
		bannedUsers = new ArrayList<String>();
		printText("Launching server...");
		
		new Thread(() -> {waitForPing();}).start();
		new Thread(() -> {waitForConnection();}).start();
    	
    }
    /**
	 * Process command from server
	 */
    protected void processInput(String input){
    	
    	processClientCommand(input);
    	
    	if(!input.startsWith("/")){
    		broadcastMessage(input, ADMIN);
    		return;
    	}
    	
    	printText(input);
    	String[] command = input.substring(1).split(" ");
    	
    	try{

    		switch(command[0]){

    		case "message": 
    			String message = "";
    			for(int i = 2; i < command.length; i++) message += command[i] + " ";
    			getClientByUsername(command[1]).sendMessage(message, ADMIN);
    			break;

    		case "ban": banUser(command[1]); break;

    		case "unban" : unbanUser(command[1]); break;

    		case "kick": kickUser(command[1]); break;

    		case "online": 

    			printText("Online users:");
    			for(Client c: connectedClients){
    				printText(c.getUsername());
    			}
    			break;



    		}

    	}catch(NoSuchElementException ne){
    		printText(ne.getMessage());
    	}
    	
    	
    }
    /**
	 * Process command from clients (limited, WIP)
	 */
    private void processClientCommand(String input){
    	
    	if(input.toLowerCase().contains("ai.disengage")){
			ai.disengage();
			this.broadcastMessage("AI program disengaged", SERVER);
			return;
		}
    	if(input.toLowerCase().contains("ai.engage")){
			ai.engage();
			this.broadcastMessage("AI program engaged", SERVER);
			return;
		}
		
    }
    /**	
     * Output text
	 */
    protected void printText(String message){
    	if(ui!=null){
    		ui.printText(message);
    	}else{
    		System.out.println(message);
    	}
    }
    /**
     * Check to see if username is permitted
     */
    private boolean usernameReserved(String username){
    	username = new String(username.toLowerCase());
    	
    	ArrayList <String> reserved = new ArrayList<String>();
    	reserved.add("admin");
    	reserved.add("adm1n");
    	reserved.add("admln");
    	reserved.add("server");
    	reserved.add("moderator");
    	
    	for(String reservedName : reserved){
    		if(username.contains(reservedName)){
    			return true;
    		}
    	}
    	
    	for(Client c : connectedClients){
			if(c.getUsername().toLowerCase().equals(username.toLowerCase())){
				return true;
			}
		}
    	
    	return false;
    	
    }
}

/**
 * Nice small client class for storing connections
 */
class Client{
	
	private InetAddress address;
	private int port;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	private Socket socket;
	private String username;
	private Color color;
		
 	public Client(Socket socket) throws IOException{
		this.address = socket.getInetAddress();
		this.port = socket.getPort();
		this.socket = socket;
		this.inputStream = new DataInputStream(socket.getInputStream());
		this.outputStream = new DataOutputStream(socket.getOutputStream());
		generateColor();
		
	}
 	
 	public Client(String username){
 		this.username = username;
 		this.color  = new Color(1, 1, 1, 1);
 	}
	
 	public Color getThemeColor(){
 		return color;
 	}
 	
	public void setUsername(String username){
		this.username = username;
	}
	
	public void sendMessage(String message, Client sender){
		if(message==null || message.trim().equals("") || sender==null){
    		return;
    	}
    	
		new Thread(() -> {

			try {
				this.getOutputStream().write((sender.getUsername() + ":" + message + ":" + sender.getThemeColor()).getBytes());
			}catch(SocketException se){
				this.disconnect();
			}catch (IOException e) {
				e.printStackTrace();
				this.disconnect();
			}
			
		}).start();
    	
	}
	
	public void disconnect(){
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getUsername(){
		return username;
	}
	
	public InetAddress getAddress(){
		return address;
	}
	
	public int getPort(){
		return port;
	}
	
	public DataInputStream getInputStream(){
		return this.inputStream;
	}
	
	public DataOutputStream getOutputStream(){
		return this.outputStream;
	}

	public Socket getSocket(){
		return this.socket;
	}
	
	private void generateColor(){
		
		ArrayList<Color> colors = new ArrayList<Color>();
		colors.add(Color.web("#F44366")); // red
		colors.add(Color.web("#E91E63")); // pink
		colors.add(Color.web("#9C27B0")); // purple
		colors.add(Color.web("#673AB7")); // deep purple
		colors.add(Color.web("#3F51B5")); // indigo
		colors.add(Color.web("#448AFF")); // blue
		colors.add(Color.web("#03A9F4")); // light blue
		colors.add(Color.web("#00BCD4")); // cyan
		colors.add(Color.web("#009688")); // teal
		colors.add(Color.web("#4CAF50")); // green
		colors.add(Color.web("#8BC34A")); // light green
		colors.add(Color.web("#FFEB3B")); // yellow
		colors.add(Color.web("#FF9800")); // orange
		colors.add(Color.web("#FF5722")); // deep orange
		colors.add(Color.web("#9E9E9E")); // grey
		colors.add(Color.web("#607D8B")); // blue grey
		
		this.color = colors.get(new Random().nextInt(colors.size()));
	}
}
