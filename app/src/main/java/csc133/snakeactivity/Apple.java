package csc133.snakeactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import java.util.Random;


class Apple implements GameObject {

    private Point location = new Point();
    private Point mSpawnRange;
    private int mSize;
    private Bitmap mBitmapApple;
    private SpaceChecker spaceChecker; // Add this line

    public Apple(Context context, Point sr, int s, SpaceChecker spaceChecker) { // Updated constructor
        mSpawnRange = sr;
        mSize = s;
        this.spaceChecker = spaceChecker; // Initialize the spaceChecker

        // Load and scale the apple bitmap
        mBitmapApple = BitmapFactory.decodeResource(context.getResources(), R.drawable.apple);
        mBitmapApple = Bitmap.createScaledBitmap(mBitmapApple, s, s, false);
    }

    void spawn() {
        Random random = new Random();

        location.x = random.nextInt(mSpawnRange.x) + 1;
        location.y = random.nextInt(mSpawnRange.y - 1) + 1;

        do {
            location.x = random.nextInt(mSpawnRange.x);
            location.y = random.nextInt(mSpawnRange.y);
        } while (spaceChecker.isOccupied(location)); // Use the checker here

    }



    Point getLocation() {
        return location;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        canvas.drawBitmap(mBitmapApple, location.x * mSize, location.y * mSize, paint);
    }

    @Override
    public void update() {
        // For Apple, update might not modify state but is required by GameObject.
        // This method could be used for effects like blinking or moving the apple in future enhancements.
    }

}
