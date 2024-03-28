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
        mPaint.setTextSize(40); // Or whatever size you need
        mPaint.setColor(Color.WHITE); // Just an example color
        // Set the custom font for mPaint as well
        mPaint.setTypeface(customTypeface);



        // Call the constructors of our two game objects
        mApple = new Apple(context,
                new Point(NUM_BLOCKS_WIDE,
                        mNumBlocksHigh),
                blockSize);

        mSnake = new Snake(context,
                new Point(NUM_BLOCKS_WIDE,
                        mNumBlocksHigh),
                blockSize);

        // Initialize the Paint object for text
        textPaint = new Paint();
        textPaint.setTextSize(40); // Set the font size
        textPaint.setColor(Color.WHITE); // Set the text color
        textPaint.setTextAlign(Paint.Align.RIGHT); // Align text to the right
        textPaint.setTypeface(customTypeface);

        int buttonWidth = 200;
        int buttonHeight = 100;
        int buttonMargin = 50;
        pauseButtonRect = new Rect(size.x - buttonWidth - buttonMargin,
                buttonMargin,
                size.x - buttonMargin,
                buttonMargin + buttonHeight);
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

        // No need to set isGameStarted or mPaused here as they are handled in onTouchEvent
    }


    // Handles the game loop
    @Override
    public void run() {
        while (mPlaying) {
            if(!mPaused && updateRequired()) {
                update();
            }
            draw();
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

        // Move the snake
        mSnake.move();

        // Did the head of the snake eat the apple?
        if(mSnake.checkDinner(mApple.getLocation())){
            // This reminds me of Edge of Tomorrow.
            // One day the apple will be ready!
            mApple.spawn();

            // Add to  mScore
            mScore = mScore + 1;

            // Play a sound
            mSP.play(mEat_ID, 1, 1, 0, 0, 1);
        }

        // Did the snake die?
        if (mSnake.detectDeath()) {
            // Pause the game ready to start again
            mSP.play(mCrashID, 1, 1, 0, 0, 1);

            mPaused =true;
        }

    }


    // Do all the drawing

    // Do all the drawing
    public void draw() {
        // Get a lock on the mCanvas
        if (mSurfaceHolder.getSurface().isValid()) {
            mCanvas = mSurfaceHolder.lockCanvas();

            // Fill the screen with a color
            mCanvas.drawColor(Color.argb(255, 8, 143, 143));

            if (backgroundImage != null) {
                mCanvas.drawBitmap(backgroundImage, 0, 0, null);
            }

            // This makes the grid more visible on complex backgrounds
            mPaint.setColor(Color.argb(50, 0, 0, 0)); // Semi-transparent black
            mCanvas.drawRect(0, 0, mCanvas.getWidth(), mCanvas.getHeight(), mPaint);

            // Draw the grid
            drawGrid(mCanvas);

            // Set the size and color of the mPaint for the text
            mPaint.setColor(Color.argb(255, 255, 255, 255));
            mPaint.setTextSize(120);

            // Draw the score
            mCanvas.drawText("" + mScore, 20, 120, mPaint);

            // Draw the apple and the snake
            mApple.draw(mCanvas, mPaint);
            mSnake.draw(mCanvas, mPaint);

            // Draw names in the top right corner
            String names = "Arjun Bhargava & Navid Baghaei";
            int x = size.x - 20; // Assuming 'size' is your screen size. Adjust 20 as needed for the margin
            int y = (int) (textPaint.getTextSize() + 20); // Add some margin to the y-coordinate as well
            mCanvas.drawText(names, x, y, textPaint);

            // Draw "Paused" when the game is paused
            if (mPaused) {
                mPaint.setTextSize(250);
                mCanvas.drawText("Paused", 200, 700, mPaint);
            }

            // Draw "Tap to Play" when the game has not started
            if (!isGameStarted) {
                mPaint.setTextSize(250);
                mCanvas.drawText(getResources().getString(R.string.tap_to_play), 200, 700, mPaint);
            }

            drawPauseButton(mCanvas);  // Separate method to draw the pause button

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
}
