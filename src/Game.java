
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;



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




    // Game resources

    Animation idle;
    Animation run;
    Animation damaged;
    Animation blueRun;

    Sound hurt;
    Sound boing;
    Sound door;

    ArrayList<Image> parallaxLayers = new ArrayList<>();
    private float bg1ScrollOffset = 0f;
    private boolean shouldRestart = false;



    Sprite	player = null;
    Sprite npc1 = null;
    Sprite npc2 = null;
    Sprite npc3 = null;



    ArrayList<Tile>		collidedTiles = new ArrayList<Tile>();

    ArrayList<Sprite> characterSprites = new ArrayList<>();
    HashMap<Sprite, Boolean> groundedStates = new HashMap<>();


    TileMap tmap = new TileMap();	// Our tile map, note that we load it in init()
    TileMap tmap2 = new TileMap();

    TileMap currentMap;


    long total;
    int playerHealth = 3;
    long damageCooldown = 1000;
    long lastDamageTaken = 0;

    private Sound backgroundMusic;


    /**
	 * The obligatory main method that creates
     * an instance of our class and starts it running
     * 
     * @param args	The list of parameters this program might use (ignored)
     */
    public static void main(String[] args) {

        Game gct = new Game();

        gct.backgroundMusic = new Sound(Arrays.asList(
                "sounds/music1.wav",
                "sounds/music2.wav",
                "sounds/music3.wav"
        ));
        gct.backgroundMusic.startLooping();

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





        // Load the tile map and print it out so we can check it is valid
        tmap.loadMap("maps", "map.txt");
        tmap2.loadMap("maps","map2.txt");


        currentMap = tmap;
        
        setSize(currentMap.getPixelWidth()/2, currentMap.getPixelHeight());
        setVisible(true);

        // Create a set of background sprites that we can 
        // rearrange to give the illusion of motion
        for (int i = 7; i >= 1; i--) {
            Image img = loadImage("images/bg" + i + ".png");
            parallaxLayers.add(img);
        }

        run = new Animation();
        run.loadAnimationFromSheet("images/run.png",8,1,85);

        blueRun = new Animation();
        blueRun.loadAnimationFromSheet("images/blueRun.png",8,1,85);

        idle = new Animation();
        idle.loadAnimationFromSheet("images/idle.png",2,1,150);

        damaged = new Animation();
        damaged.loadAnimationFromSheet("images/damaged.png",6,1,100);

        idle.play();
        
        // Initialise the player with an animation
        player = new Sprite(idle);
        npc1 = new Sprite(idle);
        npc2 = new Sprite(idle);
        npc3 = new Sprite(idle);

        



        initialiseGame();
      		
        System.out.println(currentMap);
    }

    /**
     * You will probably want to put code to restart a game in
     * a separate method so that you can call it when restarting
     * the game when the player loses.
     */
    public void initialiseGame()
    {
    	total = 0;
        playerHealth = 3;
        currentMap = tmap;
        bg1ScrollOffset = 0f; // <-- Reset parallax scroll, kept speeding up paralax on death super annoying.

        characterSprites.clear();
        groundedStates.clear();
        collidedTiles.clear();

        player.setPosition(32,750);
        player.setVelocity(0,0);
        player.setFixedSize(26, 32); // example size — pick one that fits ALL animations

        player.show();

        npc1.setPosition(380,320);
        npc1.setVelocity(0,0);
        npc1.show();
        npc1.setFixedSize(26, 30);
        npc1.setVelocityX(0.02f);  // small walking speed
        npc1.setAnimation(blueRun);    // walking animation
        npc1.flip(false);   // initially facing right


        npc2.setPosition(580,320);
        npc2.setVelocity(0,0);
        npc2.show();
        npc2.setFixedSize(26, 30);
        npc2.setVelocityX(0.02f);  // small walking speed
        npc2.setAnimation(blueRun);    // walking animation
        npc2.flip(false);

        npc3.setPosition(880,320);
        npc3.setVelocity(0,0);
        npc3.show();
        npc3.setFixedSize(26, 30);
        npc3.setVelocityX(0.02f);  // small walking speed
        npc3.setAnimation(blueRun);    // walking animation
        npc3.flip(false);

        characterSprites.add(npc1);
        characterSprites.add(npc2);
        characterSprites.add(npc3);

        groundedStates.put(npc1, false);
        groundedStates.put(npc2, false);
        groundedStates.put(npc3, false);


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
        int xo = -(int)player.getX() + 300;
        int yo = -(int)player.getY() + 220;

        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());

        Image baseLayer = parallaxLayers.get(0);
        int baseW = baseLayer.getWidth(null);
        int baseH = baseLayer.getHeight(null);

        for (int x = 0; x < getWidth(); x += baseW) {
            for (int y = 0; y < getHeight(); y += baseH) {
                g.drawImage(baseLayer, x, y, null);
            }
        }

        for (int i = 1; i < parallaxLayers.size(); i++) {
            Image layer = parallaxLayers.get(i);
            int imgW = layer.getWidth(null);


            float parallaxFactor = 0.2f + (i * 0.1f);
            int layerY = 170;

            // Add auto-scrolling to bg1 (index 1 ONLY).
            int layerX = (int)((xo * parallaxFactor + (i == 1 ? bg1ScrollOffset : 0)) % imgW);
            if (layerX > 0) layerX -= imgW;

            for (int x = layerX; x < getWidth(); x += imgW) {
                g.drawImage(layer, x, layerY, null);
            }
        }









        // Apply offsets to tile map and draw  it
        currentMap.draw(g,xo,yo);

        // Apply offsets to player and draw 
        player.setOffsets(xo, yo);
        player.drawTransformed(g);

        npc1.setOffsets(xo,yo);
        npc1.drawTransformed(g);

        npc2.setOffsets(xo,yo);
        npc2.drawTransformed(g);

        npc3.setOffsets(xo,yo);
        npc3.drawTransformed(g);

        
        // Show score and status information
        String msg = String.format("Score: %d", total/100);
        g.setColor(Color.darkGray);
        g.drawString(msg, getWidth() - 100, 50);

        g.setColor(Color.RED);
        String healthString = "Health: " + playerHealth;
        g.drawString(healthString, 20, 50);
        
        if (debug)
        {

        	// When in debug mode, you could draw borders around objects
            // and write messages to the screen with useful information.
            // Try to avoid printing to the console since it will produce 
            // a lot of output and slow down your game.
            currentMap.drawBorder(g, xo, yo, Color.black);

            g.setColor(Color.red);
        	player.drawBoundingBox(g);
        
        	g.drawString(String.format("Player: %.0f,%.0f", player.getX(),player.getY()),
        								getWidth() - 100, 70);
        	
        	drawCollidedTiles(g, xo, yo);
        }

    }

    public void drawCollidedTiles(Graphics2D g, int xOffset, int yOffset) {
        if (!collidedTiles.isEmpty())
        {
            int tileWidth = currentMap.getTileWidth();
            int tileHeight = currentMap.getTileHeight();

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

    public void updateNpcs(ArrayList<Sprite> characterSprites, long elapsed){
        for (Sprite character : characterSprites) {
            if (character == null || character == player) continue;

            float npcX = character.getX();
            float playerX = player.getX();
            float distance = Math.abs(npcX - playerX);
            boolean grounded = groundedStates.getOrDefault(character, false);

            // --- Skip if too far ---
            if (distance > 201) {
                character.setVelocityX(0);
                character.pauseAnimation();
                continue;
            }

            // --- Chase Player ---
            if (distance > 5) {
                float direction = Math.signum(playerX - npcX);
                character.setVelocityX(0.01f * direction);
                character.flip(direction < 0);
                character.playAnimation();
            } else {
                character.setVelocityX(0);
            }

            // --- Move Horizontally + Collide ---
            character.setX(character.getX() + character.getVelocityX() * elapsed);
            checkTileCollisionHorizontal(character, elapsed);

            // --- Wall Detection + Jump if grounded ---
            float offset = character.getVelocityX() > 0 ? character.getWidth() : -1;
            int tileAheadX = (int)((character.getX() + offset) / currentMap.getTileWidth());
            int tileMidY = (int)((character.getY() + character.getHeight() / 2) / currentMap.getTileHeight());

            if (isSolidTile(currentMap, tileAheadX, tileMidY) && grounded) {
                character.setVelocityY(-0.07f);
                groundedStates.put(character, false);
                grounded = false;
            }

            // --- Apply gravity if not grounded ---
            if (!grounded) {
                character.setVelocityY(character.getVelocityY() + gravity * elapsed);
                character.setY(character.getY() + character.getVelocityY() * elapsed);
                checkTileCollisionVertical(character, elapsed);
            }

            // --- Ground state checks ---
            if (grounded && noTileUnderPlayer(character)) groundedStates.put(character, false);
            if (character.getVelocityY() > 0) groundedStates.put(character, false);

            character.update(elapsed);
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
        // Scroll bg1 (index 1) slowly to the left
        bg1ScrollOffset -= 0.01f * elapsed;  // tune speed here


        updateNpcs(characterSprites,elapsed);






        // -----------------------------------------------------------
        // 2) Handle jumping (only if we’re currently grounded)
        // -----------------------------------------------------------
        if (jump && playerGrounded) {
            boing.playOnce("sounds/boing.wav");
            player.setVelocityY(-0.1f);
            player.pauseAnimation();     // Optional: pause animation during jump
            playerGrounded = false;      // No longer on the ground once we jump
        }

        // -----------------------------------------------------------
        // 3) Horizontal movement & animation for player
        // -----------------------------------------------------------
        boolean recentlyDamaged = System.currentTimeMillis() - lastDamageTaken < 500;
        if (recentlyDamaged) {
            player.setAnimation(damaged);
            player.playAnimation();
        } else {
            if (moveRight) {
                if (playerGrounded) player.playAnimation();
                player.setAnimation(run);
                player.flip(false);
                player.setVelocityX(moveSpeed);

            } else if (moveLeft) {
                if (playerGrounded) player.playAnimation();
                player.setAnimation(run);
                player.flip(true);
                player.setVelocityX(-moveSpeed);

            } else {
                player.setVelocityX(0);
                player.setAnimation(idle);
                player.playAnimation();
            }
        }

        // Move player horizontally and check for collisions
        // Move player horizontally only if moving
        if (player.getVelocityX() != 0) {
            player.setX(player.getX() + player.getVelocityX() * elapsed);
            checkTileCollisionHorizontal(player, elapsed);
        }


        // -----------------------------------------------------------
        // 4) If grounded, check whether we are still on a valid tile
        //    (i.e. not walking off the edge). If not, become ungrounded.
        // -----------------------------------------------------------
        if (playerGrounded && noTileUnderPlayer(player)) {
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
            checkTileCollisionVertical(player, elapsed);
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


        if (boundingBoxCollision(player, npc1)) {
            long currentTime = System.currentTimeMillis();
            if(currentTime - lastDamageTaken > damageCooldown){
                playerHealth--;
                lastDamageTaken = currentTime;

                if(playerHealth < 1){
                    backgroundMusic.stop();
                    shouldRestart = true;

                }

                hurt.playOnce("sounds/hurt.wav");
            }

        }

        // Optional: keep player from going off bottom of map, etc.
        handleScreenEdge(player, elapsed);
        if (shouldRestart) {
            shouldRestart = false;
            initialiseGame(); // ← only this, not `init()` which stacks everything again
        }

    }


    boolean noTileUnderPlayer(Sprite s) {
        // Check the bottom center of the player, for instance
        float bottom = s.getY() + s.getHeight();
        float midX   = s.getX() + s.getWidth() / 2;

        int tileX = (int)(midX / currentMap.getTileWidth());
        int tileY = (int)(bottom / currentMap.getTileHeight());

        // If out of bounds, count it as "no tile."
        if (tileX < 0 || tileY < 0 ||
                tileX >= currentMap.getMapWidth() || tileY >= currentMap.getMapHeight()) {
            return true;
        }

        // Or: return !isSolidTile(tileX, tileY)
        char tile = currentMap.getTileChar(tileX, tileY);
        return (tile == '.');
    }


    /**
     * Checks and handles collisions with the edge of the screen. You should generally
     * use tile map collisions to prevent the player leaving the game area. This method
     * is only included as a temporary measure until you have properly developed your
     * tile maps.
     * 
     * @param s			The Sprite to check collisions for
     *
     * @param elapsed	How much time has gone by since the last call
     */
    public void handleScreenEdge(Sprite s, long elapsed)
    {
    	// This method just checks if the sprite has gone off the bottom screen.
    	// Ideally you should use tile collision instead of this approach
    	
    	float difference = s.getY() + s.getHeight() - tmap.getPixelHeight();
        if (difference > 0)
        {
        	// Put the player back on the map according to how far over they were
        	s.setY(currentMap.getPixelHeight() - s.getHeight() - (int)(difference));
        	
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
     *
     */
    public void checkTileCollisionHorizontal(Sprite s,long elapsed) {
        float dx = s.getVelocityX() * elapsed;
        if (dx == 0) return;

        float newX = s.getX() + dx;

        float spriteLeft   = newX;
        float spriteRight  = newX + s.getWidth() - 1;
        float spriteTop    = s.getY();
        float spriteBottom = s.getY() + s.getHeight() - 1;

        int tileWidth  = currentMap.getTileWidth();
        int tileHeight = currentMap.getTileHeight();

        if (dx < 0) {
            int leftTileX = (int)(spriteLeft / tileWidth);
            int topTileY = (int)(spriteTop / tileHeight);
            int bottomTileY = (int)(spriteBottom / tileHeight);

            for (int ty = topTileY; ty <= bottomTileY; ty++) {
                if (isSolidTile(currentMap, leftTileX, ty)) {
                    newX = (leftTileX + 1) * tileWidth;
                    collidedTiles.add(new Tile(currentMap.getTileChar(leftTileX, ty), leftTileX, ty));
                    s.setVelocityX(0);


                    break;
                }
            }
        } else if (dx > 0) {
            int rightTileX = (int)(spriteRight / tileWidth);
            int topTileY = (int)(spriteTop / tileHeight);
            int bottomTileY = (int)(spriteBottom / tileHeight);

            for (int ty = topTileY; ty <= bottomTileY; ty++) {
                if (isSolidTile(currentMap, rightTileX, ty)) {
                    newX = (rightTileX * tileWidth) - s.getWidth();
                    collidedTiles.add(new Tile(currentMap.getTileChar(rightTileX, ty), rightTileX, ty));
                    s.setVelocityX(0);


                    break;
                }
            }
        }

        s.setX(newX);
    }









    public void checkTileCollisionVertical(Sprite s,long elapsed) {
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
        int tileWidth  = currentMap.getTileWidth();
        int tileHeight = currentMap.getTileHeight();

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
                if (isSolidTile(currentMap, tx, topTileY))
                {
                    // We collided: clamp the Y so the sprite is just below this tile
                    newY = (topTileY + 1) * tileHeight;
                    s.setVelocityY(0);
                    collidedTiles.add(new Tile(currentMap.getTileChar(tx, topTileY), tx, topTileY));

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
                if (isSolidTile(currentMap, tx, bottomTileY))
                {
                    // Collided: clamp so the sprite is just on top of the tile
                    newY = bottomTileY * tileHeight - (s.getHeight());
                    s.setVelocityY(0);
                    collidedTiles.add(new Tile(currentMap.getTileChar(tx, bottomTileY), tx, bottomTileY));
                    // We’ve landed on something, so we can jump again
                    if(s.equals(player)){
                        this.playerGrounded = true;
                    }else {
                        groundedStates.put(s, true);
                    }
                    break; // no need to keep checking
                }
            }
        }

        // Finally, apply that corrected newY
        s.setY(newY);
    }


    private boolean isSolidTile(TileMap currentMap, int tileX, int tileY) {
        // If tileX or tileY is out-of-bounds, treat it as solid, or adjust to your liking
        if (tileX < 0 || tileY < 0 || tileX >= currentMap.getMapWidth() || tileY >= currentMap.getMapHeight()) {
            return true;  // out of bounds => treat as solid
        }

        char tileChar = currentMap.getTileChar(tileX, tileY);

        return (tileChar != '.' && tileChar != 'x');
    }

    public void tryOpenDoor() {
        int tileWidth = currentMap.getTileWidth();
        int tileHeight = currentMap.getTileHeight();

        int leftTile   = (int)(player.getX() / tileWidth);
        int rightTile  = (int)((player.getX() + player.getWidth()) / tileWidth);
        int topTile    = (int)(player.getY() / tileHeight);
        int bottomTile = (int)((player.getY() + player.getHeight()) / tileHeight);

        for (int tx = leftTile; tx <= rightTile; tx++) {
            for (int ty = topTile; ty <= bottomTile; ty++) {
                // Bounds check
                if (tx >= 0 && ty >= 0 && tx < currentMap.getMapWidth() && ty < currentMap.getMapHeight()) {
                    char tileChar = currentMap.getTileChar(tx, ty);
                    if (tileChar == 'x') {
                        door.playOnce("sounds/door.wav");
                        if (currentMap == tmap) {
                            currentMap = tmap2;
                        } else {
                            currentMap = tmap;
                        }

                        player.setPosition(32,750);
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
