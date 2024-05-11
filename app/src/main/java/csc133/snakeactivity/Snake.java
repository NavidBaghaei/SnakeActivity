package csc133.snakeactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.MotionEvent;
import java.util.ArrayList;

public class Snake extends MoveCollide implements GameObject, SpaceChecker, ISnake {
    private Bitmap mBitmapHeadRight;
    private Bitmap mBitmapHeadLeft;
    private Bitmap mBitmapHeadUp;
    private Bitmap mBitmapHeadDown;
    private Bitmap mBitmapBody;
    private int halfWayPoint;
    private int mScore; // Track the score
    private int segmentSize;

    Snake(Context context, Point mr, int ss) {
        segmentLocations = new ArrayList<>();
        mSegmentSize = ss;
        mMoveRange = mr;
        heading = Heading.RIGHT;

        // Initialize and scale bitmaps
        mBitmapHeadRight = BitmapFactory.decodeResource(context.getResources(), R.drawable.head);
        mBitmapHeadRight = Bitmap.createScaledBitmap(mBitmapHeadRight, ss, ss, false);

        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1);
        mBitmapHeadLeft = Bitmap.createBitmap(mBitmapHeadRight, 0, 0, ss, ss, matrix, true);

        matrix.preRotate(-90);
        mBitmapHeadUp = Bitmap.createBitmap(mBitmapHeadRight, 0, 0, ss, ss, matrix, true);

        matrix.preRotate(180);
        mBitmapHeadDown = Bitmap.createBitmap(mBitmapHeadRight, 0, 0, ss, ss, matrix, true);

        mBitmapBody = BitmapFactory.decodeResource(context.getResources(), R.drawable.body);
        mBitmapBody = Bitmap.createScaledBitmap(mBitmapBody, ss, ss, false);

        halfWayPoint = mr.x * ss / 2;
    }
@Override
    public void reset(int w, int h) {
        // Reset the heading to the initial direction, typically to the right
        heading = Heading.RIGHT;

        // Clear any existing segments to prepare for a new game start
        segmentLocations.clear();

        // Add the initial segment of the snake at the center of the game area
        segmentLocations.add(new Point(w / 2, h / 2));

        // Reset the score to zero for the new game
        mScore = 0;
    }

    @Override
    public void update() {
        move();
    }
    @Override
    public void updateHeading(Heading newHeading) {
        if (Math.abs(newHeading.ordinal() - this.heading.ordinal()) % 2 == 1) {
            this.heading = newHeading;
        }
    }
@Override
   public boolean checkSuperAppleDinner(Point superAppleLocation) {
        if (segmentLocations.isEmpty()) {
            return false; // Return false if there are no segments to check against
        }

        Point head = segmentLocations.get(0); // Retrieve the head of the snake
        return head.equals(superAppleLocation); // Check if the head is at the same location as the SuperApple
    }

@Override
    public boolean checkDinner(Point appleLocation) {
        // Check if there are any segments before accessing them
        if (segmentLocations.isEmpty()) {
            return false; // Return false if there are no segments to check against
        }

        // Retrieve the head of the snake
        Point head = segmentLocations.get(0);

        // Check if the head of the snake is at the same location as the apple
        if (head.equals(appleLocation)) {
            // Add a new segment at a non-visible location; this will be adjusted by the snake's movement logic
            segmentLocations.add(new Point(-10, -10));

            // Increment the score for each new segment
            mScore++;

            return true; // Return true if the snake has eaten the apple
        }
        return false; // Return false if the snake has not eaten the apple
    }
@Override
    public boolean detectDeath() {
        return !segmentLocations.isEmpty() && (detectWallCollision() || detectTailCollision());
    }
    @Override
    public boolean checkCollisionWithHead(PointF sharkPoint, int sharkWidth, int sharkHeight) {
        // Ensure there is at least one segment to check against
        if (segmentLocations.isEmpty()) {
            return false; // Early exit if there are no segments
        }

        // Retrieve the head of the snake
        Point head = segmentLocations.get(0); // Safe to access as we've checked the list is not empty

        // Calculate the bounds of the snake's head
        float headLeft = head.x * mSegmentSize;
        float headRight = (head.x + 1) * mSegmentSize;
        float headTop = head.y * mSegmentSize;
        float headBottom = (head.y + 1) * mSegmentSize;

        // Calculate the bounds of the shark
        float sharkLeft = sharkPoint.x;
        float sharkRight = sharkPoint.x + sharkWidth;
        float sharkTop = sharkPoint.y;
        float sharkBottom = sharkPoint.y + sharkHeight;

        // Check if the shark's position intersects with the head of the snake
        return (sharkLeft < headRight && sharkRight > headLeft &&
                sharkTop < headBottom && sharkBottom > headTop);
    }


    @Override
    // Method within the Snake class to reduce the length of the snake by a certain number of segments
    public void reduceLength(int lengthToRemove) {
        // Check to make sure we're not trying to remove more segments than exist
        int segmentsToRemove = Math.min(lengthToRemove, segmentLocations.size() - 1);
        // Remove the specified number of segments from the end of the list
        for (int i = 0; i < segmentsToRemove; i++) {
            if (!segmentLocations.isEmpty()) {
                segmentLocations.remove(segmentLocations.size() - 1);
            }
        }
    }
    @Override
    public int removeCollidedSegments(PointF sharkPoint, int sharkWidth, int sharkHeight) {
        // Check if there are only head segments to avoid unnecessary checks
        if (segmentLocations.size() < 2) {
            return 0; // No body segments to check against, so no segments can be removed
        }

        int segmentsRemoved = 0;
        // Start checking from the second segment to exclude the head
        for (int i = 1; i < segmentLocations.size(); i++) {
            Point part = segmentLocations.get(i);
            float partLeft = part.x * mSegmentSize;
            float partRight = (part.x + 1) * mSegmentSize;
            float partTop = part.y * mSegmentSize;
            float partBottom = (part.y + 1) * mSegmentSize;

            // Calculate the bounds of the shark
            float sharkLeft = sharkPoint.x;
            float sharkRight = sharkPoint.x + sharkWidth;
            float sharkTop = sharkPoint.y;
            float sharkBottom = sharkPoint.y + sharkHeight;

            // Check if the shark intersects with any body segment
            if (sharkLeft < partRight && sharkRight > partLeft &&
                    sharkTop < partBottom && sharkBottom > partTop) {
                segmentsRemoved = segmentLocations.size() - i;
                segmentLocations.subList(i, segmentLocations.size()).clear(); // Remove from collision point to end of tail
                return segmentsRemoved; // Return the number of segments removed and exit after the first collision detection
            }
        }
        return segmentsRemoved; // Return the number of segments removed or 0 if no collision
    }


    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (!segmentLocations.isEmpty()) {
            switch (heading) {
                case RIGHT:
                    canvas.drawBitmap(mBitmapHeadRight, segmentLocations.get(0).x * mSegmentSize, segmentLocations.get(0).y * mSegmentSize, paint);
                    break;
                case LEFT:
                    canvas.drawBitmap(mBitmapHeadLeft, segmentLocations.get(0).x * mSegmentSize, segmentLocations.get(0).y * mSegmentSize, paint);
                    break;
                case UP:
                    canvas.drawBitmap(mBitmapHeadUp, segmentLocations.get(0).x * mSegmentSize, segmentLocations.get(0).y * mSegmentSize, paint);
                    break;
                case DOWN:
                    canvas.drawBitmap(mBitmapHeadDown, segmentLocations.get(0).x * mSegmentSize, segmentLocations.get(0).y * mSegmentSize, paint);
                    break;
            }
            for (int i = 1; i < segmentLocations.size(); i++) {
                canvas.drawBitmap(mBitmapBody, segmentLocations.get(i).x * mSegmentSize, segmentLocations.get(i).y * mSegmentSize, paint);
            }
        }
    }

    @Override
    public boolean isOccupied(Point location) {
        for (Point segment : segmentLocations) {
            if (segment.equals(location)) {
                return true;
            }
        }
        return false;
    }
@Override
   public void switchHeading(MotionEvent motionEvent) {
        if (motionEvent.getX() >= halfWayPoint) {
            switch (heading) {
                case UP:
                    heading = Heading.RIGHT;
                    break;
                case RIGHT:
                    heading = Heading.DOWN;
                    break;
                case DOWN:
                    heading = Heading.LEFT;
                    break;
                case LEFT:
                    heading = Heading.UP;
                    break;
            }
        } else {
            switch (heading) {
                case UP:
                    heading = Heading.LEFT;
                    break;
                case LEFT:
                    heading = Heading.DOWN;
                    break;
                case DOWN:
                    heading = Heading.RIGHT;
                    break;
                case RIGHT:
                    heading = Heading.UP;
                    break;
            }
        }
    }

    public int getSegmentSize() {
        return this.segmentSize;
    }
    @Override
    public Point getHeadLocation() {
        if (!segmentLocations.isEmpty()) {
            return new Point(segmentLocations.get(0));
        }
        return null; // Return null if there are no segments
    }

    @Override
    public int getSegmentCount() {
        return segmentLocations.size();
    }

    @Override
    public void removeSegments(int count) {
        int segmentsToRemove = Math.min(count, segmentLocations.size() - 1);
        for (int i = 0; i < segmentsToRemove; i++) {
            if (!segmentLocations.isEmpty()) {
                segmentLocations.remove(segmentLocations.size() - 1);
            }
        }
    }


}
