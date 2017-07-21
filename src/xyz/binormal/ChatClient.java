package xyz.binormal;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import xyz.binormal.ChatClientUI.SceneMode;
import xyz.binormal.Connectable;
import xyz.binormal.UDPBroadcaster;
import xyz.binormal.InterfaceLocator;

//TODO bugs: none that I'm aware of

/**
 * Splash chat client class. Scans for and connects to ChatServer
 * @author Ryan Rodriguez
 */
public class ChatClient extends Application{

	private ChatClientUI ui;
	
	private Thread connectionThread;
	private String myUsername;
	private String nextMessage;
	private Boolean nameSet = false;
	private Boolean disconnectNow;
	
	private Socket socket;
	private DataInputStream fromServer;
	private DataOutputStream toServer;
	
	protected InetAddress myAddress;
	
	private Stage mainWindow;
	
	/**
	 * Boring stuff 
	 */
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		
		this.mainWindow = primaryStage;
		this.loadUI();
	
		initiateScan();
		
	}

	private void loadUI() throws IOException{

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("Client.fxml"));
		Parent root = (Parent)fxmlLoader.load();
		
		if(mainWindow!=null){
			ui = (ChatClientUI) fxmlLoader.getController();
			ui.setWindow(mainWindow);
			ui.setOwner(this);
		}

		Scene mainScene = new Scene(root, 480, 480);
		mainScene.getStylesheets().add("Styles.css");

		Font.loadFont(getClass().getResourceAsStream("/RobotoSlab.ttf"), 14);
		
		mainWindow.setTitle("Splash");
		mainWindow.setScene(mainScene); 
		mainWindow.setOnCloseRequest((e) -> {
			System.exit(0);
		});
		mainWindow.show();
	}

	/**
	 * UI accessible functions
	 */
	protected void initiateScan(){
		new Thread(() -> {
			listServers();
		}).start();
	}
	
	protected void initiateConnection(InetAddress address, int port){
		
		if(connectionThread==null)
			connectionThread = new Thread();
		
		
		if(!connectionThread.isAlive()){
			connectionThread = new Thread(() -> {
				connectToServer(address, port);
			});
			connectionThread.start();
		}else{
			System.err.println("An active connection is already open!");
		}
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
	/**
	 * Start connection with local server
	 */
	private void listServers(){

		try(UDPBroadcaster broadcaster = new UDPBroadcaster(Globals.HANDSHAKE_MSG)){

			this.disconnectNow = true;
			
			ui.loadScene(SceneMode.LOADING);
			ui.printText("Searching for Splash pools...");

			myAddress = myAddress();
			List<Connectable> serverList = broadcaster.findAddresses(Globals.DEFAULT_PORT, myAddress, 800);
			
			if(!serverList.isEmpty()){
				ui.showConnectionList(serverList);
			}else{
				ui.loadScene(SceneMode.RETRY_SEARCH);
				System.err.println("Failed to locate server.");
				ui.printText("None found. (This could be due to your network configuration.) You can try entering the address manually, if you know it.");
			}
			

		} catch(Exception e){
			ui.loadScene(SceneMode.RETRY_SEARCH);
			e.printStackTrace();
			ui.printText("Something went wrong while searching. Retry?");

		}

	}
	
	/**
	 * Connect directly to specified address
	 */
	private void connectToServer(InetAddress address, int port){
		
		try{

			disconnectNow = false;
			socket = new Socket(address, port);
			fromServer = new DataInputStream(socket.getInputStream());
			toServer = new DataOutputStream(socket.getOutputStream());

			if(!nameSet){
				ui.printText("Please enter a username:");
				ui.loadScene(SceneMode.ASK_USERNAME);

			}else{
				ui.printText("Reconnecting...");
				setUsername(myUsername);
			}

			while(!nameSet){
				Thread.sleep(100);
				if(disconnectNow){ 
					socket.close();
					return;
				}
			}

			ui.loadScene(SceneMode.CHAT);
			ui.printText("Online (" + myUsername + ")");

			while(socket.isConnected() && !disconnectNow){
				listenForMessage();
			}

			socket.close();

		}catch(SocketException se){
			System.out.println("Connection reset.");
		}catch(Exception e){
			ui.loadScene(SceneMode.RETRY_SEARCH);

			e.printStackTrace();
			ui.printText("Communication error!");
		}
	}
	/**
	 * Calls for any connection to be terminated
	 */
	protected void disconnectFromServer(){
		System.out.println("Closing streams.");
		this.disconnectNow=true;
		try {
			if(socket!=null)
				socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	 * Set and transmit user name to server
	 */
	protected void setUsername(String name){
		try {
			
			nameSet = false; 
			sendMessage(name); // send username to server
			
			switch(fromServer.readInt()){ // check status of username
			
			case Globals.USERNAME_AVAILABLE: 
				myUsername = name;
				System.out.println("myUsername = " + myUsername);
				nameSet = true; 
				break;
			
			case Globals.USERNAME_TAKEN:
				ui.printText("Whoops! You can't use that username. Please choose another.");
				ui.loadScene(SceneMode.ASK_USERNAME);
				break;
			
			case Globals.USERNAME_BANNED:
				ui.printText("You have been banned from this server.");
				ui.loadScene(SceneMode.NONE);
				break;
				
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Send message to server 
	 */
	protected void sendMessage(String message){
		
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
	
	
	
    
}
