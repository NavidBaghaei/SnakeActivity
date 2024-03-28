package csc133.snakeactivity;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;
import java.util.ArrayList;

class SnakeGame extends SurfaceView implements Runnable{

    // Objects for the game loop/thread
    private Thread mThread = null;
    // Control pausing between updates
    private long mNextFrameTime;
    // Is the game currently playing and or paused?
    private volatile boolean mPlaying = false;
    private volatile boolean mPaused = true;

    // for playing sound effects
    private SoundPool mSP;
    private int mEat_ID = -1;
    private int mCrashID = -1;

    // The size in segments of the playable area
    private final int NUM_BLOCKS_WIDE = 40;
    private int mNumBlocksHigh;

    // How many points does the player have
    private int mScore;

    // Objects for drawing
    private Canvas mCanvas;
    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;

    private Paint textPaint;

    private Snake mSnake;

    private Apple mApple;

    private Bitmap backgroundImage;

    private Point size;

    private Rect pauseButtonRect;
    private String pauseButtonText = "Pause";

    private boolean isGameStarted = false;
    private boolean speedBoost = false;
    private int speedBoostUpdatesRemaining = 0;
    private ArrayList<GameObject> gameObjects;


    // This is the constructor method that gets called
    // from SnakeActivity
    public SnakeGame(Context context, Point size) {
        super(context);
        this.size = size;
        Typeface customTypeface = ResourcesCompat.getFont(context, R.font.workbench);
        // Work out how many pixels each block is
        int blockSize = size.x / NUM_BLOCKS_WIDE;
        // How many blocks of the same size will fit into the height
        mNumBlocksHigh = size.y / blockSize;

        // Initialize the SoundPool
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            mSP = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            mSP = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }
        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Prepare the sounds in memory
            descriptor = assetManager.openFd("get_apple.ogg");
            mEat_ID = mSP.load(descriptor, 0);

            descriptor = assetManager.openFd("snake_death.ogg");
            mCrashID = mSP.load(descriptor, 0);

        } catch (IOException e) {
            // Error
        }
        // Load the background image from drawable resources
        backgroundImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.snake1);
        Log.d("SnakeGame", "Background image loaded: " + (backgroundImage != null));
        // Scale the background image to fit the screen
        backgroundImage = Bitmap.createScaledBitmap(backgroundImage, size.x, size.y, false);


        // Initialize the drawing objects
        mSurfaceHolder = getHolder();
        mPaint = new Paint();
        mPaint.setTextSize(40);
        mPaint.setColor(Color.WHITE);
        mPaint.setTypeface(customTypeface);


        // Call the constructors of the two game objects
        mApple = new Apple(context,
                new Point(NUM_BLOCKS_WIDE,
                        mNumBlocksHigh),
                blockSize);

        mSnake = new Snake(context,
                new Point(NUM_BLOCKS_WIDE,
                        mNumBlocksHigh),
                blockSize);

        // Initialize gameObjects and add game entities
        gameObjects = new ArrayList<>();
        gameObjects.add(mSnake);
        gameObjects.add(mApple);

        // Initialize the Paint object for text
        textPaint = new Paint();
        textPaint.setTextSize(40); // Set the font size
        textPaint.setColor(Color.WHITE); // Set the text color
        textPaint.setTextAlign(Paint.Align.RIGHT); // Align text to the right
        textPaint.setTypeface(customTypeface);

        int buttonWidth = 200;
        int buttonHeight = 75;
        int buttonMargin = 50;
        int yOffset = 25;
        pauseButtonRect = new Rect(
                size.x - buttonWidth - buttonMargin,
                buttonMargin + yOffset, // Moved down by yOffset
                size.x - buttonMargin,
                buttonMargin + buttonHeight + yOffset // Moved down by yOffset
        );
    }

    // Called to start a new game
    public void newGame() {
        // reset the snake
        mSnake.reset(NUM_BLOCKS_WIDE, mNumBlocksHigh);

        // Get the apple ready for dinner
        mApple.spawn();

        // Reset the mScore
        mScore = 0;

        // Setup mNextFrameTime so an update can be triggered
        mNextFrameTime = System.currentTimeMillis();

        isGameStarted = true; // Now the game has started
        mPaused = false; // Game starts unpaused

        speedBoost = false;
    }


    // Handles the game loop

    @Override
    public void run() {
        while (mPlaying) {
            if (!mPaused && updateRequired()) {
                update(); // This method should update the game state, including moving objects
            }
            draw();
        }
    }


    private void updateGameObjects() {
        for (GameObject obj : gameObjects) {
            obj.update();
        }
    }


    private void drawGameObjects() {
        if (mSurfaceHolder.getSurface().isValid()) {
            mCanvas = mSurfaceHolder.lockCanvas();

            // Draw each game object
            for (GameObject obj : gameObjects) {
                obj.draw(mCanvas, mPaint);
            }

            // Finalize drawing
            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
        }
    }


    // Check to see if it is time for an update
    public boolean updateRequired() {

        // Run at 10 frames per second
        final long TARGET_FPS = 10;
        // There are 1000 milliseconds in a second
        final long MILLIS_PER_SECOND = 1000;

        // Are we due to update the frame
        if(mNextFrameTime <= System.currentTimeMillis()){
            // Tenth of a second has passed

            // Setup when the next update will be triggered
            mNextFrameTime =System.currentTimeMillis()
                    + MILLIS_PER_SECOND / TARGET_FPS;

            // Return true so that the update and draw
            // methods are executed
            return true;
        }

        return false;
    }



    // Update all the game objects
    public void update() {
        // If speed boost is active, apply it and decrement the counter
        mSnake.move(speedBoostUpdatesRemaining > 0);

        // Decrease the number of updates remaining for the speed boost
        if (speedBoostUpdatesRemaining > 0) {
            speedBoostUpdatesRemaining--;
        }

        // Rest of the update logic...
        // Check for apple collision to activate the speed boost
        if(mSnake.checkDinner(mApple.getLocation())){
            mApple.spawn();
            mScore += 1;
            speedBoostUpdatesRemaining = 20; // Reset to 2 seconds worth of updates
            mSP.play(mEat_ID, 1, 1, 0, 0, 1);
        }

        // Check if the snake has died
        if (mSnake.detectDeath()) {
            // Play the death sound and pause the game
            mSP.play(mCrashID, 1, 1, 0, 0, 1);
            // Reset the speed boost so it doesn't carry over
            speedBoostUpdatesRemaining = 0;
            mPaused = true;
            isGameStarted = false;
        }
    }


    // Do all the drawing
    public void draw() {
        // First, validate that we have a valid drawing surface
        if (mSurfaceHolder.getSurface().isValid()) {
            // Lock the canvas for drawing
            mCanvas = mSurfaceHolder.lockCanvas();

            // Clear the screen with a specific color
            mCanvas.drawColor(Color.argb(255, 8, 143, 143));

            // If there's a background image, draw it on the entire canvas
            if (backgroundImage != null) {
                mCanvas.drawBitmap(backgroundImage, 0, 0, null);
            }

            // Optionally, draw a semi-transparent overlay to enhance grid visibility
            mPaint.setColor(Color.argb(50, 0, 0, 0));
            mCanvas.drawRect(0, 0, mCanvas.getWidth(), mCanvas.getHeight(), mPaint);

            // Draw the game grid
            drawGrid(mCanvas);

            // Configure paint for drawing text (score, names, game status messages)
            mPaint.setColor(Color.argb(255, 255, 255, 255));
            mPaint.setTextSize(120);

            // Draw the current score
            mCanvas.drawText("" + mScore, 20, 120, mPaint);

            // Draw game objects: apple and snake. Make sure they implement GameObject and draw themselves
            mApple.draw(mCanvas, mPaint);
            mSnake.draw(mCanvas, mPaint);

            // Draw developer names or any other info in the top right corner
            String names = "Arjun Bhargava & Navid Baghaei";
            int x = size.x - 20; // Margin from the right edge
            int y = (int) (textPaint.getTextSize() + 20); // Margin from the top
            mCanvas.drawText(names, x, y, textPaint);

            // Display a "Paused" message when the game is paused
            if (mPaused && isGameStarted) {
                mPaint.setTextSize(250);
                mCanvas.drawText("Paused", 200, 700, mPaint);
            }

            // Show "Tap to Play" when the game has not started or after the player dies
            if (!isGameStarted || (!mPaused && !isGameStarted)) {
                mPaint.setTextSize(250);
                mCanvas.drawText(getResources().getString(R.string.tap_to_play), 200, 700, mPaint);
            }

            // Draw the pause button UI
            drawPauseButton(mCanvas);

            // Finally, post the canvas to the drawing surface
            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
        }
    }





    private void drawPauseButton(Canvas canvas) {
        // Set the color for the button with reduced alpha for transparency
        mPaint.setColor(Color.argb(128, 255, 255, 255)); // Half transparent white
        mPaint.setTextSize(35);

        // Draw the rectangle for the button
        canvas.drawRect(pauseButtonRect, mPaint);

        // Draw the text on the button
        mPaint.setColor(Color.BLACK); // No need to make the text transparent
        float textWidth = mPaint.measureText(pauseButtonText);
        int textX = pauseButtonRect.left + (pauseButtonRect.width() - (int) textWidth) / 2;
        int textY = pauseButtonRect.top + (pauseButtonRect.height() + 30) / 2;
        canvas.drawText(pauseButtonText, textX, textY, mPaint);
    }


    private void drawGrid(Canvas canvas) {
        int gridSize = 20; //  determines the size of your grid
        float colWidth = canvas.getWidth() / (float) gridSize;
        float rowHeight = canvas.getHeight() / (float) gridSize;

        // Set grid line color and stroke width
        Paint paint = new Paint();
        paint.setColor(Color.argb(255, 60, 60, 160)); // Dark gray color for the grid lines
        paint.setStrokeWidth(0.5f); // Thin lines for the grid

        // Draw vertical grid lines
        for (int col = 0; col <= gridSize; col++) {
            canvas.drawLine(col * colWidth, 0, col * colWidth, canvas.getHeight(), paint);
        }

        // Draw horizontal grid lines
        for (int row = 0; row <= gridSize; row++) {
            canvas.drawLine(0, row * rowHeight, canvas.getWidth(), row * rowHeight, paint);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();

            // Check if the pause button is pressed
            if (pauseButtonRect.contains(x, y) && isGameStarted) {
                mPaused = !mPaused;
                pauseButtonText = mPaused ? "Resume" : "Pause";
                return true;
            } else if (!isGameStarted) {
                // Start a new game with the first screen tap
                newGame();
                isGameStarted = true;
                mPaused = false; // Ensure the game starts unpaused
                mPlaying = true; // Make sure the game loop is running
                if (mThread == null || !mThread.isAlive()) {
                    mThread = new Thread(this);
                    mThread.start();
                }
                return true;
            } else if (!mPaused) {
                // Let the Snake class handle the input for direction change if the game is ongoing and not paused
                mSnake.switchHeading(motionEvent);
                return true;
            }
        }
        return super.onTouchEvent(motionEvent);
    }



    // Stop the thread
    public void pause() {
        mPlaying = false;
        try {
            mThread.join();
        } catch (InterruptedException e) {
            // Error
        }
    }


    // Start the thread
    public void resume() {
        mPlaying = true;
        mThread = new Thread(this);
        mThread.start();
    }

    public Snake getSnake() {
        return mSnake;
    }

}