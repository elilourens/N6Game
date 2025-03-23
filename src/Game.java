
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import java.awt.*;


import game2D.*;

// Game demonstrates how we can override the GameCore class
// to create our own 'game'. We usually need to implement at
// least 'draw' and 'update' (not including any local event handling)
// to begin the process. You should also add code to the 'init'
// method that will initialise event handlers etc. 

// Student ID: 2926685


@SuppressWarnings("serial")


public class Game extends GameCore 
{
	// Useful game constants
	static int screenWidth = 512;
	static int screenHeight = 384;

	// Game constants

    float	gravity = 0.0002f;

    float	moveSpeed = 0.05f;
    
    // Game state flags
    boolean jump = false;
    boolean canJump = false;
    boolean moveRight = false;
    boolean moveLeft = false;
    boolean debug = true;		

    // Game resources
    Animation landing;
    Animation idle;
    Animation run;
    
    Sprite	player = null;
    Sprite npc1 = null;
    ArrayList<Sprite> 	clouds = new ArrayList<Sprite>();
    ArrayList<Tile>		collidedTiles = new ArrayList<Tile>();

    ArrayList<Sprite> npcs = new ArrayList<>();

    TileMap tmap = new TileMap();	// Our tile map, note that we load it in init()
    
    long total;         			// The score will be the total time elapsed since a crash


    /**
	 * The obligatory main method that creates
     * an instance of our class and starts it running
     * 
     * @param args	The list of parameters this program might use (ignored)
     */
    public static void main(String[] args) {

        Game gct = new Game();
        gct.init();
        // Start in windowed mode with the given screen height and width
        gct.run(false,screenWidth,screenHeight);
    }

    /**
     * Initialise the class, e.g. set up variables, load images,
     * create animations, register event handlers.
     * 
     * This shows you the general principles but you should create specific
     * methods for setting up your game that can be called again when you wish to 
     * restart the game (for example you may only want to load animations once
     * but you could reset the positions of sprites each time you restart the game).
     */
    public void init()
    {         
        Sprite s;	// Temporary reference to a sprite

        // Load the tile map and print it out so we can check it is valid
        tmap.loadMap("maps", "map.txt");
        
        setSize(tmap.getPixelWidth()/2, tmap.getPixelHeight());
        setVisible(true);

        // Create a set of background sprites that we can 
        // rearrange to give the illusion of motion
        
        landing = new Animation();
        landing.loadAnimationFromSheet("images/landbird.png", 4, 1, 60);

        run = new Animation();
        run.loadAnimationFromSheet("images/run.png",6,1,85);

        idle = new Animation();
        idle.loadAnimationFromSheet("images/idle.png",9,1,150);

        idle.play();
        
        // Initialise the player with an animation
        player = new Sprite(idle);
        npc1 = new Sprite(idle);
        
        // Load a single cloud animation
        Animation ca = new Animation();
        ca.addFrame(loadImage("images/cloud.png"), 1000);
        
        // Create 3 clouds at random positions off the screen
        // to the right
        for (int c=0; c<3; c++)
        {
        	s = new Sprite(ca);
        	s.setX(screenWidth + (int)(Math.random()*200.0f));
        	s.setY(30 + (int)(Math.random()*150.0f));
        	s.setVelocityX(-0.02f);
        	s.show();
        	clouds.add(s);
        }

        initialiseGame();
      		
        System.out.println(tmap);
    }

    /**
     * You will probably want to put code to restart a game in
     * a separate method so that you can call it when restarting
     * the game when the player loses.
     */
    public void initialiseGame()
    {
    	total = 0;

        player.setPosition(200,200);
        player.setVelocity(0,0);
        player.show();

        npc1.setPosition(280,280);
        npc1.setVelocity(0,0);
        npc1.show();
    }
    
    /**
     * Draw the current state of the game. Note the sample use of
     * debugging output that is drawn directly to the game screen.
     */
    public void draw(Graphics2D g)
    {    	
    	// Be careful about the order in which you draw objects - you
    	// should draw the background first, then work your way 'forward'

    	// First work out how much we need to shift the view in order to
    	// see where the player is. To do this, we adjust the offset so that
        // it is relative to the player's position along with a shift
        int xo = -(int)player.getX() + 200;
        int yo = -(int)player.getY() + 200;

        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // Apply offsets to sprites then draw them
        for (Sprite s: clouds)
        {
        	s.setOffsets(xo,yo);
        	s.draw(g);
        }

        // Apply offsets to tile map and draw  it
        tmap.draw(g,xo,yo); 

        // Apply offsets to player and draw 
        player.setOffsets(xo, yo);
        player.drawTransformed(g);

        npc1.setOffsets(xo,yo);
        npc1.draw(g);

        
        // Show score and status information
        String msg = String.format("Score: %d", total/100);
        g.setColor(Color.darkGray);
        g.drawString(msg, getWidth() - 100, 50);
        
        if (debug)
        {

        	// When in debug mode, you could draw borders around objects
            // and write messages to the screen with useful information.
            // Try to avoid printing to the console since it will produce 
            // a lot of output and slow down your game.
            tmap.drawBorder(g, xo, yo, Color.black);

            g.setColor(Color.red);
        	player.drawBoundingBox(g);
        
        	g.drawString(String.format("Player: %.0f,%.0f", player.getX(),player.getY()),
        								getWidth() - 100, 70);
        	
        	drawCollidedTiles(g, tmap, xo, yo);
        }

    }

    public void drawCollidedTiles(Graphics2D g, TileMap map, int xOffset, int yOffset)
    {
		if (!collidedTiles.isEmpty())
		{	
			int tileWidth = map.getTileWidth();
			int tileHeight = map.getTileHeight();
			
			g.setColor(Color.blue);
			for (Tile t : collidedTiles)
			{
				g.drawRect(t.getXC()+xOffset, t.getYC()+yOffset, tileWidth, tileHeight);
			}
		}
    }
	
    /**
     * Update any sprites and check for collisions
     * 
     * @param elapsed The elapsed time between this call and the previous call of elapsed
     */    
    public void update(long elapsed)
    {
    	
        // Make adjustments to the speed of the sprite due to gravity

            player.setVelocityY(player.getVelocityY()+(gravity*elapsed));

        //player.setVelocityY(player.getVelocityY()+(gravity*elapsed));
    	    	
       	player.setAnimationSpeed(1.0f);
        npc1.setAnimationSpeed(1.0f);
       	
       	if (jump && this.canJump)
       	{
       		//player.setAnimationSpeed(1.8f);
       		player.setVelocityY(-0.2f);

            //Jump animation pause need to unpause when hitting the ground.
            player.pauseAnimation();
            this.canJump = false;
       	}
       	
       	if (moveRight)
       	{
            if(this.canJump){
                player.playAnimation();
            }
            player.setAnimation(run);
            player.flip(false);
       		player.setVelocityX(moveSpeed);
       	}
        else if (moveLeft)
        {
            if(this.canJump){
                player.playAnimation();
            }
            player.setAnimation(run);
            player.flip(true);
            player.setVelocityX(-moveSpeed);

        }
       	else
       	{
       		player.setVelocityX(0);
            player.setAnimation(idle);
       	}
       		
       	
                
       	for (Sprite s: clouds)
       		s.update(elapsed);
       	
        // Now update the sprites animation and position
        player.update(elapsed);
        npc1.update(elapsed);


        if(player.getVelocityX() != 0 || player.getVelocityY() != 0){
            // Then check for any collisions that may have occurred
            checkTileCollision(player, tmap);
            if(boundingBoxCollision(player,npc1)){
                npc1.hide();
            }
        }



        //For loop with all sprite npcs to check if they have velocity.
        //for()



        handleScreenEdge(player, tmap, elapsed);

    }
    
    
    /**
     * Checks and handles collisions with the edge of the screen. You should generally
     * use tile map collisions to prevent the player leaving the game area. This method
     * is only included as a temporary measure until you have properly developed your
     * tile maps.
     * 
     * @param s			The Sprite to check collisions for
     * @param tmap		The tile map to check 
     * @param elapsed	How much time has gone by since the last call
     */
    public void handleScreenEdge(Sprite s, TileMap tmap, long elapsed)
    {
    	// This method just checks if the sprite has gone off the bottom screen.
    	// Ideally you should use tile collision instead of this approach
    	
    	float difference = s.getY() + s.getHeight() - tmap.getPixelHeight();
        if (difference > 0)
        {
        	// Put the player back on the map according to how far over they were
        	s.setY(tmap.getPixelHeight() - s.getHeight() - (int)(difference)); 
        	
        	// and make them bounce
        	//s.setVelocityY(-s.getVelocityY()*0.75f);
        }
    }
    
    
     
    /**
     * Override of the keyPressed event defined in GameCore to catch our
     * own events
     * 
     *  @param e The event that has been generated
     */
    public void keyPressed(KeyEvent e) 
    { 
    	int key = e.getKeyCode();
    	
		switch (key)
		{
			case KeyEvent.VK_UP     : jump = true; break;
			case KeyEvent.VK_RIGHT  : moveRight = true; break;
            case KeyEvent.VK_LEFT   : moveLeft = true; break;
			case KeyEvent.VK_S 		: Sound s = new Sound("sounds/caw.wav"); 
									  s.start();
									  break;
			case KeyEvent.VK_ESCAPE : stop(); break;
			case KeyEvent.VK_B 		: debug = !debug; break; // Flip the debug state
			default :  break;
		}
    
    }

    /** Use the sample code in the lecture notes to properly detect
     * a bounding box collision between sprites s1 and s2.
     * 
     * @return	true if a collision may have occurred, false if it has not.
     */
    public boolean boundingBoxCollision(Sprite s1, Sprite s2)
    {
    	Rectangle s1Rectangle = new Rectangle((int) s1.getX(), (int) s1.getY(), s1.getWidth(),  s1.getHeight());
        Rectangle s2Rectangle = new Rectangle((int) s2.getX(), (int) s2.getY(), s2.getWidth(),  s2.getHeight());

        return s1Rectangle.intersects(s2Rectangle);

    }
    
    /**
     * Check and handles collisions with a tile map for the
     * given sprite 's'. Initial functionality is limited...
     * 
     * @param s			The Sprite to check collisions for
     * @param tmap		The tile map to check 
     */

    public void checkTileCollision(Sprite s, TileMap tmap)
    {
        collidedTiles.clear();

        Rectangle spriteRectangle = new Rectangle(
                (int) s.getX(), (int) s.getY(), s.getWidth(),  s.getHeight()
        );

        int tileWidth = tmap.getTileWidth();
        int tileHeight = tmap.getTileHeight();

        int startX = Math.max(0, (int) (s.getX() / tileWidth));
        int endX = Math.min(tmap.getMapWidth() - 1, (int) ((s.getX() + s.getWidth()) / tileWidth));
        int startY = Math.max(0, (int) (s.getY() / tileHeight));
        int endY = Math.min(tmap.getMapHeight() - 1, (int) ((s.getY() + s.getHeight()) / tileHeight));

        for(int y = startY; y <= endY; y++){
            for(int x = startX; x <= endX; x++){
                Tile tile = tmap.getTile(x,y);

                if(tile == null || tile.getCharacter() == '.'){
                    continue;
                }

                Rectangle tileRectangle = new Rectangle(
                        x * tileWidth, y * tileHeight, tileWidth, tileHeight
                );

                if(spriteRectangle.intersects(tileRectangle)){
                    collidedTiles.add(tile);

                    handleCollision(s,spriteRectangle,tileRectangle);
                }
            }
        }

    }

    public void handleCollision(Sprite s, Rectangle spriteRectangle, Rectangle tileRectangle) {
        int xOverlap = Math.min(
                spriteRectangle.x + spriteRectangle.width - tileRectangle.x,
                tileRectangle.x + tileRectangle.width - spriteRectangle.x
        );
        int yOverlap = Math.min(
                spriteRectangle.y + spriteRectangle.height - tileRectangle.y,
                tileRectangle.y + tileRectangle.height - spriteRectangle.y
        );
        final int bounceVelocity = 5;
        if (xOverlap < yOverlap) {
            // Horizontal collision
            if (spriteRectangle.x < tileRectangle.x) {
                s.setX(tileRectangle.x - spriteRectangle.width - 1);

                s.setVelocityX(-5); // Only stop if moving right into the wall


            } else {
                s.setX(tileRectangle.x + tileRectangle.width + 1);

                s.setVelocityX(+5); // Only stop if moving left into the wall


            }
        } else {
            // Vertical collision
            if (spriteRectangle.y < tileRectangle.y) {
                s.setY(tileRectangle.y - spriteRectangle.height);
                if (s.getVelocityY() > 0) {
                    s.setVelocityY(0); // Falling down onto a tile
                    this.canJump = true;

                }
            } else {
                s.setY(tileRectangle.y + tileRectangle.height);
                if (s.getVelocityY() < 0) {
                    s.setVelocityY(0); // Hitting head on underside of tile

                }
            }
        }
    }



    public void keyReleased(KeyEvent e) {

		int key = e.getKeyCode();

		switch (key)
		{
			case KeyEvent.VK_ESCAPE : stop(); break;
			case KeyEvent.VK_UP     : jump = false; break;
			case KeyEvent.VK_RIGHT  : moveRight = false; break;
            case KeyEvent.VK_LEFT   : moveLeft = false; break;

			default :  break;
		}
	}
}
