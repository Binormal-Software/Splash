package xyz.binormal;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import javafx.application.Application;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import xyz.binormal.ChatClientUI.SceneMode;

//TODO bugs: none that I'm aware of

/**
 * Splash chat client class. Scans for and connects to ChatServer
 * @author Ryan Rodriguez
 */
public class ChatClient extends Application{

	private String myUsername;
	private String nextMessage;
	private String manualip;
	
	private Boolean nameSet = false;
	private Boolean retry = false;
	
	private DataInputStream fromServer;
	private DataOutputStream toServer;
	protected InetAddress myAddress;
	private ChatClientUI ui;
	
	/**
	 * Boring stuff 
	 */
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		
		
		ui = new ChatClientUI(primaryStage, this);
		
		primaryStage.setTitle("Splash");
		primaryStage.setScene(ui.getScene()); 
		primaryStage.setOnCloseRequest((e) -> {System.exit(0);});
		primaryStage.show();

		ui.setSceneMode(SceneMode.INTRO);
	}
	
	/**
	 * Both hook into InterfaceLocator and UDPBroadcaster to find addresses
	 */
	private InetAddress myAddress() throws IOException{
		
		InterfaceLocator il;
		InetAddress myAddress;
		
		il = new InterfaceLocator();

		myAddress = il.getHostAddress();
		System.out.println("Found address " + myAddress.getHostAddress());
		
		return myAddress;
		
	}
	
	private InetAddress searchForServer(InetAddress myAddress) throws IOException{
		
		UDPBroadcaster bcast = new UDPBroadcaster();
		InetAddress serverAddress;
		
		serverAddress = bcast.findAddress(Globals.DEFAULT_PORT, myAddress, 3600);
		System.out.println("Server located at " + serverAddress.getHostAddress() + ".");
		
		return serverAddress;
		
	}
	
	/**
	 * Start connection with local server
	 */
	protected void startLocalConnection(){

		while(true){

			try {
				
				if(manualip==null){
					
					ui.setSceneMode(SceneMode.LOADING);
					ui.printText("Searching for Splash pools...");
					
					myAddress = myAddress();
					InetAddress serverAddress = searchForServer(myAddress);
					
					ui.printText("Connecting...");
					connectToServer(serverAddress, Globals.DEFAULT_PORT);
					
				}else{
					connectToServer(InetAddress.getByName(manualip), Globals.DEFAULT_PORT);
				}

			} catch(Exception e){
				
				ui.setSceneMode(SceneMode.RETRY);
				
				if(e instanceof SocketTimeoutException){
					System.err.println("Failed to locate server.");
					ui.printText("None found. (This could be due to your network configuration.) Try entering the address manually.");
			    }else{
			    	e.printStackTrace();
			    	ui.printText("Communication error. Retry?");
			    }
				
				retry = false;
				
				try {
					while(!retry){
						Thread.sleep(100);
					}
				} catch (InterruptedException e1) {
					e.printStackTrace();
				}
				
				
				continue;
				
			}
			
			
		}
	}
	/**
	 * Start connection with Globals.GLOBAL_HOST
	 */
	protected void startRemoteConnection(){

		try {
			connectToServer(InetAddress.getByName(Globals.GLOBAL_HOST), Globals.DEFAULT_PORT);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			ui.printText("Whoops! Looks like the server is offline.");
			//hideNode("bottom", "in");
		}


	}
	/**
	 * Called from ui, try again fool
	 */
	protected void retryConnection(String manualip){
		this.retry = true;
		this.manualip = manualip;
	}
	
	/**
	 * Initiate new connection!
	 */
	private void connectToServer(InetAddress address, int port) throws IOException, InterruptedException{
		
		Socket socket = new Socket(address, port);
		fromServer = new DataInputStream(socket.getInputStream());
		toServer = new DataOutputStream(socket.getOutputStream());
		
		if(!nameSet){
			ui.printText("Please enter a username:");
			ui.setSceneMode(SceneMode.TEXT_ONLY);
			
			while(!nameSet){
				Thread.sleep(100);
			}
			
		}
		
		ui.setSceneMode(SceneMode.CHAT);
		ui.printText("Online (" + myUsername + ")");

		while(socket.isConnected()){
			listenForMessage();
		}

		socket.close();
		
	}
	/**
	 * Wait for message from server
	 */
	private void listenForMessage() throws IOException{
		

		byte[] buffer = new byte[128];
		fromServer.read(buffer);

		String message = new String(buffer).trim();
		String sender = message.substring(0, message.indexOf(':'));
		String colorString = message.substring(message.lastIndexOf(':')+1, message.length());
		Color color = Color.web(colorString);
		
		message = message.substring(message.indexOf(':')+1, message.lastIndexOf(':'));

		
		
		Boolean you = false;
		if(sender.equals(myUsername)){
			sender = "";
			you = true;
		}

		ui.showMessage(message, sender, color, you);


		
	}
	/**
	 * Called by UI to handle text input
	 */
	protected void handleInput(String message){
		
		if(!nameSet){
			
			try {
				
				sendMessage(message); // send username to server
				
				if(!usernameTaken()){
					myUsername = message;
					System.out.println("myUsername = " + myUsername);
					nameSet = true;
				}else{
					ui.printText("Whoops! You can't use that username. Please choose another.");
				}
				
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}else{
			sendMessage(message);
		}
		
		
	}
	/**
	 * Send message to server 
	 */
	private void sendMessage(String message){
		
		int maxLength = (message.length() < Globals.MAX_SIZE)?message.length():Globals.MAX_SIZE;
		nextMessage = message.substring(0, maxLength);
		
		new Thread(() -> {
			try {
				toServer.write(nextMessage.getBytes());
			} catch (IOException e) {
				ui.printText("Failed to send message!");
				e.printStackTrace();
			}
		}).start();
		
    }
	/**
	 * Check with server if username is taken
	 */	
	private boolean usernameTaken() throws IOException{
		
		if (fromServer.readBoolean()){
			return true;
		}else{
			nameSet = true;
			return false;
		}
		
	}
	
	
    
}
