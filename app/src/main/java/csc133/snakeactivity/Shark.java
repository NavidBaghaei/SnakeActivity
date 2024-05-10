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

class Shark implements GameObject {
    private PointF location = new PointF();
    private PointF direction = new PointF();
    private int mSize;
    private Bitmap mBitmapShark;
    private Bitmap mFlippedBitmap;
    private boolean isMoving = false;
    private boolean spawnLeft = true; // Indicates the direction of spawn
    private float screenWidth, screenHeight;
    private long lastDeactivatedTime = System.currentTimeMillis();
    private static final long INITIAL_DELAY = 10000;
    private static final long RESPAWN_DELAY = 10000;
    private boolean firstSpawnDone = false;
    private Random random = new Random();
    private Snake snake;
    private static final long DESPAWN_DELAY = 10000; // Delay before respawn after despawn




    public Shark(Context context, int size, float screenWidth, float screenHeight, Snake snake) {
        this.mSize = size;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.snake = snake;
        mBitmapShark = BitmapFactory.decodeResource(context.getResources(), R.drawable.shark);
        mBitmapShark = Bitmap.createScaledBitmap(mBitmapShark, size, size, false);
        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1);
        mFlippedBitmap = Bitmap.createBitmap(mBitmapShark, 0, 0, mBitmapShark.getWidth(), mBitmapShark.getHeight(), matrix, false);
    }

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
                AudioContext.stopSharkSwimSound();
                lastDeactivatedTime = currentTime;
                reset();
            } else {
                location.x = nextX;
            }
        }

        // Check if it's time to respawn the shark after the despawn delay
        if (!isMoving && firstSpawnDone && currentTime - lastDeactivatedTime >= RESPAWN_DELAY) {
            spawn();
        }
    }

    public void reset() {
        isMoving = false;
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
        direction.set(spawnLeft ? 1 : -1, 0);
        isMoving = true;
        AudioContext.playSharkSwimSound();
    }

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



    public PointF getLocation() {
        return new PointF(location.x, location.y);
    }

    public Bitmap getBitmap() {
        return mBitmapShark;
    }

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
