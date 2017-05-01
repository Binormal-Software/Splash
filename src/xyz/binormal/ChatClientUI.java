package xyz.binormal;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ResourceBundle;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * UI Controller for ChatClient.
 */
public class ChatClientUI implements Initializable{
	
	
	// new UI controls
	
	@FXML BorderPane borderWindow;
	@FXML ScrollPane chatPane;
	@FXML VBox loadingPane;
	@FXML VBox chatArea;
	@FXML VBox sideBar;
	@FXML VBox retryPane;
	@FXML HBox headerPane;
	@FXML HBox messageHBox;
	@FXML HBox usernameHBox;
	@FXML Label statusLabel;
	@FXML TextField messageTextField;
	@FXML TextField usernameTextField;
	@FXML TextField ipTextField;
	
	private String lastSender = "";
	//private Stage mainWindow;
	private ChatClient chatClient;
	

	@Override
	public void initialize(URL location, ResourceBundle resources) {
	
	}
	
	public void setWindow(Stage mainWindow){
		//this.mainWindow = mainWindow;
		borderWindow.getChildren().clear();
		attachNode(headerPane, "top");
		applyInOutTransition(loadingPane);
		chatArea.heightProperty().addListener((observable, oldVal, newVal) ->{
            chatPane.setVvalue(((Double) newVal).doubleValue());
        });
	}
	public void setOwner(ChatClient chatClient){
		this.chatClient = chatClient;
	}
	
	
	/**
	 * Dynamically add and remove nodes
	 */
	public enum SceneMode{
		LOADING, RETRY_SEARCH, ASK_USERNAME, CHAT, NONE
	}
	public void loadScene(SceneMode sceneMode){
		
		switch(sceneMode){
		
		case NONE: 
			hideNode("center", "in"); 
			break;
		
		case LOADING: 
			showNode(loadingPane, "center");
			hideNode("right", "in");
			hideNode("bottom", "in"); 
			break;
		
		case RETRY_SEARCH: 
			hideNode("center", "in"); 
			showNode(retryPane, "bottom"); 
			break;
		
		case ASK_USERNAME: 
			showNode(usernameHBox, "bottom");
			hideNode("center", "in"); 
			break;
	
		case CHAT: 
			showNode(messageHBox, "bottom");
			showNode(sideBar, "right");
			showNode(chatPane, "center"); 
			break;
		
		default: break;
		
		}
		
	}
	
	public void showConnectionList(){
		
	}
	
	public void showConnectionList(List<SplashPool> serverList){
		
		printText("Testing the water...");
		
		VBox serverBox = new VBox();
		serverBox.setAlignment(Pos.CENTER);
		
		for(SplashPool splashPool: serverList){
			Button connectButton = new Button(
					splashPool.getName() + " (version " + splashPool.serverVersion() + ")" 
					+ "\r\n" + "(" + splashPool.getInetAddress().getHostAddress() + ")");
			
			connectButton.setPrefHeight(50);
			connectButton.setAlignment(Pos.CENTER);
			
			connectButton.setOnAction(e -> {
					chatClient.initiateConnection(splashPool.getInetAddress(), Globals.DEFAULT_PORT);
			});
		
			serverBox.getChildren().add(connectButton);
			
		}
		
		Button refreshButton = new Button("Refresh");
		refreshButton.setOnAction(e -> {
			chatClient.initiateScan();
		});
		serverBox.getChildren().add(refreshButton);
		
		printText("Pool(s) found! Please select which to dive into:");
		showNode(serverBox, "center");
		
	}
	

	/**
	 * UI Controlled event handlers
	 */
	@FXML
    protected void handleMessageSend(ActionEvent e) {
        chatClient.sendMessage(messageTextField.getText());
        messageTextField.clear();
    }
	@FXML
    protected void handleUsername(ActionEvent e) {
        chatClient.setUsername(usernameTextField.getText());
        usernameTextField.clear();
    }
	@FXML
    protected void handleSearchAgain(ActionEvent e) {
		chatClient.disconnectFromServer();
		chatClient.initiateScan();
    }
	@FXML
    protected void handleDirectConnect(ActionEvent e) {
		try {
			chatClient.initiateConnection(InetAddress.getByName(ipTextField.getText()), Globals.DEFAULT_PORT);
			ipTextField.clear();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
    }
	
	
 	/**
	 * Print message into the console and status area
	 */
	public void printText(String message){

		Platform.runLater(new Runnable() {
			public void run() {
				statusLabel.setText(message);
				System.out.println(message);
			}
		});
	}
	/**
	 * Show new chat message in message pane
	 */
	protected void showMessage(String message, String sender, Color color, Boolean you){

		Platform.runLater(new Runnable() {
			public void run() {
				HBox messageBox = messageBubble(message, sender, color, you);
				chatArea.getChildren().add(messageBox);
				
			}
		});

	}
	
	/**
	 * Self-explanatory
	 */
	private void attachNode(Node node, String position){
		Platform.runLater(new Runnable() {
			public void run() {

				switch(position){

				case "top":
					borderWindow.setTop(node); break;

				case "center": 
					borderWindow.setCenter(node); break;

				case "bottom": 
					borderWindow.setBottom(node); break;

				case "left":
					borderWindow.setLeft(node); break;
					
				case "right":
					borderWindow.setRight(node); break;
				}

			}
		});
	}
	/**
	 * Attach node but with style ;)
	 */
	private void showNode(Node node, String position){
		attachNode(node,position);

		FadeTransition ft = new FadeTransition(Duration.millis(Globals.ANIMATION_DURATION), node);
		ft.setFromValue(0.0);
		ft.setToValue(1.0);
		ft.play();


		ScaleTransition st = new ScaleTransition(Duration.millis(Globals.ANIMATION_DURATION), node);
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
	/**
	 * Remove node... but with style ;)
	 */
	private void hideNode(String position, String inOut){

		Node node = null;
		switch(position){

		case "top": node = borderWindow.getTop(); break;
		case "center": node = borderWindow.getCenter(); break;
		case "bottom": node = borderWindow.getBottom(); break;
		case "left": node = borderWindow.getLeft(); break;
		case "right": node = borderWindow.getRight(); break;
		default: return;
		}

		FadeTransition ft = new FadeTransition(Duration.millis(Globals.ANIMATION_DURATION), node);
		ft.setFromValue(1.0);
		ft.setToValue(0.0);
		ft.setOnFinished(e -> {
			attachNode(null,position);
		});
		ft.play();

		ScaleTransition st = new ScaleTransition(Duration.millis(Globals.ANIMATION_DURATION), node);
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
	/**
	 * Also self-explanatory
	 */
	private void applyInOutTransition(Node node){

		ScaleTransition st = new ScaleTransition(Duration.millis(Globals.ANIMATION_DURATION*3), node);
		st.setFromX(0.7f);
		st.setFromY(0.7f);
		st.setToX(0.8f);
		st.setToY(0.8f);
		st.setInterpolator(Interpolator.EASE_BOTH);
		st.setCycleCount(Transition.INDEFINITE);
		st.setAutoReverse(true);
		st.play();

	}

	/**
	 * Convert RGBA to hexadecimal color code
	 */
	private String toHex(Color color){
		return "#" + (color + "").substring(2, 8);
	}


	private HBox messageBubble(String message, String sender, Color color, boolean you){

		Label messageLabel = new Label(message);
		messageLabel.setId("message-label");
		messageLabel.setWrapText(true);
		HBox.setMargin(messageLabel, new Insets(5));
		VBox.setMargin(messageLabel, new Insets(5));
		
		Color textColor;
		if(color.getBrightness() < 1){
			textColor = Color.WHITE;
		}else{
			textColor = Color.BLACK;
		}

		messageLabel.setStyle(messageLabel.getStyle() +
			" -fx-background-color: " + toHex(color) + "; "
			+ "-fx-text-fill: " + toHex(textColor) + "; ");

		HBox messageBox = new HBox();
		
		if(you){

			messageBox.setAlignment(Pos.TOP_RIGHT);
			messageLabel.setTranslateX(borderWindow.getWidth() + 50);
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

	

}
