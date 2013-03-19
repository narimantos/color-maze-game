/* MazeGame Class
 * By Tyler Compton for Team Tyro
 * 
 * This is a very simple and minimal map game. It's official name is
 * "Color Maze Game."
 */

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import sql.InfoPackage;
import threads.SendData;
import etc.Constants;
import etc.ErrorReport;
import etc.MazeMap;

public class MazeGame extends Applet {
	private static final long serialVersionUID = 1L;
	private static Random generator = new Random();
	private static int[][] map;			// Universal map array 
	
	private static int [] recActions; 	// Stores all the keys pressed. [DIR_RIGHT,UP,DOWN,LEFT]
	private static int currentAction; 	// Keeps track of which part of recActions your using. Basically just a counter for recActions
	private static int rCurrentAction;	// Replay current action, just for replaying
	private static int operation;		// The phase of the test. 0= moving around, playing game. 1= Replaying the game 2= Finished with testing, sending data.
	private static java.util.Date startDate, endDate; // Actual day, time, milliseconds that you played the game.
		
	private static boolean [] keyRefresh;	//Makes sure that holding a button won't machine-gun it. [true=its up, and can be pressed. False=it's being pressed]
	
	private static int pX, pY;			// Player x and y (within the map array)
	
	Canvas display_parent;
	boolean running;
	Thread gameThread;
	
	boolean showDiagonal = false;
	
	/** Function startLWJGL()
	 * Executes LWJGL's startup methods.
	 */
	public void startLWJGL() {
		gameThread = new Thread() {
			public void run() {
				running = true;
				try {
					Display.setParent(display_parent);
					Display.create();
					initGL();
				} catch(LWJGLException ex) {
					ErrorReport e = new ErrorReport(ex.getMessage());
					e.makeFile();
					return;
				}
				mainLoop();
			}
		};
		gameThread.start();
	}
	
	/** Function stopLWJGL
	 * Stops the game thread.
	 */
	public void stopLWJGL() {
		running = false;
		try {
			gameThread.join();
		} catch(InterruptedException ex) {
			ErrorReport e = new ErrorReport(ex.getMessage());
			e.makeFile();
		}
	}
	
	/** Function start()
	 * Placeholder for the expected start() method in applets.
	 */
	public void start() {
		
	}
	
	/** Function stop()
	 * Placeholder for the expected stop() method in applets.
	 */
	public void stop() {
		
	}
	
	/** Function destroy()
	 * Destroys the canvas.
	 */
	public void destroy() {
		remove(display_parent);
		super.destroy();
	}
	
	/** Function init()
	 * Initializes the canvas and global variables
	 */
	public void init() {
		setLayout(new BorderLayout());
		try {
			display_parent = new Canvas() {
				private static final long serialVersionUID = 1L;
				public final void addNotify() {
					super.addNotify();
					startLWJGL();
				}
				public final void removeNotify() {
					stopLWJGL();
					super.removeNotify();
				}
			};
			display_parent.setSize(600,600);
			add(display_parent);
			display_parent.setFocusable(true);
			display_parent.requestFocus();
			display_parent.setIgnoreRepaint(true);
			setVisible(true);
		} catch(Exception ex) {
			ErrorReport e = new ErrorReport(ex.getMessage());
			e.makeFile();
			throw new RuntimeException("Unable to create display!");
		}
		
		map = makeMaze();
		
		pX = Constants.MAP_WIDTH/2;
		pY = 0;
		keyRefresh = new boolean [6];
		
		recActions = new int [500];
		operation = 0;
		
		currentAction = 0;
		
		startDate = new java.util.Date();
		
		// This section temporarily removed
		MazeMap maze = new MazeMap();
		URL mapURL;
		try {
			mapURL = new URL("http://jackketcham.com/teamtyro/game/map1.txt");
			maze.loadMap(mapURL);
		} catch(MalformedURLException ex) {
			ErrorReport e = new ErrorReport(ex.getMessage());
			e.makeFile();
		}
		
		for(int x=0; x<Constants.MAP_WIDTH; x++) {
			for(int y=0; y<Constants.MAP_HEIGHT; y++) {
				map[x][y] = maze.getSpace(x,y);
				if(map[x][y] == Constants.MAP_START) {
					pX = x;
					pY = y;
				}
			}
		}
		
		printMaze(map);
	}
	
	/** Function initGL()
	 * Calls OpenGL initialization functions.
	 */
	protected void initGL() {
		// Init OpenGL
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(-300, 300, -300, 300, 1, -1);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
	}
	
	/** Function begin()
	 * Sets up OpenGL and lwjgl and contains the main loop.
	 */
	private void mainLoop() {
		// Start main loop
		while(running) {
			// Clears screen and depth buffer
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
			
			// Rendering
			render();
			
			if(operation == 0) {
				// Testing in progress
				checkKeys();
				if(map[pX][pY] == Constants.MAP_WIN) {
					endDate = new java.util.Date();
					SendData sender = new SendData(packUp(startDate, endDate, recActions));
					(new Thread(sender)).start();
					
					operation = 2;
				}
			} else if(operation == 1) {
				// Replay debug feature
				if(rCurrentAction < currentAction) {
					replayGame(recActions, rCurrentAction);
					rCurrentAction++;
				}
			} else if(operation == 2) {
				// Test is over
			}

			Display.update();
		}
		
		Display.destroy();
	}
	
	/** Function replayGame(int [] s_recActions, int s_length)
	 * Replays the set of actions from the array s_recActions to the point
	 * specified by int s_length.
	 */
	private void replayGame(int [] s_recActions, int s_length) {
		switch(s_recActions[s_length]) {
		case Constants.DIR_DOWN:
			pY++;
			break;
		case Constants.DIR_UP:
			pY--;
			break;
		case Constants.DIR_RIGHT:
			pX++;
			break;
		case Constants.DIR_LEFT:
			pX--;
			break;
		}
		try {
		    Thread.sleep(100);
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
	}
	
	/** Function render()
	 * Draws all visible objects.
	 */
	private void render() {
		int x, y;	// Bottom left corner coordinates (for readability)
		
		// Left box
		x = -300;
		y = -100;
		setColor(pX-1, pY, map);
		GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2f(x    ,y    );
			GL11.glVertex2f(x+200,y  +0);
			GL11.glVertex2f(x+200,y+200);
			GL11.glVertex2f(x  +0,y+200);
		GL11.glEnd();
		
		// Right box
		x = 100;
		y = -100;
		setColor(pX+1, pY, map);
		GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2f(x    ,y    );
			GL11.glVertex2f(x+200,y  +0);
			GL11.glVertex2f(x+200,y+200);
			GL11.glVertex2f(x  +0,y+200);
		GL11.glEnd();
		
		
		// Up box
		x = -100;
		y = 100;
		setColor(pX, pY-1, map);
		GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2f(x    ,y    );
			GL11.glVertex2f(x+200,y  +0);
			GL11.glVertex2f(x+200,y+200);
			GL11.glVertex2f(x  +0,y+200);
		GL11.glEnd();
		
		// Down box
		x = -100;
		y = -300;
		setColor(pX, pY+1, map);
		GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2f(x    ,y    );
			GL11.glVertex2f(x+200,y  +0);
			GL11.glVertex2f(x+200,y+200);
			GL11.glVertex2f(x  +0,y+200);
		GL11.glEnd();
		
		if(showDiagonal) {
			// Top-Left box
			x = -300;
			y = 100;
			setColor(pX-1, pY-1, map);
			GL11.glBegin(GL11.GL_QUADS);
				GL11.glVertex2f(x    ,y    );
				GL11.glVertex2f(x+200,y  +0);
				GL11.glVertex2f(x+200,y+200);
				GL11.glVertex2f(x  +0,y+200);
			GL11.glEnd();
			
			// Top-Right box
			x = 100;
			y = 100;
			setColor(pX+1, pY-1, map);
			GL11.glBegin(GL11.GL_QUADS);
				GL11.glVertex2f(x    ,y    );
				GL11.glVertex2f(x+200,y  +0);
				GL11.glVertex2f(x+200,y+200);
				GL11.glVertex2f(x  +0,y+200);
			GL11.glEnd();
			
			// Bottom-Left box
			x = -300;
			y = -300;
			setColor(pX-1, pY+1, map);
			GL11.glBegin(GL11.GL_QUADS);
				GL11.glVertex2f(x    ,y    );
				GL11.glVertex2f(x+200,y  +0);
				GL11.glVertex2f(x+200,y+200);
				GL11.glVertex2f(x  +0,y+200);
			GL11.glEnd();
			
			// Bottom-Right box
			x = 100;
			y = -300;
			setColor(pX+1, pY+1, map);
			GL11.glBegin(GL11.GL_QUADS);
				GL11.glVertex2f(x    ,y    );
				GL11.glVertex2f(x+200,y  +0);
				GL11.glVertex2f(x+200,y+200);
				GL11.glVertex2f(x  +0,y+200);
			GL11.glEnd();
		}
		
		// Center box
		x = -100;
		y = -100;
		setColor(pX, pY, map);
		GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2f(x    ,y    );
			GL11.glVertex2f(x+200,y  +0);
			GL11.glVertex2f(x+200,y+200);
			GL11.glVertex2f(x  +0,y+200);
		GL11.glEnd();
		
		// Player
		x = -50;
		y = -50;
		GL11.glColor3f(1,1,1);
		GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2f(x    ,y    );
			GL11.glVertex2f(x+100,y  +0);
			GL11.glVertex2f(x+100,y+100);
			GL11.glVertex2f(x  +0,y+100);
		GL11.glEnd();
	}
	
	/** Function setColor(int x, int y, int [][] tmap)
	 * Returns a fitting color based on what is on the given
	 * coordinates on the given map.
	 */
	private void setColor(int x, int y, int [][] tmap) {
		if(x<0 || y<0 || x>Constants.MAP_WIDTH-1 || y>Constants.MAP_HEIGHT-1) {
			GL11.glColor3f(1,0,0);
			return;
		}
		
		switch(tmap[x][y]) {
		case Constants.MAP_BLOCK:
			GL11.glColor3f(1,0,0);
			break;
		case Constants.MAP_SPACE:
			GL11.glColor3f(0,0,1);
			break;
		case Constants.MAP_WIN:
			GL11.glColor3f(0,1,0);
			break;
		case Constants.MAP_START:
			GL11.glColor3f(0,0,1);
			break;
		}
	}
	
	/** Function checkKeys()
	 * Reads for key input and acts accordingly. More specifically,
	 * the player is moved from arrow key presses.
	 */
	private void checkKeys() {
		// Check for "Up" key
		if(Keyboard.isKeyDown(Keyboard.KEY_UP) && keyRefresh[Constants.DIR_UP]) {
			if(movePlayer(Constants.DIR_UP, pX, pY, map)) {
				pY--;
				recActions[currentAction] = Constants.DIR_UP;
				currentAction++;
			}
			keyRefresh[Constants.DIR_UP] = false;
		} else if(!Keyboard.isKeyDown(Keyboard.KEY_UP)) {
			keyRefresh[Constants.DIR_UP] = true;
		}
		// Check for "Down" key
		if(Keyboard.isKeyDown(Keyboard.KEY_DOWN) && keyRefresh[Constants.DIR_DOWN]) {
			if(movePlayer(Constants.DIR_DOWN, pX, pY, map)) {
				pY++;
				recActions[currentAction] = Constants.DIR_DOWN;
				currentAction++;
			}
			keyRefresh[Constants.DIR_DOWN] = false;
		} else if(!Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
			keyRefresh[Constants.DIR_DOWN] = true;
		}
		// Check for "Left" key
		if(Keyboard.isKeyDown(Keyboard.KEY_LEFT) && keyRefresh[Constants.DIR_LEFT]) {
			if(movePlayer(Constants.DIR_LEFT, pX, pY, map)) {
				pX--;
				recActions[currentAction] = Constants.DIR_LEFT;
				currentAction++;
			}
			keyRefresh[Constants.DIR_LEFT] = false;
		} else if(!Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			keyRefresh[Constants.DIR_LEFT] = true;
		}
		// Check for "Right" key
		if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT) && keyRefresh[Constants.DIR_RIGHT]) {
			if(movePlayer(Constants.DIR_RIGHT, pX, pY, map)) {
				pX++;
				recActions[currentAction] = Constants.DIR_RIGHT;
				currentAction++;
			}
			keyRefresh[Constants.DIR_RIGHT] = false;
		} else if(!Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			keyRefresh[Constants.DIR_RIGHT] = true;
		}
	}
	
	/** Function movePlayer(int dir, int x, int y, int [][] tmap)
	 * Checks move requests for validity. Returns true if no
	 * obstructions would keep the player from moving in that direction.
	 */
	private boolean movePlayer(int dir, int x, int y, int [][] tmap) {
		switch(dir) {
		case Constants.DIR_UP:
			if(y>0) {
				if(tmap[x][y-1] != Constants.MAP_BLOCK) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
			// break;
		case Constants.DIR_DOWN:
			if(y<Constants.MAP_HEIGHT-1) {
				if(tmap[x][y+1] != Constants.MAP_BLOCK) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
			// break;
		case Constants.DIR_LEFT:
			if(x>0) {
				if(tmap[x-1][y] != Constants.MAP_BLOCK) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		case Constants.DIR_RIGHT:
			if(x<Constants.MAP_HEIGHT-1) {
				if(tmap[x+1][y] != Constants.MAP_BLOCK) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		default:
			System.out.printf("Error: Unexpected direction in movePlayer.\n");
		}
		
		return false;
	}
	
	/** Method InfoPackage packUp(java.util.Date sD, java.util.Date eD, int[] a)
	 * sD startDate
	 * eD endDate
	 * a=recActions*
	 */
	private InfoPackage packUp(java.util.Date sD, java.util.Date eD, int[] a) {
		InfoPackage out = new InfoPackage();
		
		out.setDates(sD, eD);
		out.setActions(a);
		out.setSurvey(	getParameter("AgeRange"),
						getParameter("Ethnicity"),
						getParameter("Profession"),
						getParameter("Email") );
		
		return out;
	}
	
	/** Function makeMaze()
	 * Randomly creates a maze by drawing lines of a random
	 * direction and size and returns a two dimensional
	 * array with the map information.
	 */
	private int[][] makeMaze() {
		int [][] out = new int [Constants.MAP_WIDTH][Constants.MAP_HEIGHT];
		for(int x=0; x<Constants.MAP_WIDTH; x++) {
			for(int y=0; y<Constants.MAP_HEIGHT; y++) {
				out[x][y] = Constants.MAP_BLOCK;
			}
		}
		
		int x = Constants.MAP_WIDTH/2;
		int y = 0;
		out[x][y] = Constants.MAP_SPACE;
		int lastDir = -1;
		for(int i=0; i<20; i++) {
			int dir = generator.nextInt(4);
			int len = generator.nextInt(4);
			while( (dir==0 && lastDir==3) || (dir==1 && lastDir == 2) || 
					(dir==2 && lastDir==1) || (dir==3 && lastDir==0) ) {
				dir = generator.nextInt(4);
			}
			switch (dir) {
			case 0:		//Go down
				for(int j=0; j<len; j++) {
					if(y < Constants.MAP_WIDTH-1) {
						y+=1;
						out[x][y] = Constants.MAP_SPACE;
					}
				}
				break;
			case 1:		//Go right
				for(int j=0; j<len; j++) {
					if(x < Constants.MAP_HEIGHT-1) {
						x+=1;
						out[x][y] = Constants.MAP_SPACE;
					}
				}
				break;
			case 2:		//Go left
				for(int j=0; j<len; j++) {
					if(x > 0) {
						x-=1;
						out[x][y] = Constants.MAP_SPACE;
					}
				}
				break;
			case 3:		//Go up
				for(int j=0; j<len; j++) {
					if(y>0) {
						y-=1;
						out[x][y] = Constants.MAP_SPACE;
					}
				}
				break;
			default:
				System.out.printf("Error: Unexpected random value in map gen. %d\n", dir);
			}
			lastDir = dir;
		}
		
		out[x][y] = Constants.MAP_WIN;
		
		return out;
	}
	
	/** Function printMaze(int[][] tmap)
	 * Prints the given map as text.
	 */
	private void printMaze(int[][] tmap) {
		for(int x=0; x<Constants.MAP_WIDTH+2; x++) {
			System.out.printf("[-]");
		}
		System.out.println("");
		for (int y = 0; y < Constants.MAP_WIDTH; y++) {
			System.out.printf("[|]");
			for (int x = 0; x < Constants.MAP_HEIGHT; x++) {
				switch (tmap[x][y]) {
				case Constants.MAP_START:
					System.out.printf(" s ");
				case Constants.MAP_BLOCK:
					System.out.printf("[ ]");
					break;
				case Constants.MAP_SPACE:
					System.out.printf("   ");
					break;
				case Constants.MAP_WIN:
					System.out.printf(" w ");
				}
			}
			System.out.printf("[|]");
			System.out.println("");
		}
		for(int x=0; x<Constants.MAP_WIDTH+2; x++) {
			System.out.printf("[-]");
		}
		
		System.out.printf("\n");
	}
}
