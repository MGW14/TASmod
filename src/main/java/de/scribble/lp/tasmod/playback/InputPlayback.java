package de.scribble.lp.tasmod.playback;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.scribble.lp.tasmod.ClientProxy;
import de.scribble.lp.tasmod.tutorial.TutorialHandler;
import de.scribble.lp.tasmod.virtual.VirtualKeyboardEvent;
import de.scribble.lp.tasmod.virtual.VirtualMouseAndKeyboard;
import de.scribble.lp.tasmod.virtual.VirtualMouseEvent;
import de.scribble.lp.tasmod.virtual.VirtualSubticks;
import net.minecraft.client.Minecraft;

/**
 * Takes a file and produces VirtualEvents that can be presses in the VirtualMouseAndKeyboard
 * 
 * @author ScribbleLP
 *
 */
public class InputPlayback {
	private static boolean playingback;
	private static Logger logger= LogManager.getLogger("InputPlayback");
	private static File fileLocation;
	private static String Filename;
	public static int playbackIndex=-1;
	private static List<TickFrame> inputList=new ArrayList<TickFrame>();
	
	private static List<VirtualSubticks> subtickList= new ArrayList<VirtualSubticks>();
	public static int subtickPlaybackindex=-1;
	
	public static void startPlayback(File file, String filename) {
		if(!playingback) {
			Filename=filename;
			fileLocation=file;
			inputList=new ArrayList<TickFrame>();
			try {
				readInputs();
			} catch (IOException e) {
				logger.error("Cannot read the tasfile "+filename);
				e.printStackTrace();
				return;
			}
			playingback=true;
			playbackIndex=-1;
			subtickPlaybackindex=-1;
			TutorialHandler tutorial= ClientProxy.getPlaybackTutorial();
			if(tutorial.istutorial&&tutorial.getState()==6) {
				tutorial.advanceState();
			}
//			Minecraft.getMinecraft().randommanager.setEntityRandomnessAll(0);
//			RandomLogger.startRandomLogging();
		}else {
			logger.error("There is a playback already running!");
		}
	}
	public static void stopPlayback() {
		if(isPlayingback()) {
			TutorialHandler tutorial= ClientProxy.getPlaybackTutorial();
			if(tutorial.istutorial&&tutorial.getState()==7) {
				tutorial.advanceState();
			}
			playingback=false;
			inputList=new ArrayList<TickFrame>();
			subtickList=new ArrayList<VirtualSubticks>();
			VirtualMouseAndKeyboard.unpressEverything();
//			RandomLogger.stopRandomLogging();
		}
	}
	/**
	 * Increments the playback tick which keeps track of which keyboard event should be read by the virtual mouse and keyboard.<br>
	 * Also stops the playback if finished
	 */
	public static void nextPlaybackTick() {
		if(isPlayingback()) {
			if(playbackIndex==inputList.size()-1) {
				logger.info("Ticks finished playback");
				stopPlayback();
			}
			playbackIndex++;
		}
	}
	public static void nextPlaybackSubtick() {
		if(isPlayingback()) {
			if(subtickPlaybackindex==subtickList.size()-1) {
				logger.info("Subticks finished playback");
				stopPlayback();
			}
			subtickPlaybackindex++;
		}
	}
	public static List<TickFrame> getInputList(){
		return inputList;
	}
	
	public static List<VirtualKeyboardEvent> getCurrentKeyEvent(){
		List<VirtualKeyboardEvent> list = new ArrayList<VirtualKeyboardEvent>();
		if(!inputList.isEmpty()) {
			return inputList.get(playbackIndex).getKeyboarEvent();
		}else return list;
	}
	
	public static List<VirtualMouseEvent> getCurrentMouseEvent(){
		List<VirtualMouseEvent> list = new ArrayList<VirtualMouseEvent>();
		if(!inputList.isEmpty()) {
			return inputList.get(playbackIndex).getMouseEvent();
		}else return list;
	}
	
	private static void readInputs() throws IOException {
		//Check if Filelocation is not null
		if(fileLocation!=null) {
			//Create a bunch of variables
			BufferedReader buff = new BufferedReader(new FileReader(fileLocation));
			String wholeLine="";
			int linecounter=0;
			//Read the lines until the line is null
			while((wholeLine=buff.readLine()) != null) {
				//Increment the linecounter
				linecounter++;
				//Sets the tp location for the start of the TAS TODO Maybe remove this in the future
				if(wholeLine.startsWith("#StartLocation:")) {
					tpPlayer(wholeLine, linecounter);
					continue;
				//Skip comments
				}else if(wholeLine.startsWith("#")) {
					continue;
				//Read subticks
				}else if(wholeLine.startsWith("S")) {
					String[] sections=wholeLine.split("\\|");
					sections[0]=sections[0].replace("S", "");
					int tick=getTickCounter(sections, linecounter, 0);
					float pitch=getSubtickPitch(sections, linecounter);
					float yaw=getSubtickYaw(sections, linecounter);
					subtickList.add(new VirtualSubticks(tick, pitch, yaw));
				//Read ticks
				}else {
					//Split the whole line into sections seperated by a "|"
					String[] sections=wholeLine.split("\\|");
					int tick=getTickCounter(sections, linecounter, 0);
					List<Integer> keyboardPresses=getKeyboardPresses(sections, linecounter, 1);
					List<Boolean> keyboardStates=getKeyboardStates(sections, linecounter, 2);
					List<Character> charTyped= getCharTyped(sections, linecounter, 4);
					List<Integer> mouseButtonPresses=getMouseButtonPresses(sections, linecounter, 5);
					List<Boolean> mouseStates=getMouseStates(sections, linecounter, 6);
					List<Integer> scrollWheelDeltas=getScrollWheelDeltas(sections, linecounter, 7);
					List<Integer> mouseX=getMouseX(sections, linecounter, 8);
					List<Integer> mouseY=getMouseY(sections, linecounter, 8);
					List<Integer> slotID= getSlotID(sections, linecounter, 9);
					
					List<VirtualKeyboardEvent> keyboardEvents = new ArrayList<VirtualKeyboardEvent>();
					//Making sure keyboard presses states and chars are the same size, since there is really no reason why they shouldn't
					if(keyboardPresses.size()==keyboardStates.size()&&keyboardPresses.size()==charTyped.size()) {
						//Adding all the keyboard presses into a keyboard event. This is the same thing as in VirtualKeyboard events
						for (int i = 0; i < keyboardPresses.size(); i++) {
							keyboardEvents.add(new VirtualKeyboardEvent(keyboardPresses.get(i), keyboardStates.get(i), charTyped.get(i)));
						}
						
					}else {
						//Errorhandling
						buff.close();
						logger.error("Error while reading 'Everything related to keyboard' in " + Filename + " in line " + linecounter+". Keyboard, KeyboardState and CharTyped always need to have the same number of inputs");
						throw new IOException();
					}
					
					//Same as the keyboard but with the mouse
					List<VirtualMouseEvent> mouseEvents = new ArrayList<VirtualMouseEvent>();
					if(mouseButtonPresses.size()==mouseStates.size()&&mouseButtonPresses.size()==scrollWheelDeltas.size()&&mouseButtonPresses.size()==mouseX.size()&&mouseButtonPresses.size()==mouseY.size()&&mouseButtonPresses.size()==slotID.size()) {
						
						for (int i = 0; i < mouseButtonPresses.size(); i++) {
							mouseEvents.add(new VirtualMouseEvent(mouseButtonPresses.get(i), mouseStates.get(i), scrollWheelDeltas.get(i), mouseX.get(i), mouseY.get(i), slotID.get(i)));
						}
						
					}else {
						buff.close();
						logger.error("Error while reading 'Everything related to mouse' in " + Filename + " in line " + linecounter+". Mouse, MouseStates, ScrollWheel, DeltaX, DeltaY, MouseX, MouseY and slotID always need to have the same number of inputs");
						throw new IOException();
					}
					//Adding every keyboard an mouse event from the current tick into yet another list that contains every input from every tick 
					inputList.add(new TickFrame(tick, keyboardEvents, mouseEvents));
				}
			}
			buff.close();
		}
	}
	
	private static void tpPlayer(String wholeLine, int linecounter) throws IOException {
		wholeLine=wholeLine.replace("#StartLocation:", "");
		String[] section = wholeLine.split(",");
		if(section.length<5) {
			logger.error("Error while reading tickcounter in "+Filename+" in line "+linecounter+". Incorrect tp position");
			throw new IOException();
		}
		Minecraft.getMinecraft().player.sendChatMessage("/tp "+section[0]+" "+section[1]+" "+section[2]+" "+section[3]+" "+section[4]); //I don't care anymore TODO
		
	}
	/*The following methods take different sections of the line and interpret them. Returns all inputs as a list*/
	private static int getTickCounter(String[] sections, int linecounter, int sectionnumber) throws IOException {
		//System.out.println(sections[0]); //TODO
		if(sections[sectionnumber].isEmpty()) {
			logger.error("Error while reading tickcounter in "+Filename+ " in line "+linecounter+"and the input in position "+1+" from the left. The input is empty!");
			throw new IOException();
		}else {
			int counter=getNumber("tickcounter", sections[sectionnumber], linecounter, 0);
			return counter;
		}
		
	}
	
	private static List<Integer> getKeyboardPresses(String[] sections, int linecounter, int sectionnumber) throws IOException {
		List<Integer> out = new ArrayList<Integer>();
		if(!sections[sectionnumber].isEmpty()) {
			if(sections[sectionnumber].startsWith("Keyboard:")) {
				sections[sectionnumber]=sections[sectionnumber].replace("Keyboard:", "");
			}
			String[] keys= sections[sectionnumber].split(",");
			for (int i = 0; i < keys.length; i++) {
				//System.out.println(linecounter+" "+keys[i]); //TODO
				if(isNumber(keys[i])) {
					out.add(Integer.parseInt(keys[i]));
				}else {
					if(VirtualMouseAndKeyboard.getKeyCodeFromKeyName(keys[i])!=-1) {
						out.add(VirtualMouseAndKeyboard.getKeyCodeFromKeyName(keys[i]));
					} else{
						logger.error("Error while reading 'Keyboard' in "+Filename+ " in line "+linecounter+" and the input in position "+(i+1)+" from the left. Couldn't find the key "+keys[i]+".");
						throw new IOException();
					}
				}
			}
		}
		return out;
	}
	
	private static List<Boolean> getKeyboardStates(String[] sections, int linecounter, int sectionnumber) throws IOException {
		List<Boolean> out = new ArrayList<Boolean>();
		if(!sections[sectionnumber].isEmpty()) {
			if(sections[sectionnumber].startsWith("KeyboardState:")) {
				sections[sectionnumber]=sections[sectionnumber].replace("KeyboardState:", "");
			}
			String[] keys= sections[sectionnumber].split(",");
			for (int i = 0; i < keys.length; i++) {
				if (keys[i].contentEquals(" ")) {
					keys[i] = "false";
				}
				//System.out.println(linecounter+": "+keys[i]); //TODO
				if (keys[i].contentEquals("true")||keys[i].contentEquals("false")) {
					out.add(Boolean.parseBoolean(keys[i]));
				} else {
					logger.error("Error while reading 'KeyboardState' in " + Filename + " in line " + linecounter
							+ " and the key in position " + (i + 1) + " from the left. Input is not true or false: '"
							+ keys[i] + "'");
					throw new IOException();
				}
			}
		}
		return out;
	}
	
	private static List<Character> getCharTyped(String[] sections, int linecounter, int sectionnumber) throws IOException {
		List<Character> out = new ArrayList<Character>();
		if (!sections[sectionnumber].isEmpty()) {
			if (sections[sectionnumber].startsWith("CharacterTyped:")) {
				sections[sectionnumber] = sections[sectionnumber].replace("CharacterTyped:", "");
			}
				sections[sectionnumber]= sections[sectionnumber].replace("\\n","\n"); //TODO Seperate CR and LF for linux users
				char[] chars = sections[sectionnumber].toCharArray();
				for (int j = 0; j < chars.length; j++) {
					//System.out.println(linecounter + ": " + chars[j]); // TODO
					out.add(chars[j]);
				}

			
		}
		return out;
	}
	
	private static List<Integer> getMouseButtonPresses(String[] sections, int linecounter, int sectionnumber) throws IOException {
		List<Integer> out = new ArrayList<Integer>();
		if(!sections[sectionnumber].isEmpty()) {
			if(sections[sectionnumber].startsWith("Mouse:")) {
				sections[sectionnumber]=sections[sectionnumber].replace("Mouse:", "");
			}
			String[] keys= sections[sectionnumber].split(",");
			for (int i = 0; i < keys.length; i++) {
				if(isNumber(keys[i])) {
					out.add(Integer.parseInt(keys[i]));
				}else {
					if (keys[i].contentEquals(" ")) {
						keys[i] = "MOUSEMOVED";
					}
					//System.out.println(linecounter+": "+keys[i]); //TODO
					if (VirtualMouseAndKeyboard.getKeyCodeFromKeyName(keys[i]) != -1) {
							out.add(VirtualMouseAndKeyboard.getKeyCodeFromKeyName(keys[i]));
					} else {
						logger.error("Error while reading 'Mouse' in " + Filename + " in line " + linecounter
								+ " and the key in position " + (i + 1) + " from the left. Couldn't find the key "
								+ keys[i]);
						throw new IOException();
					}
				}
			}
		}
		return out;
	}
	
	private static List<Boolean> getMouseStates(String[] sections, int linecounter, int sectionnumber) throws IOException {
		List<Boolean> out = new ArrayList<Boolean>();
		if(!sections[sectionnumber].isEmpty()) {
			if(sections[sectionnumber].startsWith("MouseState:")) {
				sections[sectionnumber]=sections[sectionnumber].replace("MouseState:", "");
			}
			String[] keys= sections[sectionnumber].split(",");
			for (int i = 0; i < keys.length; i++) {
				if (keys[i].contentEquals(" ")) {
					keys[i] = "false";
				}
				//System.out.println(linecounter+": "+keys[i]); //TODO
				if (keys[i].contentEquals("true")||keys[i].contentEquals("false")) {
					out.add(Boolean.parseBoolean(keys[i]));
				} else {
					logger.error("Error while reading 'MouseState' in " + Filename + " in line " + linecounter
							+ " and the key in position " + (i + 1) + " from the left. Input is not true or false: '"
							+ keys[i] + "'");
					throw new IOException();
				}
			}
		}
		return out;
	}
	
	private static List<Integer> getScrollWheelDeltas(String[] sections, int linecounter, int sectionnumber) throws IOException {
		List<Integer> out = new ArrayList<Integer>();
		if (!sections[sectionnumber].isEmpty()) {
			if (sections[sectionnumber].startsWith("ScrollWheel:")) {
				sections[sectionnumber] = sections[7].replace("ScrollWheel:", "");
			}

			String[] keys = sections[sectionnumber].split(",");
			for (int i = 0; i < keys.length; i++) {
				if (keys[i].contentEquals(" ")) {
					keys[i] = "0";
				}
				//System.out.println(linecounter+": "+keys[i]); //TODO
				out.add(getNumber("ScrollWheel", keys[i], linecounter, i));
			}
		}
		return out;
	}
	
//	private static List<Float> getDeltaX(String[] sections, int linecounter) throws IOException {
//		List<Float> out = new ArrayList<Float>();
//		String[] xpart= sections[8].split(";");
//		if (xpart.length!=0) {
//			if (xpart[0].startsWith("DeltaX/Y:")) {
//				xpart[0] = xpart[0].replace("DeltaX/Y:", "");
//			}
//			String[] keys = xpart[0].split(",");
//			for (int i = 0; i < keys.length; i++) {
//				//System.out.println(linecounter+" "+keys[i]); //TODO
//				out.add(Float.parseFloat(keys[i]));
//			}
//		}
//		return out;
//	}
//	
//	private static List<Float> getDeltaY(String[] sections, int linecounter) throws IOException {
//		List<Float> out = new ArrayList<Float>();
//		String[] ypart= sections[8].split(";");
//		if (ypart.length!=0) {
//			if (ypart[1].startsWith("DeltaX/Y:")) {
//				ypart[1] = ypart[1].replace("DeltaX/Y:", "");
//			}
//			String[] keys = ypart[1].split(",");
//			for (int i = 0; i < keys.length; i++) {
//				//System.out.println(linecounter+" "+keys[i]); //TODO
//				out.add(Float.parseFloat(keys[i]));
//			}
//		}
//		return out;
//	}
	
	private static List<Integer> getMouseX(String[] sections, int linecounter, int sectionnumber) throws IOException {
		List<Integer> out = new ArrayList<Integer>();
		String[] xpart= sections[sectionnumber].split(";");
		if (xpart.length!=0) {
			if (xpart[0].startsWith("MouseX/Y:")) {
				xpart[0] = xpart[0].replace("MouseX/Y:", "");
			}
			String[] keys = xpart[0].split(",");
			for (int i = 0; i < keys.length; i++) {
				//System.out.println(linecounter+" "+keys[i]); //TODO
				out.add(getNumber("MouseX", keys[i], linecounter, i));
			}
		}
		return out;
	}
	
	private static List<Integer> getMouseY(String[] sections, int linecounter, int sectionnumber) throws IOException {
		List<Integer> out = new ArrayList<Integer>();
		String[] ypart= sections[sectionnumber].split(";");
		if (ypart.length!=0) {
			if (ypart[1].startsWith("MouseX/Y:")) {
				ypart[1] = ypart[1].replace("MouseX/Y:", "");
			}
			String[] keys = ypart[1].split(",");
			for (int i = 0; i < keys.length; i++) {
				//System.out.println(linecounter+" "+keys[i]); //TODO
				out.add(getNumber("MouseY", keys[i], linecounter, i));
			}
		}
		return out;
	}
	
	private static List<Integer> getSlotID(String[] sections, int linecounter, int sectionnumber) throws IOException {
		List<Integer> out = new ArrayList<Integer>();
		if (sections.length>=sectionnumber+1) {
			if (sections[sectionnumber].startsWith("SlotID:")) {
				sections[sectionnumber] = sections[sectionnumber].replace("SlotID:", "");
			}
			String[] keys = sections[sectionnumber].split(",");
			for (int i = 0; i < keys.length; i++) {
				if (keys[i].contentEquals(" ")) {
					keys[i] = "-1";
				}
				//System.out.println(linecounter+": "+keys[i]); //TODO
				out.add(getNumber("SlotID", keys[i], linecounter, i));
			}
		}
		return out;
	}


	private static int getNumber(String name, String number, int linecounter, int position) throws IOException {
		int counter=-1;
		try {
			counter=Integer.parseInt(number);
		} catch (NumberFormatException e) {
			logger.error("Error while reading "+name+" in "+Filename+ " in line "+linecounter+" and the input in position "+(position+1)+" from the left. The input doesn't contain a number ("+number+")");
			throw new IOException();
		}
		return counter;
	}
	private static boolean isNumber(String string) {
		int counter=-1;
		try {
			counter=Integer.parseInt(string);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	private static float getFloat(String name, String floatnumber, int linecounter, int position) throws IOException {
		float counter=-1F;
		try {
			counter=Float.parseFloat(floatnumber);
		}catch (NumberFormatException e) {
			logger.error("Error while reading "+name+" in "+Filename+ " in line "+linecounter+" and the input in position "+(position+1)+" from the left. The input doesn't contain a number ("+floatnumber+")");
			throw new IOException();
		}
		return counter;
	}
	public static boolean isPlayingback() {
		return playingback;
	}
	
	private static float getSubtickPitch(String[] sections, int linecounter) throws IOException {
		float x=0;
		if(sections[1].isEmpty()) {
			logger.error("Error while reading tickcounter in "+Filename+ " in line "+linecounter+". The input is empty!");
		}else {
			x=getFloat("subtickPitch", sections[1], linecounter, 1);
		}
		return x;
	}
	private static float getSubtickYaw(String[] sections, int linecounter) throws IOException {
		float y=0;
		if(sections.length<3) {
			logger.error("Error while reading tickcounter in "+Filename+ " in line "+linecounter+". The input is empty!");
		}else {
			y=getFloat("subtickYaw", sections[2], linecounter, 1);
		}
		return y;
	}
	public static List<VirtualSubticks> getSubtickList(){
		return subtickList;
	}
	public static VirtualSubticks getCurrentSubtick() {
		return subtickList.get(subtickPlaybackindex);
	}
	
}
