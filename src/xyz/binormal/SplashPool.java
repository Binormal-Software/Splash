package xyz.binormal;

import java.net.InetAddress;

public class SplashPool {

	private InetAddress poolAddress;
	private String name;
	private String appVersion;
	
	public SplashPool(InetAddress address, String name, String appVersion){
		this.poolAddress = address;
		this.name = name;
		this.appVersion = appVersion;
	}
	
	public String getName(){
		return this.name;
	}
	
	public InetAddress getInetAddress(){
		return this.poolAddress;
	}
	
	public String serverVersion(){
		return this.appVersion;
	}
}
