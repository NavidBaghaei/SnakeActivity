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
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import android.content.SharedPreferences;
import android.os.Handler;

class SnakeGame extends SurfaceView implements Runnable {
    private Thread mThread = null;
    private long mNextFrameTime;
    private volatile boolean mPlaying = false;
    private volatile boolean mPaused = true;
    private SoundPool mSP;
    private int mEat_ID = -1;
    private int mCrash_ID = -1;
    private static final int MAX_STREAMS = 5;
    private final int NUM_BLOCKS_WIDE = 40;
    private int mNumBlocksHigh;
    private int mScore;
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
    private List<BadApple> badApples; // Add this line
    private int speedBoostUpdatesRemaining = 0;
    private ArrayList<GameObject> gameObjects;
    private Handler handler = new Handler();
    private boolean isSpawnScheduled = false;

    private Runnable spawnBadAppleRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mPaused) {
                spawnBadApple();
                handler.postDelayed(this, 10000 + new Random().nextInt(10000));
                isSpawnScheduled = true; // Mark as scheduled
            } else {
                isSpawnScheduled = false; // Clear the flag if paused or finished
            }
        }
    };




    public SnakeGame(Context context, Point size) {
        super(context);
        this.size = size;
        Typeface customTypeface = ResourcesCompat.getFont(context, R.font.workbench);
        badApples = new ArrayList<>(); // Initialization of the badApples list
        initializeGameObjects(customTypeface);
        loadSounds(context);
        initializeGame(context,size);


    }

    private void initializeGame(Context context, Point size){
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
        // Assuming mSnake is already initialized and represents your Snake object
        mShark = new Shark(getContext(), size.x * 5 / NUM_BLOCKS_WIDE, size.x, size.y, mSnake);


    }

    private void loadSounds(Context context){
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
            descriptor = assetManager.openFd("get_apple.ogg");
            mEat_ID = mSP.load(descriptor, 0);
            descriptor = assetManager.openFd("snake_death.ogg");
            mCrash_ID = mSP.load(descriptor, 0);
        } catch (IOException e) {
            // Error handling
        }
    }

    private void saveHighScore(){
        SharedPreferences prefs = getContext().getSharedPreferences("SnakeGame", Context.MODE_PRIVATE);
        int highScore = prefs.getInt("highScore", 0);
        if(mScore > highScore) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("highScore", mScore);
            editor.apply();
        }
    }

    private int getHighScore(){
        SharedPreferences prefs = getContext().getSharedPreferences("SnakeGame", Context.MODE_PRIVATE);
        return prefs.getInt("highScore", 0);
    }

    private void initializeGameObjects(Typeface customTypeface) {
        mSurfaceHolder = getHolder();
        mPaint = new Paint();
        mPaint.setTextSize(40);
        mPaint.setColor(Color.WHITE);
        mPaint.setTypeface(customTypeface);
        gameObjects = new ArrayList<>();
        gameObjects.add(mSnake);
        gameObjects.add(mApple);
        gameObjects.add(mShark);
        handler.postDelayed(spawnBadAppleRunnable, 30000 + new Random().nextInt(15000));
        textPaint = new Paint();
        textPaint.setTextSize(40);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTypeface(customTypeface);
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
                obj.update(); // Update all game objects normally
            }


            // Check for collision with Shark
            if (obj instanceof Shark) {
                Shark shark = (Shark) obj;
                PointF sharkLocation = shark.getLocation();
                int sharkWidth = shark.getBitmap().getWidth(); // Safely retrieve the shark's bitmap width
                int sharkHeight = shark.getBitmap().getHeight(); // Safely retrieve the shark's bitmap height
                if (mSnake.checkCollisionWithHead(sharkLocation, sharkWidth, sharkHeight)) {
                    gameOver(); // Game over if the shark hits the snake's head
                    return;
                } else {
                    int segmentsRemoved = mSnake.removeCollidedSegments(sharkLocation);
                    if (segmentsRemoved > 0) {
                        mScore -= segmentsRemoved; // Subtract the number of segments removed from the score
                        mScore = Math.max(0, mScore); // Ensure the score does not go negative
                    }
                }
            }


            // Check for eating an Apple
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

            // Check for BadApple effects
            if (obj instanceof BadApple && mSnake.checkDinner(((BadApple) obj).getLocation())) {
                int segmentsToRemove = Math.min(4, mSnake.segmentLocations.size() - 1);
                mSnake.reduceLength(segmentsToRemove);
                mSP.play(mCrash_ID, 1, 1, 0, 0, 1);
                iterator.remove();
                mScore -= 3; // Subtract the penalty for eating a bad apple
                mScore = Math.max(0, mScore); // Ensure the score does not go negative
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
        BadApple.resetAll(badApples, gameObjects);
    }

    public boolean updateRequired() {
        if (mNextFrameTime <= System.currentTimeMillis()) {
            // Use the variable update interval based on the current difficulty
            mNextFrameTime = System.currentTimeMillis() + updateInterval;
            return true;
        }
        return false;
    }

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
        int highScore = getHighScore();
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
        String[] names = {"Arjun Bhargava & Navid Baghaei", "Dagem Kebede & Rodrigo Guzman"};
        int x = size.x - 20;
        int y = size.y - 80;
        for (String name : names) {
            canvas.drawText(name, x, y, textPaint);
            y += textPaint.getTextSize();
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

    private void startNewGame() {
        // Ensure any existing game thread is properly stopped before starting a new one.
        if (mThread != null && mThread.isAlive()) {
            pause(); // Pause the game which also stops the thread and clears bad apple callbacks.
        }

        // Reset the update interval to the initial value
        updateInterval = 100; // Reset to 1 second
        applesEaten = 0; // Reset the apples eaten counter


        // Reset game state
        mSnake.reset(NUM_BLOCKS_WIDE, mNumBlocksHigh);
        mApple.spawn();
        mShark.reset();
        gameObjects.add(mSnake);
        gameObjects.add(mApple);
        gameObjects.add(mShark);

        // Reset game state variables
        mScore = 0;
        mNextFrameTime = System.currentTimeMillis();
        isGameStarted = true;
        mPaused = false;
        speedBoost = false;
        mPlaying = true;

        // Reset the background index for a new game
        backgroundIndex = 0;
        backgroundImage = backgroundImages[backgroundIndex];

        // Clear and reset all BadApples
        BadApple.resetAll(badApples, gameObjects); // Make sure this method is correctly removing BadApples from gameObjects

        // Start or restart the game thread
        if (mThread == null || !mThread.isAlive()) {
            mThread = new Thread(this);
            mThread.start();
        }

        // Schedule the first Bad Apple spawn only if the game is not paused
        scheduleNextBadAppleSpawn();
    }

    private void scheduleNextBadAppleSpawn() {
        if (!mPaused && mPlaying && !isSpawnScheduled) {
            handler.postDelayed(spawnBadAppleRunnable, 30000 + new Random().nextInt(15000));
            isSpawnScheduled = true; // Set when scheduling
        }
    }



    public boolean isOccupied(Point location) {
        return mSnake.isOccupied(location);
    }


}
