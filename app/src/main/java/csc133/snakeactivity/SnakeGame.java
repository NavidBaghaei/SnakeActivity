package csc133.snakeactivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import android.content.SharedPreferences;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

@SuppressLint("ViewConstructor")
class SnakeGame extends SurfaceView implements Runnable {
    private Thread mThread = null;
    private long mNextFrameTime;
    private volatile boolean mPlaying = false;
    private volatile boolean mPaused = true;
    private final int NUM_BLOCKS_WIDE = 40;
    private int mNumBlocksHigh;
    private int mScore;
    private Canvas mCanvas;
    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;
    private ISnake mSnake;
    private Apple mApple;
    private Shark mShark;
    private Bitmap backgroundImage;
    private Point size;
    private Rect pauseButtonRect;
    private String pauseButtonText = "Pause";
    private boolean isGameStarted = false;
    private List<BadApple> badApples;
    private ArrayList<GameObject> gameObjects;
    private final Handler handler = new Handler();
    private boolean isSpawnScheduled = false;
    private Bitmap[] backgroundImages;
    private long updateInterval = 100;
    private long namesDisplayStartTime = -1;
    private static final long FADE_DURATION = 10000;
    private SuperApple mSuperApple;
    private boolean isPowerUpActive = false;
    private long powerUpEndTime;

    private final Runnable spawnBadAppleRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mPaused) {
                spawnBadApple();
                handler.postDelayed(this, 10000 + new Random().nextInt(5000));
                isSpawnScheduled = true;
            } else {
                isSpawnScheduled = false;
            }
        }
    };

    private final Runnable spawnSuperAppleRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mPaused && !gameObjects.contains(mSuperApple)) {
                mSuperApple.spawn();
                gameObjects.add(mSuperApple);
            }
            if (!mPaused) {
                handler.postDelayed(this, mSuperApple.getRespawnDelay());
            }
        }
    };

    private void scheduleSuperAppleRespawn() {
        handler.postDelayed(() -> {
            if (mPlaying && !mPaused) {
                mSuperApple.spawn();
                if (!gameObjects.contains(mSuperApple)) {
                    gameObjects.add(mSuperApple);
                }
            }
        }, mSuperApple.getRespawnDelay());
    }

    private void initializeBackgrounds(Context context, Point size) {
        backgroundImages = new Bitmap[]{
                BitmapFactory.decodeResource(context.getResources(), R.drawable.snake1),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.stage2)
        };

        for (int i = 0; i < backgroundImages.length; i++) {
            backgroundImages[i] = Bitmap.createScaledBitmap(backgroundImages[i], size.x, size.y, false);
        }
    }

    private boolean shouldDrawSpeedBoost = false;
    private long speedBoostDisplayStartTime;
    private static final long SPEED_BOOST_DISPLAY_DURATION = 5000;

    private AudioContext audioContext;

    public SnakeGame(Context context, Point size) {
        super(context);
        this.size = size;
        Typeface customTypeface = ResourcesCompat.getFont(context, R.font.workbench);
        initializeGame(context, size);
        this.audioContext = new AudioContext(new ExtendedAudioStrategy());
        audioContext.loadSounds(context);
        if (!audioContext.isMusicPlaying()) {
            audioContext.startBackgroundMusic();
            setBackgroundMusicVolume();
        }
        badApples = new ArrayList<>();
        initializeGameObjects(customTypeface);
    }

    private void initializeGame(Context context, Point size){
        int blockSize = size.x / NUM_BLOCKS_WIDE;
        mNumBlocksHigh = size.y / blockSize;
        initializeBackgrounds(context, size);
        backgroundImage = backgroundImages[0];

        mApple = new Apple(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize, location -> mSnake.isOccupied(location));
        mSnake = new SnakeDecorator(new Snake(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize));
        mShark = new Shark(getContext(), size.x * 5 / NUM_BLOCKS_WIDE, size.x, size.y, mSnake);
        mSuperApple = new SuperApple(getContext(), new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize, this::isOccupied);
    }

    private void saveHighScore() {
        SharedPreferences prefs = getContext().getSharedPreferences("SnakeGame", Context.MODE_PRIVATE);
        int highScore = prefs.getInt("highScore", 0);

        if (mScore > highScore) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("highScore", mScore);
            editor.apply();
        }
    }

    private int getHighScore() {
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
        gameObjects.add(mSuperApple);
        scheduleSuperAppleRespawn();
        handler.postDelayed(spawnBadAppleRunnable, 30000 + new Random().nextInt(15000));
        Paint textPaint = new Paint();
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

    public void run() {
        long lastSuperAppleSpawnTime = System.currentTimeMillis();

        while (mPlaying) {
            if (!mPaused) {
                long currentTime = System.currentTimeMillis();
                if (updateRequired()) {
                    updateGameObjects();
                    drawGameObjects();
                    resumeMusic();
                }

                if (currentTime - lastSuperAppleSpawnTime >= 15000) {
                    if (mSuperApple != null) {
                        mSuperApple.spawn();
                        lastSuperAppleSpawnTime = currentTime;
                    }
                }
            } else {
                pauseMusic();
                stopSounds();
            }

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void updateGameObjects() {
        Iterator<GameObject> iterator = gameObjects.iterator();
        while (iterator.hasNext()) {
            GameObject obj = iterator.next();
            obj.update();

            if (obj instanceof ISnake) {
                ISnake snake = (ISnake) obj;
                if (snake.detectDeath()) {
                    gameOver();
                    return;
                }
            }

            if (obj instanceof Apple && mSnake.checkDinner(((Apple) obj).getLocation())) {
                mApple.spawn();
                mScore += 1;
                audioContext.playEatSound();
                if (mScore % 5 == 0) {
                    increaseDifficulty();
                }
                /*if (mScore % 10 == 0) {
                    changeBackground();
                }*/
            }

            if (obj instanceof Shark) {
                Shark shark = (Shark) obj;
                if (mSnake.checkCollisionWithHead(shark.getLocation(), shark.getBitmap().getWidth(), shark.getBitmap().getHeight())) {
                    if (isPowerUpActive) {
                        audioContext.playSharkDeathSound();
                        mShark.reset();
                    } else {
                        int segmentsRemoved = mSnake.removeCollidedSegments(shark.getLocation());
                        if (segmentsRemoved > 0) {
                            mScore -= segmentsRemoved;
                            mScore = Math.max(0, mScore);
                        }
                        gameOver();
                        return;
                    }
                }
            }

            if (obj instanceof BadApple && mSnake.checkDinner(((BadApple) obj).getLocation())) {
                if (isPowerUpActive) {
                    mScore += 1;
                    audioContext.playEatSound();
                } else {
                    int segmentsToRemove = Math.min(4, mSnake.getSegmentCount() - 1);
                    mSnake.reduceLength(segmentsToRemove);
                    mScore -= 3;
                    mScore = Math.max(0, mScore);
                    audioContext.playBadEatSound();
                }
                iterator.remove();
            }

            if (obj instanceof SuperApple && mSnake.checkSuperAppleDinner(((SuperApple) obj).getLocation())) {
                ((SuperApple) obj).markAsEaten();
                audioContext.playEatSound();
                iterator.remove();
                isPowerUpActive = true;
                audioContext.playPowerUpMusic();
                powerUpEndTime = System.currentTimeMillis() + 14000;
                scheduleSuperAppleRespawn();
                activatePowerUp();
                powerUpEndTime = System.currentTimeMillis() + 10000;
                scheduleSuperAppleRespawn();
            }

            if (isPowerUpActive && System.currentTimeMillis() > powerUpEndTime) {
                isPowerUpActive = false;
                audioContext.stopPowerUpMusic();
            }

            deactivatePowerUp();
        }
    }

    private void activatePowerUp() {
        if (mSnake instanceof SnakeDecorator) {
            ((SnakeDecorator) mSnake).setPowerUpActive(true);
        }
    }

    private void deactivatePowerUp() {
        if (mSnake instanceof SnakeDecorator) {
            ((SnakeDecorator) mSnake).setPowerUpActive(false);
        }
    }

    private void increaseDifficulty() {
        int speedIncreaseFactor = mScore / 5;
        updateInterval = Math.max(100 - 10 * speedIncreaseFactor, 20);
        shouldDrawSpeedBoost = true;
        audioContext.playSpeedBoostSound();
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
        audioContext.playCrashSound();
        audioContext.stopBackgroundMusic();
        mPaused = true;
        isGameStarted = false;
        stopSounds();
        saveHighScore();
        SuperApple.gameOver();
        BadApple.resetAll(badApples, gameObjects);
        showRestartDialog();
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

        if (shouldDrawSpeedBoost) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - speedBoostDisplayStartTime < SPEED_BOOST_DISPLAY_DURATION) {
                mPaint.setTextSize(40);
                mPaint.setColor(Color.RED);
                float x = (float) size.x / 2 - mPaint.measureText("Speed boost!") / 2;
                float y = (float) size.y / 2;
                mCanvas.drawText("Speed boost!", x, y, mPaint);
            } else {
                shouldDrawSpeedBoost = false;
            }
            postInvalidate();
        }
    }

    private void drawDeveloperNames(Canvas canvas) {
        if (namesDisplayStartTime == -1) {
            namesDisplayStartTime = System.currentTimeMillis();
        }

        long elapsedTime = System.currentTimeMillis() - namesDisplayStartTime;
        if (elapsedTime <= FADE_DURATION) {
            int alpha = (int) (255 - (255 * elapsedTime / FADE_DURATION));
            alpha = Math.max(alpha, 0);

            Paint textPaint = new Paint();
            textPaint.setTextSize(40);
            textPaint.setColor(Color.WHITE);
            textPaint.setAlpha(alpha);
            textPaint.setTextAlign(Paint.Align.RIGHT);

            String[] names = {"Arjun Bhargava & Navid Baghaei", "Dagem Kebede & Rodrigo Guzman"};
            int x = size.x - 20;
            int y = size.y - 80;
            for (String name : names) {
                canvas.drawText(name, x, y, textPaint);
                y += (int) textPaint.getTextSize();
            }

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
        if (!isGameStarted) {
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

    @SuppressLint("ClickableViewAccessibility")
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
        handler.removeCallbacks(spawnSuperAppleRunnable);
        isSpawnScheduled = false;
    }

    public void resume() {
        mPaused = false;
        if (!mPlaying) {
            mPlaying = true;
            mThread = new Thread(this);
            mThread.start();
            if (!isSpawnScheduled) {
                scheduleNextBadAppleSpawn();
                scheduleSuperAppleRespawn();
            }
        }
    }

    public ISnake getSnake() {
        return mSnake;
    }

    private void togglePause() {
        mPaused = !mPaused;
        pauseButtonText = mPaused ? "Resume" : "Pause";
    }

    protected void pauseMusic() {
        audioContext.stopBackgroundMusic();
    }

    protected void resumeMusic() {
        audioContext.startBackgroundMusic();
    }

    private void startNewGame() {
        if (mThread != null && mThread.isAlive()) {
            pause();
        }

        gameObjects.clear();
        mSnake.reset(NUM_BLOCKS_WIDE, mNumBlocksHigh);
        mApple.spawn();
        mShark.reset();
        gameObjects.add(mSnake);
        gameObjects.add(mApple);
        gameObjects.add(mShark);
        mSuperApple.spawn();
        gameObjects.add(mSuperApple);
        scheduleSuperAppleRespawn();
        mScore = 0;
        mNextFrameTime = System.currentTimeMillis();
        isGameStarted = true;
        mPaused = false;
        //boolean speedBoost = false;
        mPlaying = true;
        //int regularApplesEaten = 0;
        updateInterval = 100;
        SuperApple.gameOver();
        BadApple.resetAll(badApples, gameObjects);
        int backgroundIndex = 0;
        backgroundImage = backgroundImages[backgroundIndex];

        if (mThread == null || !mThread.isAlive()) {
            mThread = new Thread(this);
            mThread.start();
        }

        scheduleNextBadAppleSpawn();

        if (!audioContext.isMusicPlaying()) {
            audioContext.startBackgroundMusic();
        }
    }

    private void showRestartDialog() {
        final Context activityContext = getContext();
        if (activityContext instanceof Activity) {
            ((Activity) activityContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activityContext);
                    builder.setTitle("Game Over");
                    builder.setMessage("Do you want to restart the game?");
                    builder.setPositiveButton("Restart", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startNewGame(); // Implement startNewGame() method to reset the game state
                        }
                    });
                    builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Exit the game
                            if (activityContext instanceof Activity) {
                                ((Activity) activityContext).finish();
                            }
                        }
                    });
                    builder.setCancelable(false); // Prevent dismissing dialog by clicking outside
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        }
    }


    private void scheduleNextBadAppleSpawn() {
        if (!mPaused && mPlaying && !isSpawnScheduled) {
            handler.postDelayed(spawnBadAppleRunnable, 30000 + new Random().nextInt(15000));
            isSpawnScheduled = true;
        }
    }

    public boolean isOccupied(Point location) {
        return mSnake.isOccupied(location);
    }

    private void stopSounds() {
        AudioContext.stopSharkSwimSound();
        audioContext.stopPowerUpMusic();
    }

    private void setBackgroundMusicVolume() {
        audioContext.setMusicVolume(0.4F);
    }
}