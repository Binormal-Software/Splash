package xyz.binormal;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ChatClient extends Application{

	// constants
	private final int MAX_SIZE = 100;
	private final int ANIMATION_DURATION = 400;
	
	// other crap
	private String username;
	private String nextMessage;
	private String lastSender = "";
	private Boolean nameSet = false;
	private Boolean retry = false;
	private String manualip;
	private DataInputStream fromServer;
	private DataOutputStream toServer;
	private InetAddress myAddress;
	
	// UI Controls
	private Label statusLabel;
	private VBox chatArea;
	private ScrollPane chatAreaBox;
	private BorderPane mainWindow;
	private VBox loadingPane;
	private VBox introPane;
	private HBox textBox;
	private HBox headerPane;
	private ScrollPane chatPane;
	
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		
		headerPane = headerPane();
		mainWindow = mainWindow(primaryStage.widthProperty(), primaryStage.heightProperty());
		loadingPane = loadingPane();
		introPane = introPane();
		textBox = textBox();
		chatPane = chatPane();
		
		Scene scene = new Scene(mainWindow, 480, 480);
		
		//Font.loadFont(getClass().getResourceAsStream("/TrashHand.TTF"), 14);
		Font.loadFont(getClass().getResourceAsStream("/ROBOTOSLAB-REGULAR.TTF"), 14);
		scene.getStylesheets().add("Styles.css");
		
		primaryStage.setTitle("Splash");
		
		primaryStage.setScene(scene); 
		primaryStage.setOnCloseRequest((e) -> {
	    	    System.exit(0);
	    	});
		primaryStage.show();

		
		showNode(introPane, "center");
		
	}
	
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
	
	private void startLocalConnection(){

		while(true){

			try {
				
				if(manualip==null){
					showNode(loadingPane, "center");
					printText("Searching for Splash pools...");
					
					myAddress = myAddress();
					InetAddress serverAddress = searchForServer(myAddress);
					
					printText("Connecting...");
					connectToServer(serverAddress, Globals.DEFAULT_PORT);
					
				}else{
					connectToServer(InetAddress.getByName(manualip), Globals.DEFAULT_PORT);
				}

			} catch(Exception e){
				
				hideNode("center", "in");
				
				if(e instanceof SocketTimeoutException){
					System.err.println("Failed to locate server.");
					printText("None found. (This could be due to your network configuration.) Try entering the address manually.");
			    }else{
			    	e.printStackTrace();
			    	printText("Communication error. Retry?");
			    }
				
				showNode(retryPane(myAddress.getHostAddress()), "bottom");
				retry = false;
				
				try {
					while(!retry){
						Thread.sleep(100);
					}
				} catch (InterruptedException e1) {
					e.printStackTrace();
				}
				
				hideNode("bottom", "in");
				continue;
				
			}
			
			
		}
	}

	private void startRemoteConnection(){

		try {
			connectToServer(InetAddress.getByName(Globals.GLOBAL_HOST), Globals.DEFAULT_PORT);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			printText("Communication error.");
			hideNode("bottom", "in");
		}


	}
	
	private void connectToServer(InetAddress address, int port) throws IOException, InterruptedException{
		
		
		if(!nameSet){
			hideNode("center", "out");
			printText("Please enter a username:");
		}
		
		showNode(textBox, "bottom");
		

		while(!nameSet){
			Thread.sleep(100);
		}

		showNode(chatPane, "center");	
		
		printText("Diving in...");

		Socket socket = new Socket(address, port);

		fromServer = new
				DataInputStream(socket.getInputStream());
		toServer = new
				DataOutputStream(socket.getOutputStream());

		sendMessage(username);

		while(usernameTaken()){
			nameSet = false;
			printText("Whoops! You can't use that username. Please choose another.");
		}

		printText("Online (" + username + ")");

		while(socket.isConnected()){
			listenForMessage();
		}

		socket.close();
		
		
		
	}
	
	private void listenForMessage() throws IOException{
		

		byte[] buffer = new byte[128];
		fromServer.read(buffer);

		String message = new String(buffer).trim();
		String sender = message.substring(0, message.indexOf(':'));
		String colorString = message.substring(message.lastIndexOf(':')+1, message.length());
		Color color = Color.web(colorString);
		
		message = message.substring(message.indexOf(':')+1, message.lastIndexOf(':'));

		
		
		Boolean you = false;
		if(sender.equals(username)){
			sender = "";
			you = true;
		}

		showMessage(message, sender, color, you);


		
	}
	
	private void sendMessage(String message){
		
		int maxLength = (message.length() < MAX_SIZE)?message.length():MAX_SIZE;
		nextMessage = message.substring(0, maxLength);
		
		if(!nameSet){
			username = message;
			nameSet = true;
			return;
		}
		
		new Thread(() -> {
			try {
				toServer.write(nextMessage.getBytes());
			} catch (IOException e) {
				printText("Failed to send message!");
				e.printStackTrace();
			}
		}).start();
		
    }
    	
	private boolean usernameTaken() throws IOException{
		
		return fromServer.readBoolean();
		
	}
	
	
    private void printText(String message){
    	
    	Platform.runLater(new Runnable() {
    	    public void run() {
    	    	statusLabel.setText(message);
    	    	System.out.println(message);
    	    }
    	});
    }
	
    private void showMessage(String message, String sender, Color color, Boolean you){
    	
    	Platform.runLater(new Runnable() {
    	    public void run() {
    	    	HBox messageBox = messageBox(message, sender, color, you);
    	    	chatArea.getChildren().add(messageBox);
    	    }
    	});
    	
    	
    }
    
    private void attachNode(Node node, String position){
    	Platform.runLater(new Runnable() {
    	    public void run() {
    	    	
    	    	switch(position){
    	    	
    	    	case "top":
    	    		mainWindow.setTop(node); break;
    	    	
    	    	case "center": 
    	    		mainWindow.setCenter(node); break;
    	    		    	
    	    	case "bottom": 
    	    		mainWindow.setBottom(node); break;
    	    		
    	    	}
    	    	
    	    }
    	});
    }
    
    private void showNode(Node node, String position){
    	attachNode(node,position);
    	
    	FadeTransition ft = new FadeTransition(Duration.millis(ANIMATION_DURATION), node);
		ft.setFromValue(0.0);
		ft.setToValue(1.0);
		ft.play();
		
    	
    	ScaleTransition st = new ScaleTransition(Duration.millis(ANIMATION_DURATION), node);
    	st.setFromX(0);
        st.setFromY(0);
        st.setToX(1);
        st.setToY(1);
        st.setInterpolator(Interpolator.EASE_BOTH);
        st.setOnFinished(e -> {
        	attachNode(node,position);
		});
    	st.play();
    }
    
    private void hideNode(String position, String inOut){
    	
    	Node node = null;
    	switch(position){
    	
    	case "top": node = mainWindow.getTop(); break;
    	case "center": node = mainWindow.getCenter(); break;
    	case "bottom": node = mainWindow.getBottom(); break;
    	default: return;
    	}
    	
    	FadeTransition ft = new FadeTransition(Duration.millis(ANIMATION_DURATION), node);
		ft.setFromValue(1.0);
		ft.setToValue(0.0);
		ft.setOnFinished(e -> {
			attachNode(null,position);
		});
		ft.play();
		
		ScaleTransition st = new ScaleTransition(Duration.millis(ANIMATION_DURATION), node);
		if(inOut.equals("out")){
			st.setFromX(1);
	        st.setFromY(1);
	        st.setToX(10);
	        st.setToY(10);
		}else{
			st.setFromX(1);
	        st.setFromY(1);
	        st.setToX(0);
	        st.setToY(0);
		}
    	
        st.setInterpolator(Interpolator.EASE_IN);
    	st.play();
    }
    
    private void applyInOutTransition(Node node){
    	
    	ScaleTransition st = new ScaleTransition(Duration.millis(ANIMATION_DURATION*3), node);
    	st.setFromX(0.7f);
        st.setFromY(0.7f);
        st.setToX(0.8f);
        st.setToY(0.8f);
        st.setInterpolator(Interpolator.EASE_BOTH);
        st.setCycleCount(Transition.INDEFINITE);
        st.setAutoReverse(true);
    	st.play();
    	
    }
    
    private String toHex(Color color){
    	return "#" + (color + "").substring(2, 8);
    }
    
    
 	private BorderPane mainWindow
	(ReadOnlyDoubleProperty windowWidth, ReadOnlyDoubleProperty windowHeight){
	    	
	    	mainWindow = new BorderPane();
	    	mainWindow.setId("window-dark");
	    	mainWindow.prefWidthProperty().bind(windowWidth);
	    	mainWindow.prefHeightProperty().bind(windowHeight);
	    	
	    	mainWindow.setTop(headerPane);
	    	headerPane.toFront();
	    	
	    	return mainWindow;
	    }
 	
 	private HBox headerPane(){
 		
    	statusLabel = new Label();
    	statusLabel.setId("status-label");
    	statusLabel.setWrapText(true);
    	
    	Button button = new Button("Button");
    	
 		HBox headerPane = new HBox();
 		headerPane.setAlignment(Pos.CENTER);
 		headerPane.setId("header-pane");
 		headerPane.setMinHeight(50);
 		headerPane.getChildren().addAll(statusLabel);
 		//headerPane.setEffect(dropShadow());
 		statusLabel.toFront();
 		return headerPane;
 	}
 	
	private HBox messageBox(String message, String sender, Color color, boolean you){
		
		Label messageLabel = new Label(message);
		HBox.setMargin(messageLabel, new Insets(5));
		VBox.setMargin(messageLabel, new Insets(5));
		messageLabel.setWrapText(true);
		messageLabel.setEffect(dropShadow());
		
		HBox messageBox = new HBox();
		
		Color textColor;
		if(color.getBrightness() < 1){
			textColor = Color.WHITE;
		}else{
			textColor = Color.BLACK;
		}
		
		messageLabel.setStyle(
				"-fx-background-color: " + toHex(color) + "; "
				+ "-fx-text-fill: " + toHex(textColor) + "; ");
		
		if(you){
			
			messageBox.setAlignment(Pos.TOP_RIGHT);
			messageLabel.setTranslateX(mainWindow.getWidth() + 50);
			messageLabel.setTranslateY(20);
			
			messageBox.getChildren().add(messageLabel);
			
		}else{

			
			messageBox.setAlignment(Pos.TOP_LEFT);
			messageLabel.setTranslateX(-50);
			messageLabel.setTranslateY(20);
			
			if(!lastSender.equals(sender)){
				
				VBox container = new VBox();
				
				Label senderLabel = new Label(sender + ":");
				senderLabel.setId("sender-label");
				
				container.getChildren().addAll(senderLabel, messageLabel);
				messageBox.getChildren().add(container);
			}else{
				
				messageBox.getChildren().add(messageLabel);
			}
		}
		
		lastSender = sender;
		
		
		new Animator().addAnimation(messageLabel);
		return messageBox;
	}
 
	private VBox introPane(){
		VBox introBox = new VBox();
		introBox.setAlignment(Pos.CENTER);
		
		Label label = new Label("Welcome to Splash Chat!");
		label.setId("status-label");
		
		Image image = new Image("splash logo.png");
		ImageView icon = new ImageView();
		icon.setImage(image);
		icon.setEffect(dropShadow());
		applyInOutTransition(icon);
		
		HBox buttonBox = new HBox();
		buttonBox.setAlignment(Pos.CENTER);
		Button local = new Button("Join Local Pool");
		local.setPrefHeight(50);
		local.setOnAction(e -> {
				new Thread(() -> {
					startLocalConnection();}).start();
		});
		
		Button remote = new Button("Join Online Pool");
		remote.setPrefHeight(50);
		remote.setOnAction(e -> {
				new Thread(() -> {
					startRemoteConnection();}).start();
		});
		
		buttonBox.getChildren().addAll(local, remote);
		introBox.getChildren().addAll(label, icon, buttonBox);
		
		
		return introBox;
	}
	
	private VBox loadingPane(){
		VBox loadingBox = new VBox();
		Image image = new Image("loading.gif");
		ImageView loading = new ImageView();
		loadingBox.setStyle("-fx-background-color: #1c9cf2;");
		loadingBox.setAlignment(Pos.CENTER);
		loading.setImage(image);
		loading.setFitWidth(480);
		loading.setFitHeight(360);
		
		applyInOutTransition(loading);
		
		loadingBox.getChildren().add(loading);
		return loadingBox;
	}
	
	private VBox retryPane(String ip){
		
		
		VBox retryPane = new VBox();
		retryPane.setPadding(new Insets(20));
		
		TextField addressField = new TextField();
		addressField.setPromptText("Enter ip address, like " + ip);
		
		EventHandler<ActionEvent> event = new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
            	String newip = addressField.getText();
    			if(newip!=null && !newip.trim().equals("")){
    				retry = true;
    		    	manualip = newip;
    			}
            }
        };
		
		addressField.setOnAction(event);
		
		Button retryButton = new Button("Search Again");
		retryButton.setPrefHeight(50);
		retryButton.prefWidthProperty().bind(mainWindow.widthProperty());
		retryButton.setOnAction((e) -> {
	    	  retry = true;
	    	  manualip = null;
	    });
		
		Button manual = new Button("Connect Manually");
		manual.setPrefHeight(50);
		manual.prefWidthProperty().bind(mainWindow.widthProperty());
		manual.setOnAction(event);
		
		HBox buttonBox = new HBox();
		buttonBox.getChildren().addAll(retryButton, manual);
		
		retryPane.getChildren().addAll(addressField, buttonBox);
		return retryPane;
	}
	
	private HBox textBox(){
		HBox textBox = new HBox();
		textBox.setAlignment(Pos.CENTER);
		textBox.setPadding(new Insets(5));
		
		
		
		TextField newCommand = new TextField();
    	newCommand.setPrefHeight(20);
    	newCommand.prefWidthProperty().bind(mainWindow.widthProperty().divide(1.5));
    	newCommand.setPromptText("Enter text");
    	
    	Button button = new Button("Send");//("\u27a4");
    	button.setFont(Font.font(java.awt.Font.SANS_SERIF, 40));
    	button.setPadding(new Insets(0));
    	button.prefHeightProperty().bind(newCommand.heightProperty());
    	
    	
    	EventHandler<ActionEvent> submit = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            	if(!newCommand.getText().trim().equals("")){
        			sendMessage(newCommand.getText());
        			newCommand.clear();
        		}
            }
        };
        
        newCommand.setOnAction(submit);
        button.setOnAction(submit);
        
        Platform.runLater(new Runnable() {
    	    public void run() {
    	        newCommand.requestFocus();
    	    }
    	});
        
        
    	textBox.getChildren().addAll(newCommand, button);
    	return textBox;
		
	}
	
	private ScrollPane chatPane(){
		chatArea = new VBox();
    	
    	chatAreaBox = new ScrollPane();
    	
    	chatArea.heightProperty().addListener((observable, oldVal, newVal) ->{
            chatAreaBox.setVvalue(((Double) newVal).doubleValue());
        });
    	
    	chatArea.prefWidthProperty().bind(chatAreaBox.widthProperty().subtract(20));
    	chatAreaBox.setContent(chatArea);
    	
    	return chatAreaBox;
	}
	
	private DropShadow dropShadow(){
		
		DropShadow ds = new DropShadow();
		ds.setRadius(2);
		ds.setOffsetX(0f);
		ds.setOffsetY(2f);
		ds.setColor(Color.color(0, 0, 0, 0.2f));
		
		return ds;
	}
	
	
}
