package csc133.snakeactivity;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
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
import android.content.SharedPreferences;
import android.os.Handler;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

class SnakeGame extends SurfaceView implements Runnable {

    // Objects for the game loop/thread
    private Thread mThread = null;
    //backgrounds being loaded
    private int backgroundIndex = 0; // To keep track of the current background
    private Bitmap[] backgroundImages; // Array to hold background images
    //fade names
    private long namesDisplayStartTime = -1;
    private boolean isNamesFading = false;
    private static final long FADE_DURATION = 3000; // 3 seconds fade duration

    //speed logic
    private int applesEaten = 0;
    private long updateInterval = 100;

    // Control pausing between updates
    private long mNextFrameTime;
    // Is the game currently playing and or paused?
    private volatile boolean mPlaying = false;
    private volatile boolean mPaused = true;

    // for playing sound effects
    private SoundPool mSP;
    private int mEat_ID = -1;
    private int mCrash_ID = -1;
    private static final int MAX_STREAMS = 5;

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
    private Shark mShark;

    private Bitmap backgroundImage;

    private Point size;

    private Rect pauseButtonRect;
    private String pauseButtonText = "Pause";

    private boolean isGameStarted = false;
    private boolean speedBoost = false;
    private int speedBoostUpdatesRemaining = 0;
    private ArrayList<GameObject> gameObjects;

    private List<BadApple> badApples = new ArrayList<>();
    private Handler handler = new Handler();
    private Runnable spawnBadAppleRunnable = new Runnable() {
        @Override
        public void run() {
            spawnBadApple();
            handler.postDelayed(this, 10000 + new Random().nextInt(10000));
        }
    };

    public SnakeGame(Context context, Point size) {
        super(context);
        this.size = size;
        Typeface customTypeface = ResourcesCompat.getFont(context, R.font.workbench);
        initializeGameObjects(customTypeface);
        loadSounds(context);
        initializeGame(context,size);


    }

    private void initializeGame(Context context,Point size){
        int blockSize = size.x / NUM_BLOCKS_WIDE;
        mNumBlocksHigh = size.y / blockSize;

        // Load and scale all background images
        backgroundImages = new Bitmap[]{
                BitmapFactory.decodeResource(context.getResources(), R.drawable.snake1),
                //BitmapFactory.decodeResource(context.getResources(), R.drawable.snake2),
                // Add more backgrounds as needed
        };
        for (int i = 0; i < backgroundImages.length; i++) {
            backgroundImages[i] = Bitmap.createScaledBitmap(backgroundImages[i], size.x, size.y, false);
        }

        // Set the initial background
        backgroundImage = backgroundImages[backgroundIndex];
        mApple = new Apple(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize, location -> mSnake.isOccupied(location));
        mSnake = new Snake(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize);
        mShark = new Shark(getContext(), size.x / NUM_BLOCKS_WIDE, this::isOccupied, size.x, size.y);
    }
    private void loadSounds(Context context){
        // Initialize the SoundPool
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        mSP = new SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(audioAttributes)
                .build();
        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Prepare the sounds in memory
            descriptor = assetManager.openFd("get_apple.ogg");
            mEat_ID = mSP.load(descriptor, 0);

            descriptor = assetManager.openFd("snake_death.ogg");
            mCrash_ID = mSP.load(descriptor, 0);

        } catch (IOException e) {
            // Error
        }
    }

    //saves high score for the user
    private void saveHighScore(){
        SharedPreferences prefs = getContext().getSharedPreferences("SnakeGame", Context.MODE_PRIVATE);
        int highScore = prefs.getInt("highScore", 0);

        if(mScore > highScore) {
            //Save highScore
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("highScore", mScore);
            editor.apply();
        }
    }

    //gets stored high score value
    private int getHighScore(){
        SharedPreferences prefs = getContext().getSharedPreferences("SnakeGame", Context.MODE_PRIVATE);
        return prefs.getInt("highScore", 0);

    }

    private void initializeGameObjects(Typeface customTypeface) {
        //initialize drawing text
        mSurfaceHolder = getHolder();
        mPaint = new Paint();
        mPaint.setTextSize(40);
        mPaint.setColor(Color.WHITE);
        mPaint.setTypeface(customTypeface);
        //initialize objects
        gameObjects = new ArrayList<>();
        gameObjects.add(mSnake);
        gameObjects.add(mApple);
        gameObjects.add(mShark);
        handler.postDelayed(spawnBadAppleRunnable, 30000 + new Random().nextInt(15000)); // Initial delay
        //initialize text
        textPaint = new Paint();
        textPaint.setTextSize(40);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTypeface(customTypeface);
        //initialize button
        int buttonWidth = 200;
        int buttonHeight = 75;
        int buttonMargin = 50;
        int yOffset = 25;
        pauseButtonRect = new Rect(
                size.x - buttonWidth - buttonMargin,
                buttonMargin + yOffset,
                size.x - buttonMargin,
                buttonMargin + buttonHeight + yOffset
        );
    }

    private void spawnBadApple() {
        BadApple badApple = new BadApple(getContext(), new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), size.x / NUM_BLOCKS_WIDE, this::isOccupied);
        badApple.spawn();
        badApples.add(badApple);
        gameObjects.add(badApple);
    }


    @Override
    public void run() {
        while (mPlaying) {
            if (!mPaused && updateRequired()) {
                updateGameObjects();
                drawGameObjects();
            }
        }
    }

    private void updateGameObjects() {
        Iterator<GameObject> iterator = gameObjects.iterator();
        while (iterator.hasNext()) {
            GameObject obj = iterator.next();

            if (obj instanceof Snake) {
                Snake snake = (Snake) obj;
                snake.update(); // Update snake without speed logic

                if (snake.detectDeath()) {
                    gameOver();
                    return; // Exit method after calling gameOver()
                }
            } else {
                obj.update();
            }

            if (obj instanceof Apple && mSnake.checkDinner(((Apple) obj).getLocation())) {
                mApple.spawn();
                mScore += 1;
                mSP.play(mEat_ID, 1, 1, 0, 0, 1);
                applesEaten+=1;

                // Check if 10 apples have been eaten
                if (applesEaten % 10 == 0) {
                    changeBackground();
                }
                if (applesEaten % 5 == 0) {
                    increaseDifficulty();
                }
            }

            if (obj instanceof BadApple && mSnake.checkDinner(((BadApple) obj).getLocation())) {
                if (mSnake.segmentLocations.size() > 1) {
                    int segmentsToRemove = Math.min(4, mSnake.segmentLocations.size() - 1); // Calculate the maximum number of segments to remove
                    mScore = Math.max(0, mScore - 3); // Adjust score by removing 3 points
                    mSnake.reduceLength(segmentsToRemove); // Reduce snake length by the calculated amount
                    mSP.play(mCrash_ID, 1, 1, 0, 0, 1);
                    iterator.remove(); // Remove bad apple safely
                } else {
                    gameOver(); // End game if snake is too short to safely subtract segments
                    return; // Exit method after calling gameOver()
                }
            }
        }
    }

    // Method to change the background
    private void changeBackground() {
        // Increment the background index and loop back if necessary
        backgroundIndex = (backgroundIndex + 1) % backgroundImages.length;
        backgroundImage = backgroundImages[backgroundIndex];

        // Invalidate the view to force a redraw with the new background
        postInvalidate();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        // Make sure to draw the current background image
        canvas.drawBitmap(backgroundImage, 0, 0, null);
    }

    private void increaseDifficulty() {
        // Decrease the update interval to make the game update faster (snake moves faster)
        updateInterval = Math.max(updateInterval - 10, 20); // Decrease by 10ms, don't go below 20ms
    }

    private void gameOver() {
        mSP.play(mCrash_ID, 1, 1, 0, 0, 1);
        mPaused = true;
        isGameStarted = false;
        speedBoostUpdatesRemaining = 0;
        saveHighScore();
        Log.d("SnakeGame", "Game Over!");

        // Reset all instances of BadApple
        BadApple.resetAll(badApples, gameObjects);
    }

    // Check to see if it is time for an update
    public boolean updateRequired() {
        if (mNextFrameTime <= System.currentTimeMillis()) {
            // Use the variable update interval based on the current difficulty
            mNextFrameTime = System.currentTimeMillis() + updateInterval;
            return true;
        }
        return false;
    }


    // Update all the game objects
    private void drawGameObjects() {
        if (!mSurfaceHolder.getSurface().isValid()) {
            return;
        }

        mCanvas = mSurfaceHolder.lockCanvas();
        if (mCanvas == null) {
            return;
        }

        drawBackground();
        drawGrid();
        drawScore();
        drawGameObjectsOnCanvas();
        drawDeveloperNames(mCanvas);
        drawPausedMessage();
        drawTapToPlayMessage();
        drawPauseButton();
        drawShark(mCanvas);
        mSurfaceHolder.unlockCanvasAndPost(mCanvas);
    }

    private void drawBackground() {
        mCanvas.drawColor(Color.argb(255, 26, 128, 182));
        if (backgroundImage != null) {
            mCanvas.drawBitmap(backgroundImage, 0, 0, null);
        }
        mPaint.setColor(Color.argb(50, 0, 0, 0));
        mCanvas.drawRect(0, 0, mCanvas.getWidth(), mCanvas.getHeight(), mPaint);
    }

    private void drawShark(Canvas canvas) {
        if (mShark != null) {
            mShark.draw(canvas, mPaint);
        }
    }

    private void drawGrid() {
        int gridSize = 40;
        float colWidth = mCanvas.getWidth() / (float) gridSize;
        float rowHeight = mCanvas.getHeight() / (float) gridSize;
        mPaint.setColor(Color.argb(255, 255, 255, 255));
        mPaint.setStrokeWidth(1);

        for (int col = 0; col < gridSize; col++) {
            mCanvas.drawLine(col * colWidth, 0, col * colWidth, mCanvas.getHeight(), mPaint);
        }

        for (int row = 0; row < gridSize; row++) {
            mCanvas.drawLine(0, row * rowHeight, mCanvas.getWidth(), row * rowHeight, mPaint);
        }
    }

    private void drawScore() {
        int highScore=getHighScore();
        mPaint.setColor(Color.argb(255, 255, 255, 255));
        mPaint.setTextSize(120);
        mCanvas.drawText("High Score: " + highScore, 20, 120, mPaint);
        mCanvas.drawText("Score: " + mScore, 20, 260, mPaint);
    }

    private void drawGameObjectsOnCanvas() {
        for (GameObject obj : gameObjects) {
            obj.draw(mCanvas, mPaint);
        }
    }


    private void drawDeveloperNames(Canvas canvas) {
        if (namesDisplayStartTime == -1) {
            namesDisplayStartTime = System.currentTimeMillis();
            isNamesFading = true;
        }

        long elapsedTime = System.currentTimeMillis() - namesDisplayStartTime;
        if (elapsedTime > FADE_DURATION) {
            isNamesFading = false;
            return; // Stop drawing after the fade duration has passed
        }

        // Calculate the alpha based on the elapsed time
        int alpha = (int) (255 - (elapsedTime * 255 / FADE_DURATION));
        alpha = Math.max(alpha, 0); // Ensure alpha doesn't go below 0

        Paint textPaint = new Paint();
        textPaint.setTextSize(40);
        textPaint.setColor(Color.WHITE);
        textPaint.setAlpha(alpha); // Set the calculated alpha
        textPaint.setTextAlign(Paint.Align.RIGHT);

        // Define your names
        String[] names = {
                "Arjun Bhargava & Navid Baghaei",
                "Dagem Kebede & Rodrigo Guzman"
        };

        // Calculate where to start drawing the names
        int x = size.x - 20; // Right align the text
        int y = size.y - 80; // Starting position from the bottom

        // Loop through each name and draw it on a new line
        for (String name : names) {
            canvas.drawText(name, x, y, textPaint);
            y += textPaint.getTextSize(); // Move y position down for the next line
        }

        if (isNamesFading) {
            // Invalidate the view to trigger a redraw
            invalidate();
        }
    }

    private void drawPausedMessage() {
        if (mPaused && isGameStarted) {
            mPaint.setTextSize(250);
            mCanvas.drawText("Paused", size.x / 4f, size.y / 2f, mPaint);
        }
    }

    private void drawTapToPlayMessage() {
        if (!isGameStarted || (!mPaused && !isGameStarted)) {
            mPaint.setTextSize(250);
            mCanvas.drawText(getResources().getString(R.string.tap_to_play), size.x / 4f, size.y / 2f, mPaint);
        }
    }

    private void drawPauseButton() {
            Paint buttonPaint = new Paint();
            buttonPaint.setColor(Color.argb(128, 0, 0, 0));
            mCanvas.drawRect(pauseButtonRect, buttonPaint);

            buttonPaint.setColor(Color.WHITE);
            buttonPaint.setTextSize(35);
            float textWidth = buttonPaint.measureText(pauseButtonText);
            float x = pauseButtonRect.left + (pauseButtonRect.width() - textWidth) / 2;
            float y = pauseButtonRect.top + (pauseButtonRect.height() - buttonPaint.descent() - buttonPaint.ascent()) / 2;
            mCanvas.drawText(pauseButtonText, x, y, buttonPaint);
        }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();

            if (pauseButtonRect.contains(x, y) && isGameStarted) {
                togglePause();
                return true;
            } else if (!isGameStarted) {
                startNewGame();
                return true;
            } else if (!mPaused) {
                mSnake.switchHeading(motionEvent);
                return true;
            }
        }
        return super.onTouchEvent(motionEvent);
    }

    public void pause() {
        mPlaying = false;
        try {
            mThread.join();
        } catch (InterruptedException e) {
            // Handle error
        }
    }

    public void resume() {
        mPlaying = true;
        mThread = new Thread(this);
        mThread.start();
    }

    public Snake getSnake() {
        return mSnake;
    }

    private void togglePause() {
        mPaused = !mPaused;
        pauseButtonText = mPaused ? "Resume" : "Pause";
    }

    // Combined method for starting or restarting the game
    private void startNewGame() {
        // Pause the game if it's currently running
        if (mThread != null && mThread.isAlive()) {
            pause();
        }

        // Reset the update interval to the initial value
        updateInterval = 100; // Reset to 100ms
        applesEaten = 0; // Reset the apples eaten counter


        // Reset game state
        mSnake.reset(NUM_BLOCKS_WIDE, mNumBlocksHigh);
        mApple.spawn();
        mScore = 0;
        mNextFrameTime = System.currentTimeMillis();
        isGameStarted = true;
        mPaused = false;
        speedBoost = false;

        // Reset the background index for a new game
        backgroundIndex = 0;
        backgroundImage = backgroundImages[backgroundIndex];

        // Clear and reset all BadApples
        BadApple.resetAll(badApples, gameObjects); // Make sure this method is correctly removing BadApples from gameObjects

        // Ensure game objects are correctly initialized
        gameObjects.clear();  // Clear old game objects
        gameObjects.add(mSnake);
        gameObjects.add(mApple);
        gameObjects.add(mShark); // Assuming you want to keep the shark in the game

        // Re-initiate BadApple spawning mechanism if it's part of the game dynamics
        handler.removeCallbacks(spawnBadAppleRunnable);
        handler.postDelayed(spawnBadAppleRunnable, 30000 + new Random().nextInt(15000)); // Initial delay

        // Start the thread if not already running
        if (mThread == null || !mThread.isAlive()) {
            mPlaying = true;
            mThread = new Thread(this);
            mThread.start();
        } else {
            // Resume the game if it was paused
            resume();
        }
    }


    public boolean isOccupied(Point location) {
            return mSnake.isOccupied(location);
        }
    }