package csc133.snakeactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import java.util.Random;
import java.util.List;
import java.util.Iterator;

public class BadApple implements GameObject {

    private Point location = new Point();
    private Point mSpawnRange;
    private int mSize;
    private Bitmap mBitmapApple;
    private SpaceChecker spaceChecker;

    public BadApple(Context context, Point spawnRange, int size, SpaceChecker spaceChecker) {
        mSpawnRange = spawnRange;
        mSize = size;
        this.spaceChecker = spaceChecker;
        mBitmapApple = BitmapFactory.decodeResource(context.getResources(), R.drawable.bad_apple);
        mBitmapApple = Bitmap.createScaledBitmap(mBitmapApple, size, size, false);
    }

    public void spawn() {
        Random random = new Random();
        do {
            location.x = random.nextInt(mSpawnRange.x);
            location.y = random.nextInt(mSpawnRange.y);
        } while (spaceChecker.isOccupied(location));
    }

    public Point getLocation() {
        return location;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        canvas.drawBitmap(mBitmapApple, location.x * mSize, location.y * mSize, paint);
    }

    @Override
    public void update() {
        // No dynamic updates required for BadApple
    }

    public static void resetAll(List<BadApple> badApples, List<GameObject> gameObjects) {
        badApples.clear();
        gameObjects.removeIf(obj -> obj instanceof BadApple);
    }
}
