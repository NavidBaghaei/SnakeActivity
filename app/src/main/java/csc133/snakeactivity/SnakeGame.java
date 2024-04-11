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
import android.content.SharedPreferences;

class SnakeGame extends SurfaceView implements Runnable {



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

        loadSounds(context);
        initializeGame(context,size);
        initializeGameObjects(customTypeface);

    }

    private void initializeGame(Context context,Point size){
        int blockSize = size.x / NUM_BLOCKS_WIDE;
        mNumBlocksHigh = size.y / blockSize;

        backgroundImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.snake1);
        Log.d("SnakeGame", "Background image loaded: " + (backgroundImage != null));
        backgroundImage = Bitmap.createScaledBitmap(backgroundImage, size.x, size.y, false);

        mApple = new Apple(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize, location -> mSnake.isOccupied(location));
        mSnake = new Snake(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize);
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

    // Called to start a new game
    public void newGame() {
        mSnake.reset(NUM_BLOCKS_WIDE, mNumBlocksHigh);
        mApple.spawn();
        mScore = 0;
        mNextFrameTime = System.currentTimeMillis();
        isGameStarted = true;
        mPaused = false;
        speedBoost = false;
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
        for (GameObject obj : gameObjects) {
            if (obj instanceof Snake && speedBoostUpdatesRemaining > 0) {
                ((Snake) obj).move(true);
            } else {
                obj.update();
            }
        }

        if (speedBoostUpdatesRemaining > 0) {
            speedBoostUpdatesRemaining--;
        }

        if (mSnake.checkDinner(mApple.getLocation())) {
            mApple.spawn();
            mScore += 1;
            mSP.play(mEat_ID, 1, 1, 0, 0, 1);
            speedBoostUpdatesRemaining = 20;
        }

        if (mSnake.detectDeath()) {
            mSP.play(mCrash_ID, 1, 1, 0, 0, 1);
            mPaused = true;
            isGameStarted = false;
            speedBoostUpdatesRemaining = 0;
            saveHighScore();
        }

    }

    // Check to see if it is time for an update
    public boolean updateRequired() {

        final long TARGET_FPS = 10;
        final long MILLIS_PER_SECOND = 1000;

        if (mNextFrameTime <= System.currentTimeMillis()) {
            mNextFrameTime = System.currentTimeMillis() + MILLIS_PER_SECOND / TARGET_FPS;
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
        drawDeveloperNames();
        drawPausedMessage();
        drawTapToPlayMessage();
        drawPauseButton();

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

    private void drawDeveloperNames() {
        String names = "Arjun Bhargava & Navid Baghaei & Dagem Kebede & Rodrigo Guzman";
        int x = size.x - 20;
        int y = (int) (textPaint.getTextSize() + 20);
        mCanvas.drawText(names, x, y, textPaint);
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
        newGame();
        isGameStarted = true;
        mPaused = false;
        mPlaying = true;
        if (mThread == null || !mThread.isAlive()) {
            mThread = new Thread(this);
            mThread.start();
        }
    }
}