
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
    float	gravity = 0.0002f;
    float	moveSpeed = 0.04f;
    
    // Game state flags
    private boolean jump = false;
    private boolean playerGrounded = false;
    private boolean moveRight = false;
    private boolean moveLeft = false;
    private boolean debug = true;
    private boolean hasKey = false;
    private boolean hasWon = false;
    private float   bg1ScrollOffset = 0f;
    private boolean shouldRestart = false;
    private boolean bitcrusherWasEnabled = false;



    // Game resources

    private Animation idle;
    private Animation run;
    private Animation damaged;
    private Animation blueRun;
    private Animation idleYellow;

    private Sound hurt;
    private Sound boing;
    private Sound door;
    private Sound metal;

    private ArrayList<Image> parallaxLayers = new ArrayList<>();




    private Sprite	player;
    private Sprite goal;
    private ArrayList<Sprite> npcs = new ArrayList<>();

    /*
    Sprite npc1;
    Sprite npc2;
    Sprite npc3;
     */

    private ArrayList<Tile>		collidedTiles = new ArrayList<Tile>();
    private HashMap<Sprite, Boolean> groundedStates = new HashMap<>();


    private TileMap tmap = new TileMap();	// Our tile map, note that we load it in init()
    private TileMap tmap2 = new TileMap();

    private TileMap currentMap;



    private int playerHealth = 3;
    private long damageCooldown = 1000;
    private long lastDamageTaken = 0;

    private Sound backgroundMusic;


    /**
	 * The obligatory main method that creates
     * an instance of our class and starts it running
     * 
     * @param args	The list of parameters this program might use (ignored)
     */
    public static void main(String[] args) {

        Game gct = new Game();
        //Load music from the get go.
        gct.backgroundMusic = new Sound(Arrays.asList(
                "sounds/song1.mid",
                "sounds/song2.mid",
                "sounds/song3.mid"
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

        //Create a set of background sprites that we can
        //rearrange to give the illusion of motion
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

        idleYellow = new Animation();
        idleYellow.loadAnimationFromSheet("images/idleYellow.png",2,1,150);

        damaged = new Animation();
        damaged.loadAnimationFromSheet("images/damaged.png",6,1,100);

        idle.play();
        
        // Initialise the player + goal with an animation
        player = new Sprite(idle);
        goal = new Sprite(idleYellow);


        


        //Start
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
        //Set basic stats
        playerHealth = 3;
        currentMap = tmap;
        bg1ScrollOffset = 0f; // <-- Reset parallax scroll, kept speeding up paralax on death super annoying.
        hasKey = false;


        groundedStates.clear();
        collidedTiles.clear();

        goal.setPosition(1850,190);
        goal.show();

        player.setPosition(32,750);
        player.setVelocity(0,0);
        player.setFixedSize(26, 32); // example size — pick one that fits ALL animations

        player.show();

        npcs.clear();

        int[][] npcPositions = { //List of starting NPC positions.
                {380, 320},
                {580, 320},
                {880, 320}
        };

        for (int[] pos : npcPositions) { //Spawn NPCs
            Sprite npc = new Sprite(idle);
            npc.setPosition(pos[0], pos[1]);
            npc.setVelocity(0, 0);
            npc.setFixedSize(26, 30);
            npc.setVelocityX(0.02f);
            npc.setAnimation(blueRun);
            npc.flip(false);
            npc.show();

            npcs.add(npc);




        }
        //BG Music - resets distortion and starts from track 0 on death.
        backgroundMusic.stop();
        backgroundMusic.enableBitCrusher(false);
        backgroundMusic.startLooping();
        bitcrusherWasEnabled = false;

    }
    
    /**
     * Draw the current state of the game. Note the sample use of
     * debugging output that is drawn directly to the game screen.
     */
    public void draw(Graphics2D g)
    {    	



        int xo = -(int)player.getX() + 300;
        int yo = -(int)player.getY() + 220;

        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());


        //Grab base layer blue image and cover screen with it to have blue background.
        Image baseLayer = parallaxLayers.get(0);
        int baseW = baseLayer.getWidth(null);
        int baseH = baseLayer.getHeight(null);

        for (int x = 0; x < getWidth(); x += baseW) {
            for (int y = 0; y < getHeight(); y += baseH) {
                g.drawImage(baseLayer, x, y, null);
            }
        }


        //Now do paralax layers on that blue background and set layerY to be lower down.
        for (int i = 1; i < parallaxLayers.size(); i++) {
            Image layer = parallaxLayers.get(i);
            int imageWidth = layer.getWidth(null);


            float parallaxFactor = 0.2f + (i * 0.1f);
            int layerY = 170;

            // Add auto-scrolling to bg1 (index 1 ONLY).
            int layerX = (int)((xo * parallaxFactor + (i == 1 ? bg1ScrollOffset : 0)) % imageWidth);
            if (layerX > 0) layerX -= imageWidth;

            // As image is too small for screen tile it horizontally.
            for (int x = layerX; x < getWidth(); x += imageWidth) {
                g.drawImage(layer, x, layerY, null);
            }
        }

        // win condition.
        if (hasWon) {
            g.setColor(Color.GREEN);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("YOU WIN!", getWidth() / 2 - 60, 100);
        }

        // Apply offsets to tile map and draw  it
        currentMap.draw(g,xo,yo);

        // Apply offsets to player and draw 
        player.setOffsets(xo, yo);
        player.drawTransformed(g);

        //If on right map - show winning goal.
        if (currentMap == tmap) {
            goal.setOffsets(xo, yo);
            goal.drawTransformed(g);
        }


        //draw sprites
        for (Sprite npc : npcs) {
            npc.setOffsets(xo, yo);
            npc.drawTransformed(g);
        }



        // Show score and status information
        String msg = "E to interact!";
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
    //Update NPC positions and make them have collision detection + chase player. Jump if stuck against a wall.
    public void updateNpcs(long elapsed){
        for (Sprite character : npcs) {
            if (character == null || character == player) continue;

            float npcX = character.getX();
            float playerX = player.getX();
            float distance = Math.abs(npcX - playerX);
            boolean grounded = groundedStates.getOrDefault(character, false);

            //Skip if too far
            if (distance > 201) {
                character.setVelocityX(0);
                character.pauseAnimation();
                continue;
            }

            //Chase Player
            if (distance > 5) {
                float direction = Math.signum(playerX - npcX);
                character.setVelocityX(0.01f * direction);
                character.flip(direction < 0);
                character.playAnimation();
            } else {
                character.setVelocityX(0);
            }

            //Move Horizontally + Collide
            character.setX(character.getX() + character.getVelocityX() * elapsed);
            checkTileCollisionHorizontal(character, elapsed);

            //wall Detection + Jump if grounded
            float offset = character.getVelocityX() > 0 ? character.getWidth() : -1;
            int tileAheadX = (int)((character.getX() + offset) / currentMap.getTileWidth());
            int temp = (int)((character.getY() + character.getHeight() / 2) / currentMap.getTileHeight());

            if (isSolidTile(currentMap, tileAheadX, temp) && grounded) {
                character.setVelocityY(-0.07f);
                groundedStates.put(character, false);
                grounded = false;
            }

            //Apply gravity if not grounded like whats done to player
            if (!grounded) {
                character.setVelocityY(character.getVelocityY() + gravity * elapsed);
                character.setY(character.getY() + character.getVelocityY() * elapsed);
                checkTileCollisionVertical(character, elapsed);
            }

            //Ground state checks update hashmap
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
        //always clear from previous frame
        collidedTiles.clear();
        //scroll bg1 (index 1) slowly to the left. (the cloud in background.)
        bg1ScrollOffset -= 0.01f * elapsed;  // tune speed here

        //update npcs
        updateNpcs(elapsed);

        //handle jumping only if we’re currently grounded
        if (jump && playerGrounded) {
            boing.playOnce("sounds/boing.wav");
            player.setVelocityY(-0.1f);
            player.pauseAnimation();
            playerGrounded = false;      //No longer on the ground
        }

        //horizontal movement +animation for player
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

        //move player horizontally and check for collisions
        //move player horizontally only if moving
        if (player.getVelocityX() != 0) {
            player.setX(player.getX() + player.getVelocityX() * elapsed);
            checkTileCollisionHorizontal(player, elapsed);
        }

        //If grounded, check whether we are still on a valid tile
        if (playerGrounded && noTileUnderPlayer(player)) {
            playerGrounded = false;
        }

        //vertical movement for player only if not grounded
        if (!playerGrounded) {
            //apply gravity
            player.setVelocityY(player.getVelocityY() + (gravity * elapsed));

            //move and do vertical collision
            player.setY(player.getY() + player.getVelocityY() * elapsed);
            checkTileCollisionVertical(player, elapsed);
        }
        else {
            player.setVelocityY(0);
        }


        if (player.getVelocityY() > 0) {
            playerGrounded = false;
        }

        player.update(elapsed);


        for (Sprite npc : npcs) {
            if (boundingBoxCollision(player, npc)) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastDamageTaken > damageCooldown) {
                    playerHealth--;
                    lastDamageTaken = currentTime;

                    //update music based on playerhealth
                    if (playerHealth == 1 && !bitcrusherWasEnabled) {
                        backgroundMusic.enableBitCrusher(true);
                        backgroundMusic.stop();
                        backgroundMusic.startLooping();
                        bitcrusherWasEnabled = true;
                    } else if (playerHealth > 1 && bitcrusherWasEnabled) {
                        backgroundMusic.enableBitCrusher(false);
                        backgroundMusic.stop();

                        bitcrusherWasEnabled = false;
                    }

                    if (playerHealth < 1) {
                        backgroundMusic.stop();
                        shouldRestart = true;
                    }

                    hurt.playOnce("sounds/hurt.wav");
                }
                break; //only damage once per frame
            }
        }

        //check for goal collision (win condition)
        if (currentMap == tmap && !hasWon && boundingBoxCollision(player, goal)) {
            hasWon = true;
            player.setVelocity(0, 0);
            player.pauseAnimation();
            System.out.println("You reached the goal!");
        }




        handleScreenEdge(player, elapsed);
        if (shouldRestart) {
            shouldRestart = false;
            initialiseGame();
        }

    }


    public boolean noTileUnderPlayer(Sprite s) {

        float bottom = s.getY() + s.getHeight();
        float midX   = s.getX() + s.getWidth() / 2;

        int tileX = (int)(midX / currentMap.getTileWidth());
        int tileY = (int)(bottom / currentMap.getTileHeight());


        if (tileX < 0 || tileY < 0 ||
                tileX >= currentMap.getMapWidth() || tileY >= currentMap.getMapHeight()) {
            return true;
        }


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
    	float difference = s.getY() + s.getHeight() - tmap.getPixelHeight();
        if (difference > 0)
        {
        	s.setY(currentMap.getPixelHeight() - s.getHeight() - (int)(difference));
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
			case KeyEvent.VK_B 		: debug = !debug; break;
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
    

    public void checkTileCollisionHorizontal(Sprite s,long elapsed) {
        //amount we want to move this frame.
        float dx = s.getVelocityX() * elapsed;
        if (dx == 0) return; //no horizontal movemet so dont check.

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
        //Amount we want to move this frame
        float dy = s.getVelocityY() * elapsed;
        if (dy == 0) return;  //No vertical movement so nothing to check


        float newY = s.getY() + dy;


        float spriteLeft   = s.getX();
        float spriteRight  = s.getX() + s.getWidth() - 1;
        float spriteTop    = newY;
        float spriteBottom = newY + s.getHeight() - 1;


        int tileWidth  = currentMap.getTileWidth();
        int tileHeight = currentMap.getTileHeight();


        if (dy < 0)
        {

            int topTileY = (int)(spriteTop / tileHeight);


            int leftTileX  = (int)(spriteLeft  / tileWidth);
            int rightTileX = (int)(spriteRight / tileWidth);


            for (int tx = leftTileX; tx <= rightTileX; tx++)
            {
                if (isSolidTile(currentMap, tx, topTileY))
                {

                    newY = (topTileY + 1) * tileHeight;
                    s.setVelocityY(0);
                    collidedTiles.add(new Tile(currentMap.getTileChar(tx, topTileY), tx, topTileY));

                    break;
                }
            }
        }
        else if (dy > 0)
        {

            int bottomTileY = (int)(spriteBottom / tileHeight);


            int leftTileX  = (int)(spriteLeft  / tileWidth);
            int rightTileX = (int)(spriteRight / tileWidth);


            for (int tx = leftTileX; tx <= rightTileX; tx++)
            {
                if (isSolidTile(currentMap, tx, bottomTileY))
                {

                    newY = bottomTileY * tileHeight - (s.getHeight());
                    s.setVelocityY(0);
                    collidedTiles.add(new Tile(currentMap.getTileChar(tx, bottomTileY), tx, bottomTileY));

                    if(s.equals(player)){
                        this.playerGrounded = true;
                    }else {
                        groundedStates.put(s, true);
                    }
                    break;
                }
            }
        }


        s.setY(newY);
    }


    private boolean isSolidTile(TileMap currentMap, int tileX, int tileY) {
        //If tileX or tileY is out-of-bounds treat it as solid
        if (tileX < 0 || tileY < 0 || tileX >= currentMap.getMapWidth() || tileY >= currentMap.getMapHeight()) {
            return true;  //out of bounds => treat as solid
        }

        char tileChar = currentMap.getTileChar(tileX, tileY);
        if(hasKey){
            return (tileChar != 't' && tileChar != '.' && tileChar != 'x' && tileChar != 'o');
        }
        return (tileChar != '.' && tileChar != 'x' && tileChar != 'o');
    }

    public void tryInteract() {
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

                        player.setPosition(32,450);
                        System.out.print("Opened door ");
                        return;
                    } else if(tileChar == 'o'){
                        System.out.println("Player Has Key.");
                        metal.playOnce("sounds/metal.wav");
                        hasKey = true;
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
            case KeyEvent.VK_E    : tryInteract(); break; //interact added

			default :  break;
		}
	}
}
