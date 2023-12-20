package com.cloudfuze.mail.model.request;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import javax.swing.JOptionPane;

public class VirtualKeyboardOpener {
	public static void main(String[] args) {
		String keyboardPath = System.getenv("windir") + "\\System32\\osk.exe";

		try {
			int i =0;
			for (;;) {
				i=i+1;
				openVirtualKeyboard(keyboardPath);
				System.out.println("Started.");
				
				Thread.sleep(5000); // Wait for 5 seconds
				typeHey(i);
				//getOpenPrograms();
				// closeVirtualKeyboard();
				closeVirtualKeyboard_1("osk.exe");
				moveCursor();
				// closeVirtualKeyboard_2();
				System.out.println("Ended...");

				Thread.sleep(5000); // Wait for 5 seconds

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void openVirtualKeyboard(String keyboardPath) throws IOException {
		File keyboardFile = new File(keyboardPath);
		Desktop desktop = Desktop.getDesktop();

		if (desktop.isSupported(Desktop.Action.OPEN)) {
			desktop.open(keyboardFile);
			// desktop.
		} else {
			System.out.println("Opening virtual keyboard is not supported on this platform.");
		}
	}

	private static void closeVirtualKeyboard() throws IOException {
		Robot robot;
		try {
			robot = new Robot();
			robot.keyPress(27); // Press the Escape key to close the virtual keyboard
			robot.keyRelease(27);
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void typeHey( int i) {
		try {
			Robot robot = new Robot();
			robot.delay(500); // Delay before typing (500 milliseconds = 0.5 seconds)

 
			robot.keyPress(KeyEvent.VK_H);
			robot.keyRelease(KeyEvent.VK_H);
			robot.delay(500); 
			robot.keyPress(KeyEvent.VK_W);
			robot.keyRelease(KeyEvent.VK_W);
//			robot.keyPress(KeyEvent.VK_TAB);
//			robot.keyRelease(KeyEvent.VK_TAB);
//			robot.delay(500); 
			robot.keyPress(KeyEvent.VK_Y);
			robot.keyRelease(KeyEvent.VK_Y);
			robot.keyPress(KeyEvent.VK_SPACE);
			robot.keyRelease(KeyEvent.VK_SPACE);
			robot.delay(500); 
			robot.keyPress(KeyEvent.VK_CAPS_LOCK);
			robot.keyRelease(KeyEvent.VK_CAPS_LOCK);
			robot.delay(500); 
			robot.keyPress(KeyEvent.VK_Y);
			robot.keyRelease(KeyEvent.VK_Y);
			robot.keyPress(KeyEvent.VK_H);
			robot.keyRelease(KeyEvent.VK_H);
			robot.delay(500); 
			robot.keyPress(KeyEvent.VK_B);
			robot.keyRelease(KeyEvent.VK_B);
			robot.delay(500); 
			robot.keyPress(KeyEvent.VK_8);
			robot.keyRelease(KeyEvent.VK_8);
			robot.delay(500); 
			robot.keyPress(KeyEvent.VK_4);
			robot.keyRelease(KeyEvent.VK_4);
			robot.keyPress(KeyEvent.VK_COMMA);
			robot.keyRelease(KeyEvent.VK_COMMA);
			robot.delay(500); 
			robot.keyPress(KeyEvent.VK_BACK_SPACE);
			robot.keyRelease(KeyEvent.VK_BACK_SPACE);
			robot.delay(500); 
			robot.keyPress(KeyEvent.VK_CAPS_LOCK);
			robot.keyRelease(KeyEvent.VK_CAPS_LOCK);
			robot.keyPress(KeyEvent.VK_SCROLL_LOCK);
			robot.keyRelease(KeyEvent.VK_SCROLL_LOCK);
			robot.delay(50); 
			if(i%2==0) {
				robot.keyPress(KeyEvent.VK_ENTER);
				robot.keyRelease(KeyEvent.VK_ENTER);
				robot.delay(500); 
				robot.keyPress(KeyEvent.VK_ENTER);
				robot.keyRelease(KeyEvent.VK_ENTER);
				robot.delay(500); 
				robot.keyPress(KeyEvent.VK_2);
				robot.keyRelease(KeyEvent.VK_2);
				robot.delay(500); 
				robot.keyPress(KeyEvent.VK_DELETE);
				robot.keyRelease(KeyEvent.VK_DELETE);
				robot.delay(500); 
//				robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//				robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
//				robot.delay(500); 
//				robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
//				robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);	
//				robot.delay(500); 
				robot.mouseWheel(i*2);
				robot.keyPress(KeyEvent.VK_BACK_SPACE);
				robot.keyRelease(KeyEvent.VK_BACK_SPACE);
			}else {
				robot.keyPress(KeyEvent.VK_BACK_SPACE);
				robot.keyRelease(KeyEvent.VK_BACK_SPACE);
			}
			robot.delay(500); 
	//		robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
//			robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);	
//		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);	
			robot.mouseWheel(i);
			
			robot.delay(500);
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	private static void closeVirtualKeyboard_1(String exeFileName) {
		try {
			// Execute the taskkill command
			Process process = Runtime.getRuntime().exec("taskkill /F /IM " + exeFileName);

			// Wait for the process to complete
			int exitCode = process.waitFor();
			Process pr = process.destroyForcibly();
			reader(pr.getErrorStream());
			System.out.println(pr.exitValue());
			// Print the exit code
			System.out.println("Exit Code: " + exitCode);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void closeVirtualKeyboard_2() {
		try {
			Robot robot = new Robot();
			robot.keyPress(KeyEvent.VK_ALT);
			robot.keyPress(KeyEvent.VK_F4);
			robot.keyRelease(KeyEvent.VK_ALT);
			robot.keyRelease(KeyEvent.VK_F4);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Set<String> getOpenPrograms() throws IOException {
		try {
			Process process = runCommand("tasklist /fo csv /nh");
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				// Each line contains process information in CSV format
				String[] processInfo = line.split(",");
				String processName = processInfo[0].replace("\"", "");
				//popUp(processName);
				System.out.println("Process: " + processName);
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Process runCommand(String command) throws IOException {
		return new ProcessBuilder("cmd", "/c", command).start();
	}

	public static void program(String[] args) {
		try {
			Set<String> openPrograms = getOpenPrograms();
			System.out.println("Opened programs:");
			for (String program : openPrograms) {
				System.out.println(program);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static String convertToString(InputStream inputStream) {
		Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
		return scanner.hasNext() ? scanner.next() : "";
	}

	public static void reader(InputStream inputStream) {

		String result = convertToString(inputStream);
		System.out.println(result);
	}

	private static void moveCursor() {
		try {
			Robot robot = new Robot();
			Random random = new Random();
			System.out.println("cursor moving started");
			// Get the screen size
			GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
			int screenWidth = gd.getDisplayMode().getWidth();
			int screenHeight = gd.getDisplayMode().getHeight();
			// Move the mouse cursor randomly
			//  while (true) {
			int x = random.nextInt(screenWidth);
			int y = random.nextInt(screenHeight);
			robot.mouseMove(x, y);
			Thread.sleep(1000); // Wait for 1 second before moving again
			System.out.println("cursor moving ended");
			//}
		} catch (AWTException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	
	 public static void popUp(String text) {
		  JOptionPane optionPane = new JOptionPane(text, JOptionPane.INFORMATION_MESSAGE);
		  final javax.swing.JDialog dialog = optionPane.createDialog("Popup Message");
		 /// optionPane.createImage(100, 100);

	        // Display the dialog
	        dialog.setVisible(true);

	        int delayInMillis = 100; // 1 seconds
	        javax.swing.Timer timer = new javax.swing.Timer(delayInMillis, e -> {
	            dialog.dispose();
	            optionPane.setValue(JOptionPane.OK_OPTION);
	        });
	        timer.setRepeats(false); // Only fire the timer event once
	        timer.start();
	    }

}

