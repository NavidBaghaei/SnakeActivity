package csc133.snakeactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import java.util.Random;
import android.util.Log;

public class SuperApple implements GameObject {
    private Point location = new Point();
    private Point velocity = new Point(1, 1); // Manageable velocity
    private int updateFrequency = 2;
    private int updateCounter = 0;
    private Point mSpawnRange;
    private int mSize;
    private Bitmap mBitmapSuperApple;
    private SpaceChecker spaceChecker;
    private long lastEatenTimestamp;
    private long lastSpawnTime;
    private long visibleDuration = 10000; // 10 seconds visibility
    private long visibleStartTime; // Time when the apple became visible
    private long respawnDelay = 65000;
    private long initialSpawnDelay = 30000;
    private static boolean isSpawned;
    private static boolean isVisible;

    public SuperApple(Context context, Point spawnRange, int size, SpaceChecker spaceChecker) {
        mSpawnRange = spawnRange;
        mSize = size;
        this.spaceChecker = spaceChecker;
        mBitmapSuperApple = BitmapFactory.decodeResource(context.getResources(), R.drawable.super_apple);
        mBitmapSuperApple = Bitmap.createScaledBitmap(mBitmapSuperApple, mSize, mSize, false);
        reset();
    }

    public void spawn() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSpawnTime >= initialSpawnDelay && !isSpawned) {
            do {
                location.x = new Random().nextInt(mSpawnRange.x);
                location.y = new Random().nextInt(mSpawnRange.y);
            } while (spaceChecker.isOccupied(location));

            lastSpawnTime = currentTime;
            visibleStartTime = currentTime;
            isSpawned = true;
            isVisible = true;
            Log.d("SuperApple", "SuperApple spawned at (" + location.x + ", " + location.y + ")");
        }
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (isVisible && isSpawned) {
            canvas.drawBitmap(mBitmapSuperApple, location.x * mSize, location.y * mSize, paint);
        }
    }

    @Override
    public void update() {
        if (isSpawned) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - visibleStartTime > visibleDuration) {
                isVisible = false;
                isSpawned = false;
            } else {
                updateCounter++;
                if (updateCounter >= updateFrequency) {
                    // Update position based on velocity
                    location.x += velocity.x;
                    location.y += velocity.y;

                    // Boundary checks
                    if (location.x <= 0 || location.x >= mSpawnRange.x - 1) {
                        velocity.x = -velocity.x;
                        location.x = Math.max(0, Math.min(location.x, mSpawnRange.x - 1)); // Prevent sticking
                    }
                    if (location.y <= 0 || location.y >= mSpawnRange.y - 1) {
                        velocity.y = -velocity.y;
                        location.y = Math.max(0, Math.min(location.y, mSpawnRange.y - 1)); // Prevent sticking
                    }

                    updateCounter = 0;
                }
            }
        }
    }

    public void markAsEaten() {
        lastEatenTimestamp = System.currentTimeMillis();
        isSpawned = false;
        isVisible = false;
    }

    public void reset() {
        isSpawned = false;
        isVisible = false;
        lastSpawnTime = System.currentTimeMillis() - initialSpawnDelay;
        lastEatenTimestamp = System.currentTimeMillis();
    }

    public static void gameOver() {
        isSpawned = false;
        isVisible = false;
        Log.d("SuperApple", "Game Over: SuperApple is reset.");
    }

    public Point getLocation() {
        return location;
    }

    public long getRespawnDelay() {
        return respawnDelay;
    }
}
