package csc133.snakeactivity;
import csc133.snakeactivity.PowerUpSnakeDecorator;

import android.annotation.SuppressLint;
import android.content.*;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.core.content.res.ResourcesCompat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import android.graphics.drawable.Drawable;

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
    private boolean shouldDrawSpeedBoost = false;
    private long speedBoostDisplayStartTime;
    private static final long SPEED_BOOST_DISPLAY_DURATION = 5000;
    private static final long POWER_UP_DURATION = 14000; // Power-up duration in milliseconds
    private volatile boolean isGameOver = false; // Add a flag to track game over state

    private AudioContext audioContext;

    // Runnable for spawning BadApples
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

    // Runnable for spawning SuperApples
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

    // Method to schedule respawn of SuperApples
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

    // Method to initialize background images
    private void initializeBackgrounds(Context context, Point size) {
        backgroundImages = new Bitmap[]{
                BitmapFactory.decodeResource(context.getResources(), R.drawable.snake1),
                BitmapFactory.decodeResource(context.getResources(), R.drawable.stage2)
        };

        for (int i = 0; i < backgroundImages.length; i++) {
            backgroundImages[i] = Bitmap.createScaledBitmap(backgroundImages[i], size.x, size.y, false);
        }
    }

    // Constructor
    public SnakeGame(Context context, Point size) {
        super(context);
        this.size = size;
        Typeface customTypeface = ResourcesCompat.getFont(context, R.font.workbench);
        initializeGame(context, size);
        // Create AudioContext instance
        this.audioContext = new AudioContext(new ExtendedAudioStrategy());
        audioContext.loadSounds(context);
        // Start background music if not playing
        if (!audioContext.isMusicPlaying()) {
            audioContext.startBackgroundMusic();
            setBackgroundMusicVolume();
        }
        badApples = new ArrayList<>();
        initializeGameObjects(customTypeface);
    }

    private void initializeGame(Context context, Point size) {
        int blockSize = size.x / NUM_BLOCKS_WIDE;
        mNumBlocksHigh = size.y / blockSize;
        initializeBackgrounds(context, size);
        backgroundImage = backgroundImages[0];

        // Create the snake object
        mSnake = new Snake(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize);

        // Load the drawable resources for the snake head and body
        @SuppressLint("UseCompatLoadingForDrawables")
        Drawable snakeHeadDrawable = context.getResources().getDrawable(R.drawable.head);
        @SuppressLint("UseCompatLoadingForDrawables")
        Drawable snakeBodyDrawable = context.getResources().getDrawable(R.drawable.body);

        // Ensure that the drawables are mutable
        snakeHeadDrawable = snakeHeadDrawable.mutate();
        snakeBodyDrawable = snakeBodyDrawable.mutate();

        // Decorate the snake object with PowerUpSnakeDecorator
        mSnake = new PowerUpSnakeDecorator(mSnake, snakeHeadDrawable, snakeBodyDrawable, blockSize);

        // Initialize other game objects
        mApple = new Apple(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize, location -> mSnake.isOccupied(location));
        mShark = new Shark(getContext(), size.x * 5 / NUM_BLOCKS_WIDE, size.x, size.y, mSnake);
        mSuperApple = new SuperApple(getContext(), new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize, this::isOccupied);
    }

    // Method to save high score
    private void saveHighScore() {
        SharedPreferences prefs = getContext().getSharedPreferences("SnakeGame", Context.MODE_PRIVATE);
        int highScore = prefs.getInt("highScore", 0);

        if (mScore > highScore) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("highScore", mScore);
            editor.apply();
        }
    }

    // Method to get high score
    private int getHighScore() {
        SharedPreferences prefs = getContext().getSharedPreferences("SnakeGame", Context.MODE_PRIVATE);
        return prefs.getInt("highScore", 0);
    }

    // Method to initialize game objects and UI elements
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

    // Method to spawn BadApples
    private void spawnBadApple() {
        BadApple badApple = new BadApple(getContext(), new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), size.x / NUM_BLOCKS_WIDE, this::isOccupied);
        badApple.spawn();
        badApples.add(badApple);
        gameObjects.add(badApple);
    }

    // Method to run the game loop
    public void run() {
        long lastSuperAppleSpawnTime = System.currentTimeMillis();

        while (mPlaying) {
            long currentTime = System.currentTimeMillis();

            if (!mPaused) {
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

    // Method to update game objects
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

            // Check collisions and update score
            if (obj instanceof Apple && mSnake.checkDinner(((Apple) obj).getLocation())) {
                mApple.spawn();
                mScore += 1;
                audioContext.playEatSound();
                if (mScore % 5 == 0) {
                    increaseDifficulty();
                }
            }

            // Interaction with Shark
            if (obj instanceof Shark) {
                Shark shark = (Shark) obj;
                PointF sharkLocation = new PointF(shark.getLocation().x, shark.getLocation().y);
                int sharkWidth = shark.getBitmap().getWidth();
                int sharkHeight = shark.getBitmap().getHeight();

                if (mSnake.checkCollisionWithHead(sharkLocation, sharkWidth, sharkHeight)) {
                    if (isPowerUpActive) {
                        // Power-up active: defeat shark but do not end game
                        audioContext.playSharkDeathSound();
                        mShark.reset();
                    } else {
                        // No power-up: game over on head collision
                        gameOver();
                        return;
                    }
                } else {
                    // Check for body collisions if no head collision
                    if (!isPowerUpActive) {  // Only remove segments if the power-up is not active
                        int segmentsRemoved = mSnake.removeCollidedSegments(sharkLocation, sharkWidth, sharkHeight);
                        if (segmentsRemoved > 0) {
                            mScore -= segmentsRemoved;
                            mScore = Math.max(0, mScore);
                        }
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
                isPowerUpActive = true;
                audioContext.playPowerUpMusic();
                long duration = POWER_UP_DURATION; // Duration of the power-up
                powerUpEndTime = System.currentTimeMillis() + duration;
                scheduleSuperAppleRespawn();
                activatePowerUp(duration); // Call method to activate power-up with duration
                // Remove the power-up object after the iteration is complete
                iterator.remove();
            }
        }

        // Check if the power-up duration has expired and deactivate if necessary
        if (isPowerUpActive && System.currentTimeMillis() > powerUpEndTime) {
            isPowerUpActive = false;
            audioContext.stopPowerUpMusic();
            deactivatePowerUp(); // Call method to deactivate power-up
        }
    }

    // Method to activate power-up
    private void activatePowerUp(long duration) {
        if (mSnake instanceof PowerUpSnakeDecorator) {
            ((PowerUpSnakeDecorator) mSnake).setPowerUpActive(true, duration);
        }
        isPowerUpActive = true;
        audioContext.playPowerUpMusic();
    }

    // Method to deactivate power-up
    private void deactivatePowerUp() {
        if (mSnake instanceof PowerUpSnakeDecorator) {
            ((PowerUpSnakeDecorator) mSnake).setPowerUpActive(false, 0L); // Set duration to 0 for immediate deactivation
        }
        isPowerUpActive = false;
        audioContext.stopPowerUpMusic(); // Stop the power-up music
    }

    // Method to increase game difficulty
    private void increaseDifficulty() {
        int speedIncreaseFactor = mScore / 5;
        updateInterval = Math.max(100 - 10 * speedIncreaseFactor, 20);
        shouldDrawSpeedBoost = true;
        audioContext.playSpeedBoostSound();
    }

    // Method to check if update is required
    public boolean updateRequired() {
        final long currentTime = System.currentTimeMillis();
        if (mNextFrameTime <= currentTime) {
            mNextFrameTime = currentTime + updateInterval;
            return true;
        }
        return false;
    }

    // Method to handle game over
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

    // Method to draw game objects on canvas
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

    // Method to draw background
    private void drawBackground() {
        mCanvas.drawColor(Color.argb(255, 26, 128, 182));
        if (backgroundImage != null) {
            mCanvas.drawBitmap(backgroundImage, 0, 0, null);
        }
        mPaint.setColor(Color.argb(50, 0, 0, 0));
        mCanvas.drawRect(0, 0, mCanvas.getWidth(), mCanvas.getHeight(), mPaint);
    }

    // Method to draw shark on canvas
    private void drawShark(Canvas canvas) {
        if (mShark != null) {
            mShark.draw(canvas, mPaint);
        }
    }

    // Method to draw score
    private void drawScore() {
        int highScore = getHighScore();
        mPaint.setColor(Color.argb(255, 255, 255, 255));
        mPaint.setTextSize(120);
        mCanvas.drawText("High Score: " + highScore, 20, 120, mPaint);
        mCanvas.drawText("Score: " + mScore, 20, 260, mPaint);
    }

    // Method to draw game objects on canvas
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

    // Method to draw developer names
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

    // Method to draw paused message
    private void drawPausedMessage() {
        if (mPaused && isGameStarted) {
            mPaint.setTextSize(250);
            mCanvas.drawText("Paused", size.x / 4f, size.y / 2f, mPaint);
        }
    }

    // Method to draw tap to play message
    private void drawTapToPlayMessage() {
        if (!isGameStarted) {
            mPaint.setTextSize(250);
            mCanvas.drawText(getResources().getString(R.string.tap_to_play), size.x / 4f, size.y / 2f, mPaint);
        }
    }

    // Method to draw pause button
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

    // Method to handle touch events
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

    // Method to pause the game
    public void pause() {
        mPaused = true;
        handler.removeCallbacks(spawnBadAppleRunnable);
        handler.removeCallbacks(spawnSuperAppleRunnable);
        isSpawnScheduled = false;
    }

    // Method to resume the game
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

    // Method to get the snake
    public ISnake getSnake() {
        return mSnake;
    }

    // Method to toggle pause state
    private void togglePause() {
        mPaused = !mPaused;
        pauseButtonText = mPaused ? "Resume" : "Pause";
    }

    // Method to pause music
    protected void pauseMusic() {
        audioContext.stopBackgroundMusic();
    }

    // Method to resume music
    protected void resumeMusic() {
        audioContext.startBackgroundMusic();
    }

    // Method to start a new game
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
        mPlaying = true;
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
                    // Inflate custom layout for dialog
                    LayoutInflater inflater = LayoutInflater.from(activityContext);
                    View dialogView = inflater.inflate(R.layout.custom_dialog_layout, null);

                    // Find views in custom layout
                    ImageView backgroundImageView = dialogView.findViewById(R.id.background_image_view);
                    Button restartButton = dialogView.findViewById(R.id.restart_button);
                    Button exitButton = dialogView.findViewById(R.id.exit_button);

                    // Set image on the background image view
                    backgroundImageView.setImageResource(R.drawable.game_over);

                    AlertDialog.Builder builder = new AlertDialog.Builder(activityContext);
                    builder.setView(dialogView);
                    builder.setCancelable(false); // Prevent dismissing dialog by clicking outside
                    AlertDialog dialog = builder.create();

                    // Set click listeners for buttons
                    restartButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss(); // Dismiss dialog
                            startNewGame(); // Implement startNewGame() method to reset the game state
                        }
                    });

                    exitButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss(); // Dismiss dialog
                            if (activityContext instanceof Activity) {
                                ((Activity) activityContext).finish(); // Exit the game
                            }
                        }
                    });

                    // Ensure background image is square
                    backgroundImageView.post(new Runnable() {
                        @Override
                        public void run() {
                            int size = Math.min(backgroundImageView.getWidth(), backgroundImageView.getHeight());
                            ViewGroup.LayoutParams layoutParams = backgroundImageView.getLayoutParams();
                            layoutParams.width = size;
                            layoutParams.height = size;
                            backgroundImageView.setLayoutParams(layoutParams);
                        }
                    });

                    dialog.show();
                }
            });
        }
    }


    // Method to schedule next bad apple spawn
    private void scheduleNextBadAppleSpawn() {
        if (!mPaused && mPlaying && !isSpawnScheduled) {
            handler.postDelayed(spawnBadAppleRunnable, 30000 + new Random().nextInt(15000));
            isSpawnScheduled = true;
        }
    }

    // Method to check if a location is occupied
    public boolean isOccupied(Point location) {
        return mSnake.isOccupied(location);
    }

    // Method to stop sounds
    private void stopSounds() {
        AudioContext.stopSharkSwimSound();
        audioContext.stopPowerUpMusic();
    }

    // Method to set background music volume
    private void setBackgroundMusicVolume() {
        audioContext.setMusicVolume(0.4F);
    }
}