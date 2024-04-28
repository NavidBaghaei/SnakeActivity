package csc133.snakeactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;
import java.util.Random;

class Shark implements GameObject {
    private PointF location = new PointF();
    private PointF direction = new PointF();
    private int mSize;
    private Bitmap mBitmapShark;
    private Random random = new Random();
    private SpaceChecker spaceChecker;
    private boolean isMoving = false;
    private long startTime;
    private static final long ROTATION_TIME = 5000; // Time before starting to move
    private float screenWidth, screenHeight;
    private float speedFactor = 10.0f;  // Increased speed factor

    public Shark(Context context, int size, SpaceChecker spaceChecker, float screenWidth, float screenHeight) {
        this.mSize = size;
        this.spaceChecker = spaceChecker;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        mBitmapShark = BitmapFactory.decodeResource(context.getResources(), R.drawable.shark);
        mBitmapShark = Bitmap.createScaledBitmap(mBitmapShark, size, size, false);
    }

    void spawn(PointF targetLocation) {
        do {
            location.x = random.nextInt((int) (screenWidth - mSize));
            location.y = random.nextInt((int) (screenHeight - mSize));
        } while (spaceChecker.isOccupied(new Point((int) location.x, (int) location.y)));

        Log.d("Shark", "Spawning at x:" + location.x + " y:" + location.y);
        setDirection(targetLocation);
        startTime = System.currentTimeMillis();
        isMoving = false;  // Initially, the shark is not moving; it's rotating
    }

    private void setDirection(PointF targetLocation) {
        direction.x = targetLocation.x - location.x;
        direction.y = targetLocation.y - location.y;
        normalizeDirection();
        direction.x *= speedFactor;  // Apply speed factor to direction
        direction.y *= speedFactor;
        Log.d("Shark", "Direction set to x:" + direction.x + " y:" + direction.y);
    }

    private void normalizeDirection() {
        float length = (float) Math.sqrt(direction.x * direction.x + direction.y * direction.y);
        if (length != 0) {
            direction.x /= length;
            direction.y /= length;
        } else {
            direction.x = 0;
            direction.y = 1;  // Default direction if division by zero occurs
        }
    }

    @Override
    public void update() {
        if (!isMoving && System.currentTimeMillis() - startTime > ROTATION_TIME) {
            isMoving = true;  // Start moving after the rotation time has passed
            Log.d("Shark", "Starting to move");
        }

        if (isMoving) {
            float nextX = location.x + direction.x;
            float nextY = location.y + direction.y;

            if (nextX < 0 || nextX > screenWidth || nextY < 0 || nextY > screenHeight) {
                Log.d("Shark", "Off-screen - Respawning");
                respawn();  // Respawn if next position is off-screen
            } else {
                location.x = nextX;
                location.y = nextY;
                Log.d("Shark", "Moving to x:" + location.x + " y:" + location.y);
            }
        }
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        Matrix matrix = new Matrix();
        if (!isMoving) {
            float rotateAngle = ((System.currentTimeMillis() - startTime) % ROTATION_TIME) / (float) ROTATION_TIME * 360;
            matrix.postRotate(rotateAngle, mBitmapShark.getWidth() / 2f, mBitmapShark.getHeight() / 2f);
        }
        matrix.postTranslate(location.x, location.y);
        canvas.drawBitmap(mBitmapShark, matrix, paint);
    }

    private void respawn() {
        location.x = random.nextInt((int) (screenWidth - mSize));
        location.y = random.nextInt((int) (screenHeight - mSize));
        isMoving = false;
        startTime = System.currentTimeMillis(); // Reset the rotation timer
    }

    public boolean isOffScreen() {
        return location.x < 0 || location.x > screenWidth || location.y < 0 || location.y > screenHeight;
    }

    public boolean isMoving() {
        return isMoving;
    }
}
