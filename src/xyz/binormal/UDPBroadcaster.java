package xyz.binormal;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * <p>Class used to broadcast a UDP packet to all addresses in a specific range.</p>
 * 
 * @author Ryan Rodriguez
 * 
 */
public class UDPBroadcaster{
	
	private int socketTimeout = 5000;
    private InetAddress host;
    private DatagramSocket socket;
    private DatagramPacket packet;
    private String data;
    
    public InetAddress findAddress(int port, InetAddress hostAddress, int timeout) throws IOException{
    	socketTimeout = timeout;
    	return findAddress(port, hostAddress);
    }
    
    /**
     * 
     * @param port
     * @param hostAddress
     * @return address found by scan
     * @throws IOException
     */
    public InetAddress findAddress(int port, InetAddress hostAddress) throws IOException{

    	String hostPrefix = hostAddress.getHostAddress();
    	hostPrefix = hostPrefix.substring(0, hostPrefix.lastIndexOf('.'));

    	host = InetAddress.getByName(hostPrefix + ".255");
    	socket = new DatagramSocket (null);
    	socket.setSoTimeout(socketTimeout);
    	
    	packet = new DatagramPacket (new byte[36], 0, host, port);
    	socket.send (packet);
    	
    	packet.setLength(36);
    	socket.receive (packet);
    	socket.close();

    	data = new String(packet.getData());
    	return packet.getAddress();


    }
    
    public String getData(){
    	return data;
    }
}
