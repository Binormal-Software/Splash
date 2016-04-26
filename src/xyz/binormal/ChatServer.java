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
import java.util.Random;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ChatServer extends Application{
	
	private final Client SERVER = new Client("Server");
	private final Client ADMIN = new Client("Admin");
	private List<Client> connectedClients;
	
	private Socket tcpSocket;
	private ServerSocket serverSocket;
	private DatagramSocket udpSocket;
	
	private ChatServerUI ui;
	protected boolean serverStopped;
	
	
	private Stage mainWindow;
	
	public static void main(String[] args){
		launch(args);
	}
	
	public void start(Stage primaryStage) {
		
		if(!Globals.SERVER_HEADLESS){
			try{
				mainWindow = primaryStage;
				loadUI(mainWindow);}
			catch(IOException e){
				e.printStackTrace();
				System.err.println("Failed to load interface!");}
		}
		
		startServer();
		
	}
	
	private void loadUI(Stage stage) throws IOException{
		
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("Server.fxml"));
		Parent root = (Parent)fxmlLoader.load();
		ui = (ChatServerUI) fxmlLoader.getController();
		ui.setWindow(mainWindow);
		ui.setOwner(this);
		
		Scene mainScene = new Scene(root, 600, 400);
		mainScene.getStylesheets().add("Styles.css");
		
		stage.setTitle("Splash - Chat Server");
		stage.setScene(mainScene); 
		stage.setOnCloseRequest((e) -> {
				ui.printText("Stopping server...");
	    	    System.exit(0);
	    	});
		stage.show();
	}
	
	
    private void waitForPing(){
        
    	
        DatagramPacket packet;
    	
    	try{
            udpSocket = new DatagramSocket(Globals.DEFAULT_PORT);
            ui.printText("Listening for udp broadcasts (port " + Globals.DEFAULT_PORT + ")");
        }
        catch( Exception ex ){
        	ui.printText("Problem creating socket on port " + Globals.DEFAULT_PORT );
        }

        packet = new DatagramPacket (new byte[1], 1);

        while (!serverStopped){
            try{
            	
                udpSocket.receive (packet);
                
                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();
                
                ui.printText("Received ping from: " + clientAddress + ":" + clientPort);
                
                packet.setData (Globals.WELCOME_MSG.getBytes());
                udpSocket.send (packet);
                
            }
            catch (IOException ie){
                ie.printStackTrace();
            }
        }
    }

    private void waitForConnection(){
    	
 
		try {
		
			serverSocket = new ServerSocket(Globals.DEFAULT_PORT);
			ui.printText("Listening for tcp connections (port " + Globals.DEFAULT_PORT + ")");
			
			while(!serverStopped){
				
				tcpSocket = serverSocket.accept();
				Client newClient = new Client(tcpSocket);              
				new Thread(() -> {connectToClient(newClient); }).start();
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        
    	
    }
    
    private void connectToClient(Client client){
    	
    	String username = "Unknown";
    	
    	try {
    		
    		byte[] buffer;
    		boolean usernameTaken;
    		do{
    			buffer = new byte[128];
    			client.getInputStream().read(buffer);
    			username = new String(buffer).trim();
    			
    			usernameTaken = false;
    			for(Client c : connectedClients){
    				if(c.getUsername().toLowerCase().equals(username.toLowerCase()) 
    						|| username.toLowerCase().equals("server") 
    						|| username.toLowerCase().equals("admin"))
    					usernameTaken = true;
    			}
    			
    			client.getOutputStream().writeBoolean(usernameTaken);
    			
    		}while(usernameTaken);


    		client.setUsername(username);
			connectedClients.add(client);
			
			sendMessage(client.getUsername() + " jumped in! (" + connectedClients.size() + " currently online)", SERVER);
			
			while(client.getSocket().isConnected()){
								
				buffer = new byte[128];
				client.getInputStream().read(buffer);
				this.sendMessage(new String(buffer).trim(), client);
				
			}
			
		} catch (SocketException e) {
			ui.printText(username + " disconnected.");
		} catch (Exception e){
			ui.printText("Connection error! " + client.getUsername() + " disconnected.");
			e.printStackTrace();
		} finally{
			if(client.getUsername()!=null)
				sendMessage(client.getUsername() + " left.", SERVER);
			connectedClients.remove(client);
		}
    	
    	
    }

    private void sendMessage(String message, Client sender){
    	
    	ui.printText(sender.getUsername() + ": " + message);
    	
    	new Thread(() -> {

    		for(Client c : connectedClients){
    			try {
    				c.getOutputStream().write((sender.getUsername() + ":" + message + ":" + sender.getThemeColor()).getBytes());
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
    		}

    	}).start();
    	
    }
    
    private void kickUser(String user){
    	ui.printText("Kicking user '" + user + "'");
    	
    	for(Client c : connectedClients){
    		if(c.getUsername().toLowerCase().equals(user.toLowerCase())){
    			try {
					c.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	}
    	
    }
    
    
    protected void stopServer(){
    	sendMessage("Server is shutting down.", SERVER);
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
    
    protected void startServer(){

    	serverStopped = false;
    	
		connectedClients = new ArrayList<Client>();
		ui.printText("Launching server...");
		
		new Thread(() -> {waitForPing();}).start();
		new Thread(() -> {waitForConnection();}).start();
    	
    }
    
    protected void processInput(String input){
    	
    	if(!input.startsWith("/")){
    		sendMessage(input, ADMIN);
    		return;
    	}
    	
    	input = input.substring(1);
    	String[] command = input.split(" ");
    	
    	switch(command[0]){
    	
    	
    	case "kick": kickUser(command[1]); break;
    	case "online": 
    		
    		ui.printText("Online users:");
    		for(Client c: connectedClients){
    			ui.printText(c.getUsername());
    		}
    		break;
    	
    	}
    	
    	
    }
    
    
}

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
	
	public void disconnect() throws IOException{
		socket.close();
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
