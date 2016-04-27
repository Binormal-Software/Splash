package xyz.binormal;

/**
 * Start ChatServer without any JavaFX dependencies. Useful for use on remote servers with no display/limited resources
 */
public class LaunchServerHeadless {

	public static void main(String[] args) {
		new ChatServer().startServer();
	}

}
