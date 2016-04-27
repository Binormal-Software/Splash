package xyz.binormal;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ChatServerUI implements Initializable {

	@FXML TextArea serverLog;
	@FXML TextField commandBox;
	@FXML Button toggleButton;
	Stage mainWindow;
	ChatServer chatServer;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// not really needed but whatever
	}

	public void setWindow(Stage mainWindow){
		this.mainWindow = mainWindow;
	}
	public void setOwner(ChatServer chatServer){
		this.chatServer = chatServer;
	}
	
	/**
	 * Print text into console and perhaps server window
	 */
	public void printText(String message){
    	
    	
    	Platform.runLater(new Runnable() {
    	    public void run() {
    	    	if(!Globals.SERVER_HEADLESS)
    	    		serverLog.appendText(message + "\r\n");    	    		
    	    	System.out.println(message);
    	    }
    	});
    	
    	
    }
	/**
	 * Get rid of window. Good luck getting it back
	 */
	public void hideWindow(){
    	mainWindow.hide();
    }
	/**
	 * I wonder what this one does...
	 */
    public void clearLog(){
    	serverLog.clear();
    }
    /**
	 * Start/Stop server
	 */
    public void toggleServer(){
    	if(chatServer.serverStopped){
			chatServer.startServer();
			toggleButton.setText("Stop Server");
		}else{
			chatServer.stopServer();
			toggleButton.setText("Start Server");
		}
    }
    /**
	 * Pass command to server and clear box
	 */
    public void processInput(){
    	chatServer.processInput(commandBox.getText());
    	commandBox.clear();
    }
    
}
