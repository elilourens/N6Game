
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import java.awt.Rectangle;


import game2D.*;

//import static sun.jvm.hotspot.gc.shared.CollectedHeapName.EPSILON;

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

    float	moveSpeed = 0.04f;
    
    // Game state flags
    boolean jump = false;
    boolean playerGrounded = false;
    boolean moveRight = false;
    boolean moveLeft = false;
    boolean debug = true;		

    //npc1
    boolean npcMovingRight = true;

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
    TileMap tmapp2 = new TileMap();

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
        //tmap2.loadMap("maps","map2.txt");
        
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

        player.setPosition(32,750);
        player.setVelocity(0,0);
        player.setFixedSize(38, 30); // example size — pick one that fits ALL animations

        player.show();

        npc1.setPosition(340,280);
        npc1.setVelocity(0,0);
        npc1.show();
        npc1.setFixedSize(38, 30);
        npc1.setVelocityX(0.02f);  // small walking speed
        npc1.setAnimation(run);    // walking animation
        npc1.flip(false);   // initially facing right

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

    public void drawCollidedTiles(Graphics2D g, TileMap map, int xOffset, int yOffset) {
        if (!collidedTiles.isEmpty())
        {
            int tileWidth = map.getTileWidth();
            int tileHeight = map.getTileHeight();

            g.setColor(Color.blue);

            for (Tile t : collidedTiles)
            {
                // Convert tile coordinate to pixel position
                int px = t.getXC() * tileWidth;
                int py = t.getYC() * tileHeight;

                // Then draw the rectangle using the pixel coordinate + camera offset
                g.drawRect(px + xOffset, py + yOffset, tileWidth, tileHeight);
            }
        }
    }
	
    /**
     * Update any sprites and check for collisions
     * 
     * @param elapsed The elapsed time between this call and the previous call of elapsed
     */
    public void update(long elapsed) {
        // Always clear from previous frame
        collidedTiles.clear();

        // -----------------------------------------------------------
        // 1) NPC: simple gravity & movement (no skipping vertical collisions here)
        // -----------------------------------------------------------
        npc1.setVelocityY(npc1.getVelocityY() + (gravity * elapsed));
        npc1.setY(npc1.getY() + npc1.getVelocityY() * elapsed);
        checkTileCollisionVertical(npc1, tmap, elapsed);

        npc1.setX(npc1.getX() + npc1.getVelocityX() * elapsed);
        checkTileCollisionHorizontal(npc1, tmap, elapsed);

        // -----------------------------------------------------------
        // 2) Handle jumping (only if we’re currently grounded)
        // -----------------------------------------------------------
        if (jump && playerGrounded) {
            player.setVelocityY(-0.1f);
            player.pauseAnimation();     // Optional: pause animation during jump
            playerGrounded = false;      // No longer on the ground once we jump
        }

        // -----------------------------------------------------------
        // 3) Horizontal movement & animation for player
        // -----------------------------------------------------------
        if (moveRight) {
            if (playerGrounded) {
                player.playAnimation();
            }
            player.setAnimation(run);
            player.flip(false);
            player.setVelocityX(moveSpeed);

        } else if (moveLeft) {
            if (playerGrounded) {
                player.playAnimation();
            }
            player.setAnimation(run);
            player.flip(true);
            player.setVelocityX(-moveSpeed);

        } else {
            // Not pressing left or right
            player.setVelocityX(0);
            player.setAnimation(idle);
            player.playAnimation();
        }

        // Move player horizontally and check for collisions
        player.setX(player.getX() + player.getVelocityX() * elapsed);
        checkTileCollisionHorizontal(player, tmap, elapsed);

        // -----------------------------------------------------------
        // 4) If grounded, check whether we are still on a valid tile
        //    (i.e. not walking off the edge). If not, become ungrounded.
        // -----------------------------------------------------------
        if (playerGrounded && noTileUnderPlayer(player, tmap)) {
            playerGrounded = false;
        }

        // -----------------------------------------------------------
        // 5) Vertical movement for player only if not grounded
        // -----------------------------------------------------------
        if (!playerGrounded) {
            // Apply gravity
            player.setVelocityY(player.getVelocityY() + (gravity * elapsed));

            // Move and do vertical collision
            player.setY(player.getY() + player.getVelocityY() * elapsed);
            checkTileCollisionVertical(player, tmap, elapsed);
        }
        else {
            // If we’re on the ground, ensure we’re not accumulating downward velocity
            player.setVelocityY(0);
        }

        // If velocity is positive (moving down), definitely not on the ground
        if (player.getVelocityY() > 0) {
            playerGrounded = false;
        }

        // -----------------------------------------------------------
        // 6) Update animations, check collisions, etc.
        // -----------------------------------------------------------
        player.update(elapsed);
        npc1.update(elapsed);

        if (boundingBoxCollision(player, npc1)) {
            npc1.hide();  // Example collision response
        }

        // Optional: keep player from going off bottom of map, etc.
        handleScreenEdge(player, tmap, elapsed);
    }


    boolean noTileUnderPlayer(Sprite s, TileMap tmap) {
        // Check the bottom center of the player, for instance
        float bottom = s.getY() + s.getHeight();
        float midX   = s.getX() + s.getWidth() / 2;

        int tileX = (int)(midX / tmap.getTileWidth());
        int tileY = (int)(bottom / tmap.getTileHeight());

        // If out of bounds, count it as "no tile."
        if (tileX < 0 || tileY < 0 ||
                tileX >= tmap.getMapWidth() || tileY >= tmap.getMapHeight()) {
            return true;
        }

        // Or: return !isSolidTile(tileX, tileY)
        char tile = tmap.getTileChar(tileX, tileY);
        return (tile == '.');
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
    public void checkTileCollisionHorizontal(Sprite s, TileMap tmap,long elapsed) {
        // Amount we want to move this frame
        float dx = s.getVelocityX() * elapsed;
        if (dx == 0) return;  // No horizontal movement

        // Proposed new X
        float newX = s.getX() + dx;

        // Sprite bounding box in new horizontal position
        float spriteLeft   = newX;
        float spriteRight  = newX + s.getWidth() - 1;
        float spriteTop    = s.getY();
        float spriteBottom = s.getY() + s.getHeight() - 1;

        int tileWidth  = tmap.getTileWidth();
        int tileHeight = tmap.getTileHeight();

        if (dx < 0) //moving left?
        {
            int leftTileX = (int)(spriteLeft / tileWidth);
            int topTileY = (int)(spriteTop / tileHeight);
            int bottomTileY = (int)(spriteBottom / tileHeight);

            for (int ty = topTileY; ty <= bottomTileY; ty++)
            {
                if (isSolidTile(tmap, leftTileX, ty))
                {
                    // collided: clamp sprite just to the right of that tile
                    newX = (leftTileX + 1) * tileWidth;
                    collidedTiles.add(new Tile(tmap.getTileChar(leftTileX, ty), leftTileX, ty));
                    s.setVelocityX(0);
                    break;
                }
            }
        }

        else if (dx > 0) //moving right?
        {
            int rightTileX = (int)(spriteRight / tileWidth);
            int topTileY = (int)(spriteTop / tileHeight);
            int bottomTileY = (int)(spriteBottom / tileHeight);

            for (int ty = topTileY; ty <= bottomTileY; ty++)
            {
                if (isSolidTile(tmap, rightTileX, ty))
                {
                    // Collided: clamp so sprite is just to left of that tile
                    newX = (rightTileX * tileWidth) - s.getWidth();
                    collidedTiles.add(new Tile(tmap.getTileChar(rightTileX, ty), rightTileX, ty));
                    s.setVelocityX(0);
                    break;
                }
            }
        }


        s.setX(newX);
    }








    public void checkTileCollisionVertical(Sprite s, TileMap tmap,long elapsed) {
        // Amount we want to move this frame
        float dy = s.getVelocityY() * elapsed;
        if (dy == 0) return;  // No vertical movement, so nothing to check

        // Proposed new Y position
        float newY = s.getY() + dy;

        // Current bounding box edges of sprite
        float spriteLeft   = s.getX();
        float spriteRight  = s.getX() + s.getWidth() - 1;
        float spriteTop    = newY;
        float spriteBottom = newY + s.getHeight() - 1;

        // Convert all these edges to tile coordinates
        int tileWidth  = tmap.getTileWidth();
        int tileHeight = tmap.getTileHeight();

        // We’ll check the left and right corners of the top/bottom
        //   depending on the direction of movement
        if (dy < 0) //moving left?
        {
            // Moving UP: check top edge
            int topTileY = (int)(spriteTop / tileHeight);

            // Left & right tile column
            int leftTileX  = (int)(spriteLeft  / tileWidth);
            int rightTileX = (int)(spriteRight / tileWidth);

            // Check each tile from leftTileX..rightTileX at row topTileY
            for (int tx = leftTileX; tx <= rightTileX; tx++)
            {
                if (isSolidTile(tmap, tx, topTileY))
                {
                    // We collided: clamp the Y so the sprite is just below this tile
                    newY = (topTileY + 1) * tileHeight;
                    s.setVelocityY(0);
                    collidedTiles.add(new Tile(tmap.getTileChar(tx, topTileY), tx, topTileY));

                    break; // no need to keep checking
                }
            }
        }
        else if (dy > 0) //moving right?
        {
            // Moving DOWN: check bottom edge
            int bottomTileY = (int)(spriteBottom / tileHeight);

            // Left & right tile column
            int leftTileX  = (int)(spriteLeft  / tileWidth);
            int rightTileX = (int)(spriteRight / tileWidth);

            // Check each tile from leftTileX..rightTileX at row bottomTileY
            for (int tx = leftTileX; tx <= rightTileX; tx++)
            {
                if (isSolidTile(tmap, tx, bottomTileY))
                {
                    // Collided: clamp so the sprite is just on top of the tile
                    newY = bottomTileY * tileHeight - (s.getHeight());
                    s.setVelocityY(0);
                    collidedTiles.add(new Tile(tmap.getTileChar(tx, bottomTileY), tx, bottomTileY));
                    // We’ve landed on something, so we can jump again
                    if(s.equals(player)){
                        this.playerGrounded = true;
                    }
                    break; // no need to keep checking
                }
            }
        }

        // Finally, apply that corrected newY
        s.setY(newY);
    }


    private boolean isSolidTile(TileMap tmap, int tileX, int tileY) {
        // If tileX or tileY is out-of-bounds, treat it as solid, or adjust to your liking
        if (tileX < 0 || tileY < 0 || tileX >= tmap.getMapWidth() || tileY >= tmap.getMapHeight()) {
            return true;  // out of bounds => treat as solid
        }

        char tileChar = tmap.getTileChar(tileX, tileY);
        // Suppose '.' is empty, everything else (like '#', '?', etc.) is solid:
        return (tileChar != '.' && tileChar != 'x');
    }

    public void tryOpenDoor() {
        int tileWidth = tmap.getTileWidth();
        int tileHeight = tmap.getTileHeight();

        int leftTile   = (int)(player.getX() / tileWidth);
        int rightTile  = (int)((player.getX() + player.getWidth()) / tileWidth);
        int topTile    = (int)(player.getY() / tileHeight);
        int bottomTile = (int)((player.getY() + player.getHeight()) / tileHeight);

        for (int tx = leftTile; tx <= rightTile; tx++) {
            for (int ty = topTile; ty <= bottomTile; ty++) {
                // Bounds check
                if (tx >= 0 && ty >= 0 && tx < tmap.getMapWidth() && ty < tmap.getMapHeight()) {
                    char tileChar = tmap.getTileChar(tx, ty);
                    if (tileChar == 'x') {

                        System.out.printf("Opened door at (%d,%d)%n", tx, ty);
                        return;
                    }
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
            case KeyEvent.VK_E    : tryOpenDoor(); break;

			default :  break;
		}
	}
}
