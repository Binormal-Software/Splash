package xyz.binormal;

import java.io.Serializable;

public class Message implements Serializable {

	private static final long serialVersionUID = 8152184430033840721L;
	public String text;
	public String sender;
	
	public Message(String text, String sender){
		this.text = text;
		this.sender = sender;
	}
	
}
