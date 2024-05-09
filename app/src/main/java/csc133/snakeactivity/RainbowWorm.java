package csc133.snakeactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import java.util.Random;
import android.os.Handler;
import android.util.Log;

public class RainbowWorm implements GameObject {
    private Point location;
    private Point spawnRange;
    private int size;
    private Bitmap bitmapWorm;
    private SpaceChecker spaceChecker;

    public RainbowWorm(Context context, Point spawnRange, int size, SpaceChecker spaceChecker) {
        this.spawnRange = spawnRange;
        this.size = size;
        this.spaceChecker = spaceChecker;
        this.bitmapWorm = BitmapFactory.decodeResource(context.getResources(), R.drawable.superworm);
        this.bitmapWorm = Bitmap.createScaledBitmap(bitmapWorm, size, size, false);
        this.location = null; // Initialize location as null
    }

    public void delayedSpawn(long delay) {
        new Handler().postDelayed(this::spawn, delay);
    }

    public void spawn() {
        if (location == null) {
            location = new Point();  // Initialize location when spawning
        }
        Random random = new Random();
        do {
            location.x = random.nextInt(spawnRange.x);
            location.y = random.nextInt(spawnRange.y);
        } while (spaceChecker.isOccupied(location));
        Log.d("RainbowWorm", "Spawned at: " + location.x + ", " + location.y);
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (location != null) {  // Check if location is not null before drawing
            canvas.drawBitmap(bitmapWorm, location.x * size, location.y * size, paint);
        }
    }

    @Override
    public void update() {
        // Not needed unless the worm has animations or other behavior while on screen
    }

    public Point getLocation() {
        return location;
    }
}
