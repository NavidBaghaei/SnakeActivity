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
    private Point mSpawnRange;
    private int mSize;
    private Bitmap mBitmapSuperApple;
    private SpaceChecker spaceChecker;
    private long lastEatenTimestamp; // Timestamp when the SuperApple was last eaten
    private long lastSpawnTime; // Time when the SuperApple was last spawned
    private long respawnDelay = 10000; // Respawn delay in milliseconds
    private long initialSpawnDelay = 15000; // Initial delay before first spawn
    private boolean isInitiallySpawned = false; // Flag to check if initially spawned

    public SuperApple(Context context, Point spawnRange, int size, SpaceChecker spaceChecker) {
        mSpawnRange = spawnRange;
        mSize = size;
        this.spaceChecker = spaceChecker;

        // Load and scale the Super Apple bitmap
        mBitmapSuperApple = BitmapFactory.decodeResource(context.getResources(), R.drawable.super_apple);
        mBitmapSuperApple = Bitmap.createScaledBitmap(mBitmapSuperApple, mSize, mSize, false);

        lastSpawnTime = System.currentTimeMillis(); // Set the initial spawn timer
    }

    public void spawn() {
        long currentTime = System.currentTimeMillis();

        // Logging for debugging
        Log.d("SuperApple", "Attempting to spawn: isInitiallySpawned = " + isInitiallySpawned);

        // Check for initial spawn delay
        if (!isInitiallySpawned && currentTime - lastSpawnTime < initialSpawnDelay) {
            Log.d("SuperApple", "Initial spawn delay not yet passed.");
            return; // Do not spawn if initial delay has not passed
        }

        // Check for respawn delay
        if (isInitiallySpawned && currentTime - lastEatenTimestamp < respawnDelay) {
            Log.d("SuperApple", "Respawn delay not yet passed.");
            return; // Do not respawn if respawn delay has not passed
        }

        // Generate a new random location that is not occupied
        Random random = new Random();
        do {
            location.x = random.nextInt(mSpawnRange.x);
            location.y = random.nextInt(mSpawnRange.y);
        } while (spaceChecker.isOccupied(location));

        // Reset the appropriate timestamps and state flags
        if (!isInitiallySpawned) {
            lastSpawnTime = currentTime; // Update lastSpawnTime on first spawn
        }
        lastEatenTimestamp = currentTime; // Update to current time to manage respawn delay
        isInitiallySpawned = true; // Mark as spawned at least once

        // Confirm spawning for debugging
        Log.d("SuperApple", "SuperApple spawned at (" + location.x + ", " + location.y + ")");
    }


    @Override
    public void draw(Canvas canvas, Paint paint) {
        long currentTime = System.currentTimeMillis();
        // Only draw if the appropriate delay has passed
        if ((isInitiallySpawned && currentTime - lastEatenTimestamp >= respawnDelay) ||
                (!isInitiallySpawned && currentTime - lastSpawnTime >= initialSpawnDelay)) {
            canvas.drawBitmap(mBitmapSuperApple, location.x * mSize, location.y * mSize, paint);
        }
    }

    @Override
    public void update() {
        // Typically, no dynamic updates are needed unless there are time-based effects
    }

    public void markAsEaten() {
        lastEatenTimestamp = System.currentTimeMillis(); // Record the time it was eaten
        isInitiallySpawned = false; // Reset this flag to manage the respawn correctly
    }

    public Point getLocation() {
        return location;
    }

    public long getRespawnDelay() {
        return respawnDelay; // Accessor for the respawn delay
    }
}
