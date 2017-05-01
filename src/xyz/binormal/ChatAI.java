package xyz.binormal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

/**
 * A small AI experiment for fun. Not really relevant to the project.
 * @author Ryan Rodriguez
 *
 */
public class ChatAI implements Runnable{

	private boolean active;
	
	
	public ChatAI(){
		active = false;
	}
	
	public boolean engaged(){
		return this.active;
	}
	
	public void engage(){
		active = true;
	}
	
	public void disengage(){
		active = false;
	}
	
	public String getResponse(String input){
		if(this.active){
			return thinkAbout(input);
		}else{
			return null;
		}
	}
	
	
	
	// TODO make smarter
	private String thinkAbout(String input){
		
		input = input.trim().toLowerCase();
		
		if(this.inputContainsType(input, secondPersonWords())){
			if(this.inputContainsType(input, goodWords())){
				return "Thank you!";
			}
			if(this.inputContainsType(input, badWords())){
				return "You're mean...";
			}
			
		}
		
		if(this.inputContainsType(input, firstPersonWords())){
			if(this.inputContainsType(input, goodWords())){
				return "Well... good for you.";
			}
			if(this.inputContainsType(input, badWords())){
				return "That's not true...";
			}
			
		}

		if(input.contains("what")){
			if(input.contains("time")){
				if(input.contains("is")){
					return ("The current time is " + new SimpleDateFormat("hh.mm").format(new Date()));
				}
			}
		}
		
		if(input.contains("why")){
			return "Well to be honest I'm not sure.";
		}


		return genericResponse();

	}
	
	private String genericResponse(){
		
		ArrayList<String> response = new ArrayList<String>();
		response.add("Ok.");
		response.add("Alright then.");
		response.add("Whatever you say!");
		
		return response.get(new Random().nextInt(response.size()));
	}
	
	private boolean inputContainsType(String input, ArrayList<String> type){
		
		for (String s : type){
			if(input.contains(s)){
				return true;
			}
		}
		
		return false;
		
	}
	
	private ArrayList<String> badWords(){
		
		ArrayList<String> response = new ArrayList<String>();
		response.add("dumb");
		response.add("stupid");
		response.add("lame");
		response.add("useless");
		
		return response;
	}
	
	private ArrayList<String> goodWords(){
		
		ArrayList<String> response = new ArrayList<String>();
		response.add("smart");
		response.add("good");
		response.add("nice");
		response.add("awesome");
		response.add("cool");
		
		return response;
	}
	
	private ArrayList<String> firstPersonWords(){
		
		ArrayList<String> response = new ArrayList<String>();
		response.add("i");
		response.add("i'm");
		response.add("im");
		response.add("me");
		
		return response;
	}
	
	private ArrayList<String> secondPersonWords(){
		
		ArrayList<String> response = new ArrayList<String>();
		response.add("you are");
		response.add("u are");
		response.add("you r");
		response.add("your");
		response.add("you're");
		response.add("youre");
		
		return response;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
}
