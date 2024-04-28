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
    private Bitmap[] backgroundImages;
    private int backgroundIndex = 0;
    private int applesEaten = 0;
    private long updateInterval = 100;
    private long namesDisplayStartTime = -1;
    private static final long FADE_DURATION = 10000;
    private boolean isNamesFading = false;
    private int regularApplesEaten = 0;
    private final int SPEED_BOOST_THRESHOLD = 5;





    // Method to change background
    private void changeBackground() {
        backgroundIndex = (backgroundIndex + 1) % backgroundImages.length;
        backgroundImage = backgroundImages[backgroundIndex];
        postInvalidate();
    }

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


    private void initializeBackgrounds(Context context, Point size) {
        backgroundImages = new Bitmap[]{
                BitmapFactory.decodeResource(context.getResources(), R.drawable.snake1),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.test1) // Assuming you have a second background
                // Add additional backgrounds here
        };

        for (int i = 0; i < backgroundImages.length; i++) {
            backgroundImages[i] = Bitmap.createScaledBitmap(backgroundImages[i], size.x, size.y, false);
        }
    }


    public SnakeGame(Context context, Point size) {
        super(context);
        this.size = size;
        Typeface customTypeface = ResourcesCompat.getFont(context, R.font.workbench);
        loadSounds(context);
        initializeGame(context, size);
        badApples = new ArrayList<>(); // Initialization of the badApples list
        initializeGameObjects(customTypeface);
    }

    private void initializeGame(Context context, Point size){
        int blockSize = size.x / NUM_BLOCKS_WIDE;
        mNumBlocksHigh = size.y / blockSize;

        // Initialize backgrounds
        initializeBackgrounds(context, size); // This method will handle loading and scaling of background images
        backgroundImage = backgroundImages[0]; // Set the initial background from the array

        // Initialize game objects
        mApple = new Apple(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize, location -> mSnake.isOccupied(location));
        mSnake = new Snake(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize);
        mShark = new Shark(getContext(), size.x * 5 / NUM_BLOCKS_WIDE, size.x, size.y, mSnake);
    }


    private void loadSounds(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME) // Adjusted for game-specific sound usage
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        mSP = new SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(audioAttributes)
                .build();

        AssetManager assetManager = context.getAssets();
        try {
            AssetFileDescriptor descriptor = assetManager.openFd("get_apple.ogg");
            mEat_ID = mSP.load(descriptor, 1); // Priority set to 1 for game sounds
            descriptor = assetManager.openFd("snake_death.ogg");
            mCrash_ID = mSP.load(descriptor, 1);
        } catch (IOException e) {
            Log.e("SnakeGame", "Error loading sound effects", e);
        }
    }


    private void saveHighScore() {
        SharedPreferences prefs = getContext().getSharedPreferences("SnakeGame", Context.MODE_PRIVATE);
        int highScore = prefs.getInt("highScore", 0);

        if (mScore > highScore) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("highScore", mScore);
            editor.apply();  // Use apply() instead of commit() for asynchronous commit to disk
        }
    }

    private int getHighScore() {
        SharedPreferences prefs = getContext().getSharedPreferences("SnakeGame", Context.MODE_PRIVATE);
        return prefs.getInt("highScore", 0);  // Returns 0 if "highScore" does not exist
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

            obj.update();  // Update all game objects

            if (obj instanceof Snake) {
                if (mSnake.detectDeath()) {
                    gameOver();
                    return;
                }
            }

            if (obj instanceof Apple && mSnake.checkDinner(((Apple) obj).getLocation())) {
                mApple.spawn();
                mScore += 1;
                mSP.play(mEat_ID, 1, 1, 0, 0, 1);



                // Apply speed boost every time the score increases by 5
                if (mScore % 5 == 0) {
                    increaseDifficulty();
                }
                // Apply speed boost every time the score increases by 5
                if (mScore % 10 == 0) {
                    changeBackground();
                }


            }

            if (obj instanceof Shark) {
                Shark shark = (Shark) obj;
                if (mSnake.checkCollisionWithHead(shark.getLocation(), shark.getBitmap().getWidth(), shark.getBitmap().getHeight())) {
                    gameOver();
                    return;
                } else {
                    int segmentsRemoved = mSnake.removeCollidedSegments(shark.getLocation());
                    if (segmentsRemoved > 0) {
                        mScore -= segmentsRemoved;
                        mScore = Math.max(0, mScore);
                    }
                }
            }

            if (obj instanceof BadApple && mSnake.checkDinner(((BadApple) obj).getLocation())) {
                int segmentsToRemove = Math.min(4, mSnake.segmentLocations.size() - 1);
                mSnake.reduceLength(segmentsToRemove);
                mSP.play(mCrash_ID, 1, 1, 0, 0, 1);
                iterator.remove();
                mScore -= 3;
                mScore = Math.max(0, mScore);
            }
        }

    }

    private void increaseDifficulty() {
        // Decrease update interval based on the score proportionally
        int speedIncreaseFactor = mScore / 5;  // Increase factor for every 5 apples eaten
        updateInterval = Math.max(100 - 10 * speedIncreaseFactor, 20);  // Cap minimum interval to 20ms

        // Provide feedback that speed has increased
        mCanvas.drawText("Speed boost!", size.x / 2, size.y / 2, mPaint);  // Visual feedback
        mSP.play(mEat_ID, 1, 1, 1, 0, 1.5f);  // Auditory feedback with different sound settings
    }

    public boolean updateRequired() {
        final long currentTime = System.currentTimeMillis();
        if (mNextFrameTime <= currentTime) {
            mNextFrameTime = currentTime + updateInterval;
            return true;
        }
        return false;
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

    private void drawGameObjects() {
        if (!mSurfaceHolder.getSurface().isValid()) {
            return;
        }
        mCanvas = mSurfaceHolder.lockCanvas();
        if (mCanvas == null) {
            return;
        }
        drawBackground();
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
        // Check if the fade has started, if not, initialize it
        if (namesDisplayStartTime == -1) {
            namesDisplayStartTime = System.currentTimeMillis();
            isNamesFading = true;
        }

        // Calculate the elapsed time
        long elapsedTime = System.currentTimeMillis() - namesDisplayStartTime;
        if (elapsedTime > FADE_DURATION) {
            isNamesFading = false; // Stop fading after the duration
        } else {
            // Calculate the alpha based on the elapsed time
            int alpha = (int) (255 - (255 * elapsedTime / FADE_DURATION));
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

            // Ensure the canvas redraws to continue the fade
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
        mPaused = true;
        handler.removeCallbacks(spawnBadAppleRunnable);
        isSpawnScheduled = false; // Reset the flag when game is paused
    }


    public void resume() {
        mPaused = false;
        if (!mPlaying) {
            mPlaying = true;
            mThread = new Thread(this);
            mThread.start();
            if (!isSpawnScheduled) { // Check flag before scheduling
                scheduleNextBadAppleSpawn();
            }
        }
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

        // Clear the game objects list and reinitialize game objects
        gameObjects.clear();
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
        regularApplesEaten = 0;  // Reset the count of regular apples eaten
        updateInterval = 100;    // Reset the update interval to default

        // Reset and clear any lingering BadApples and prepare the game objects list
        BadApple.resetAll(badApples, gameObjects);

        // Reset the background index for a new game
        backgroundIndex = 0;
        backgroundImage = backgroundImages[backgroundIndex];

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