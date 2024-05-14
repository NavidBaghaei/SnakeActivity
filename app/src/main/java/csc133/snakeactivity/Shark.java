package csc133.snakeactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;
import android.graphics.Point;
import java.util.Random;
import android.os.Handler;

// Class representing the shark object in the game
class Shark implements GameObject {
    // Position of the shark
    private PointF location = new PointF();
    // Direction of the shark's movement
    private PointF direction = new PointF();
    // Size of the shark
    private int mSize;
    // Bitmap for the shark
    private Bitmap mBitmapShark;
    // Flipped bitmap for the shark (for leftward movement)
    private Bitmap mFlippedBitmap;
    // Flag indicating if the shark is moving
    private boolean isMoving = false;
    // Flag indicating the direction of shark spawn
    private boolean spawnLeft = true;
    // Screen dimensions
    private float screenWidth, screenHeight;
    // Time when the shark was last deactivated
    private long lastDeactivatedTime = System.currentTimeMillis();
    // Delay before initial spawn
    private static final long INITIAL_DELAY = 10000;
    // Delay before respawn after despawn
    private static final long RESPAWN_DELAY = 10000;
    // Delay before respawn after despawn
    private static final long DESPAWN_DELAY = 10000;
    // Flag indicating if the first spawn has occurred
    private boolean firstSpawnDone = false;
    // Random object for generating random values
    private Random random = new Random();
    // Reference to the snake object
    private ISnake snake;

    // Constructor
    public Shark(Context context, int size, float screenWidth, float screenHeight, ISnake snake) {
        this.mSize = size;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.snake = snake;
        // Load shark bitmap from resources and scale it to the specified size
        mBitmapShark = BitmapFactory.decodeResource(context.getResources(), R.drawable.shark);
        mBitmapShark = Bitmap.createScaledBitmap(mBitmapShark, size, size, false);
        // Create a flipped bitmap for leftward movement
        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1);
        mFlippedBitmap = Bitmap.createBitmap(mBitmapShark, 0, 0, mBitmapShark.getWidth(), mBitmapShark.getHeight(), matrix, false);
    }

    // Update method to handle shark movement and respawn logic
    public void update() {
        long currentTime = System.currentTimeMillis();

        // Check if it's time to spawn the shark
        if (!isMoving && ((currentTime - lastDeactivatedTime >= INITIAL_DELAY && !firstSpawnDone) ||
                (currentTime - lastDeactivatedTime >= RESPAWN_DELAY && firstSpawnDone))) {
            spawn();
        }

        // Despawn the shark if it goes off the screen
        if (isMoving) {
            float nextX = location.x + direction.x * 40;
            if ((spawnLeft && nextX > screenWidth) || (!spawnLeft && nextX + mBitmapShark.getWidth() < 0)) {
                isMoving = false;
                AudioContext.stopSharkSwimSound(); // Stop shark audio
                lastDeactivatedTime = currentTime;
                reset(); // Reset the shark's state
            } else {
                location.x = nextX;
            }
        }

        // Check if it's time to respawn the shark after the despawn delay
        if (!isMoving && firstSpawnDone && currentTime - lastDeactivatedTime >= RESPAWN_DELAY) {
            spawn();
        }
    }

    // Reset method to reset the shark's state
    public void reset() {
        isMoving = false;
        AudioContext.stopSharkSwimSound(); // Stop shark audio
        // Reset the shark's position to a valid position off-screen
        if (spawnLeft) {
            location.x = -mBitmapShark.getWidth();
        } else {
            location.x = screenWidth;
        }
        firstSpawnDone = false; // Reset the firstSpawnDone flag
        spawnLeft = random.nextBoolean(); // Randomize the spawn side again
        lastDeactivatedTime = System.currentTimeMillis(); // Reset lastDeactivatedTime
    }

    // Spawn method to spawn the shark at a random position
    public void spawn() {
        float minY = 50; // Slightly away from the very top and bottom
        float maxY = screenHeight - mBitmapShark.getHeight() - 50;
        float y = minY + random.nextFloat() * (maxY - minY);

        // Calculate x position based on spawn direction
        float x = spawnLeft ? -mBitmapShark.getWidth() : screenWidth;

        // Check if spawn position collides with the snake
        while (snake.isOccupied(new Point((int) x, (int) y))) {
            y = minY + random.nextFloat() * (maxY - minY); // Recalculate y
        }

        location.set(x, y);
        direction.set(spawnLeft ? 1 : -1, 0); // Set movement direction
        isMoving = true;
        AudioContext.playSharkSwimSound(); // Play shark swim sound
    }

    // Method to check collision between the shark and the snake
    public boolean checkCollision(Snake snake) {
        if (!isMoving) {
            return false; // Do not check for collisions if the shark is not moving
        }

        // Retrieve the snake's head location
        Point snakeHead = snake.getHeadLocation();

        int snakeSize = snake.getSegmentSize(); // Assuming this method exists to get the size of the snake's segment

        // Define the shark's hitbox
        int sharkLeft = (int) location.x;
        int sharkRight = sharkLeft + mBitmapShark.getWidth();
        int sharkTop = (int) location.y;
        int sharkBottom = sharkTop + mBitmapShark.getHeight();

        // Check if the snake's head intersects the shark's rectangle
        return (snakeHead.x < sharkRight && snakeHead.x + snakeSize > sharkLeft &&
                snakeHead.y < sharkBottom && snakeHead.y + snakeSize > sharkTop);
    }

    // Getter method for shark's location
    public PointF getLocation() {
        return new PointF(location.x, location.y);
    }

    // Getter method for shark's bitmap
    public Bitmap getBitmap() {
        return mBitmapShark;
    }

    // Method to draw the shark on the canvas
    public void draw(Canvas canvas, Paint paint) {
        Bitmap toDraw = mBitmapShark;
        if (!spawnLeft) {
            // Flip the bitmap horizontally if the shark is moving towards the left
            Matrix matrix = new Matrix();
            matrix.setScale(-1, 1);
            matrix.postTranslate(location.x + mBitmapShark.getWidth(), location.y);
            canvas.drawBitmap(mBitmapShark, matrix, paint);
        } else {
            canvas.drawBitmap(toDraw, location.x, location.y, paint);
        }
    }
}