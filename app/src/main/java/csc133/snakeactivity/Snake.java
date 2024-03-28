package csc133.snakeactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.MotionEvent;

import java.util.ArrayList;

class Snake {

    private ArrayList<Point> segmentLocations;
    private int mSegmentSize;
    private Point mMoveRange;
    private int halfWayPoint;
    private Heading heading = Heading.RIGHT;
    private Bitmap mBitmapHeadRight, mBitmapHeadLeft, mBitmapHeadUp, mBitmapHeadDown, mBitmapBody;

    private enum Heading {
        UP, RIGHT, DOWN, LEFT
    }

    Snake(Context context, Point mr, int ss) {
        segmentLocations = new ArrayList<>();
        mSegmentSize = ss;
        mMoveRange = mr;
        initializeBitmaps(context, ss);
        halfWayPoint = mr.x * ss / 2;
    }

    private void initializeBitmaps(Context context, int ss) {
        mBitmapHeadRight = BitmapFactory.decodeResource(context.getResources(), R.drawable.head);
        mBitmapHeadLeft = getRotatedBitmap(mBitmapHeadRight, ss, 180);
        mBitmapHeadUp = getRotatedBitmap(mBitmapHeadRight, ss, 270);
        mBitmapHeadDown = getRotatedBitmap(mBitmapHeadRight, ss, 90);
        mBitmapBody = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.body), ss, ss, false);
    }

    private Bitmap getRotatedBitmap(Bitmap source, int size, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, size, size, matrix, true);
    }

    void reset(int w, int h) {
        heading = Heading.RIGHT;
        segmentLocations.clear();
        segmentLocations.add(new Point(w / 2, h / 2));
    }

    void move() {
        if (segmentLocations.isEmpty()) return;

        for (int i = segmentLocations.size() - 1; i > 0; i--) {
            segmentLocations.get(i).set(segmentLocations.get(i - 1).x, segmentLocations.get(i - 1).y);
        }

        Point head = segmentLocations.get(0);
        switch (heading) {
            case UP: head.y--; break;
            case RIGHT: head.x++; break;
            case DOWN: head.y++; break;
            case LEFT: head.x--; break;
        }
    }

    void move(boolean speedBoost) {
        move();
        if (speedBoost) move();
    }

    void move(Heading newDirection) {
        if (Math.abs(newDirection.ordinal() - this.heading.ordinal()) % 2 == 1) {
            this.heading = newDirection;
            move();
        }
    }

    boolean detectDeath() {
        if (segmentLocations.isEmpty()) return false;

        Point head = segmentLocations.get(0);
        if (head.x < 0 || head.x >= mMoveRange.x || head.y < 0 || head.y >= mMoveRange.y) return true;

        for (int i = 1; i < segmentLocations.size(); i++) {
            if (head.equals(segmentLocations.get(i))) return true;
        }
        return false;
    }

    boolean checkDinner(Point appleLocation) {
        if (segmentLocations.isEmpty()) return false;

        if (segmentLocations.get(0).equals(appleLocation)) {
            segmentLocations.add(new Point(-10, -10));
            return true;
        }
        return false;
    }

    void draw(Canvas canvas, Paint paint) {
        if (segmentLocations.isEmpty()) return;

        switch (heading) {
            case RIGHT: canvas.drawBitmap(mBitmapHeadRight, segmentLocations.get(0).x * mSegmentSize, segmentLocations.get(0).y * mSegmentSize, paint); break;
            case LEFT: canvas.drawBitmap(mBitmapHeadLeft, segmentLocations.get(0).x * mSegmentSize, segmentLocations.get(0).y * mSegmentSize, paint); break;
            case UP: canvas.drawBitmap(mBitmapHeadUp, segmentLocations.get(0).x * mSegmentSize, segmentLocations.get(0).y * mSegmentSize, paint); break;
            case DOWN: canvas.drawBitmap(mBitmapHeadDown, segmentLocations.get(0).x * mSegmentSize, segmentLocations.get(0).y * mSegmentSize, paint); break;
        }

        for (int i = 1; i < segmentLocations.size(); i++) {
            canvas.drawBitmap(mBitmapBody, segmentLocations.get(i).x * mSegmentSize, segmentLocations.get(i).y * mSegmentSize, paint);
        }
    }

    void switchHeading(MotionEvent motionEvent) {
        if (motionEvent.getX() >= halfWayPoint) {
            switch (heading) {
                case UP: heading = Heading.RIGHT; break;
                case RIGHT: heading = Heading.DOWN; break;
                case DOWN: heading = Heading.LEFT; break;
                case LEFT: heading = Heading.UP; break;
            }
        } else {
            switch (heading) {
                case UP: heading = Heading.LEFT; break;
                case LEFT: heading = Heading.DOWN; break;
                case DOWN: heading = Heading.RIGHT; break;
                case RIGHT: heading = Heading.UP; break;
            }
        }
    }
}
