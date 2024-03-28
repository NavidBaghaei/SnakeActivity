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

public class Snake implements GameObject {

    // The location in the grid of all the segments
    private ArrayList<Point> segmentLocations;

    // How big is each segment of the snake?
    private int mSegmentSize;

    // How big is the entire grid
    private Point mMoveRange;

    // Where is the center of the screen
    // horizontally in pixels?
    private int halfWayPoint;

    // For tracking movement Heading
    public enum Heading {
        UP, RIGHT, DOWN, LEFT
    }

    // Start by heading to the right
    private Heading heading = Heading.RIGHT;

    // A bitmap for each direction the head can face
    private Bitmap mBitmapHeadRight;
    private Bitmap mBitmapHeadLeft;
    private Bitmap mBitmapHeadUp;
    private Bitmap mBitmapHeadDown;

    // A bitmap for the body
    private Bitmap mBitmapBody;



    Snake(Context context, Point mr, int ss) {

        // Initialize our ArrayList
        segmentLocations = new ArrayList<>();

        // Initialize the segment size and movement
        // range from the passed in parameters
        mSegmentSize = ss;
        mMoveRange = mr;

        // Create and scale the bitmaps
        mBitmapHeadRight = BitmapFactory
                .decodeResource(context.getResources(),
                        R.drawable.head);

        // Create 3 more versions of the head for different headings
        mBitmapHeadLeft = BitmapFactory
                .decodeResource(context.getResources(),
                        R.drawable.head);

        mBitmapHeadUp = BitmapFactory
                .decodeResource(context.getResources(),
                        R.drawable.head);

        mBitmapHeadDown = BitmapFactory
                .decodeResource(context.getResources(),
                        R.drawable.head);

        // Modify the bitmaps to face the snake head
        // in the correct direction
        mBitmapHeadRight = Bitmap
                .createScaledBitmap(mBitmapHeadRight,
                        ss, ss, false);

        // A matrix for scaling
        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1);

        mBitmapHeadLeft = Bitmap
                .createBitmap(mBitmapHeadRight,
                        0, 0, ss, ss, matrix, true);

        // A matrix for rotating
        matrix.preRotate(-90);
        mBitmapHeadUp = Bitmap
                .createBitmap(mBitmapHeadRight,
                        0, 0, ss, ss, matrix, true);

        // Matrix operations are cumulative
        // so rotate by 180 to face down
        matrix.preRotate(180);
        mBitmapHeadDown = Bitmap
                .createBitmap(mBitmapHeadRight,
                        0, 0, ss, ss, matrix, true);

        // Create and scale the body
        mBitmapBody = BitmapFactory
                .decodeResource(context.getResources(),
                        R.drawable.body);

        mBitmapBody = Bitmap
                .createScaledBitmap(mBitmapBody,
                        ss, ss, false);

        // The halfway point across the screen in pixels
        // Used to detect which side of screen was pressed
        halfWayPoint = mr.x * ss / 2;
    }

    // Get the snake ready for a new game
    void reset(int w, int h) {

        // Reset the heading
        heading = Heading.RIGHT;

        // Delete the old contents of the ArrayList
        segmentLocations.clear();

        // Start with a single snake segment
        segmentLocations.add(new Point(w / 2, h / 2));
    }

    @Override
    public void update() {
        move();
        // Include additional update logic here if necessary.
    }

    public void move() {

        if (segmentLocations.isEmpty()) {
            // If there are no segments, there's nothing to move
            return;
        }

        // Move the body
        // Start at the back and move it
        // to the position of the segment in front of it
        for (int i = segmentLocations.size() - 1; i > 0; i--) {

            // Make it the same value as the next segment
            // going forwards towards the head
            segmentLocations.get(i).x = segmentLocations.get(i - 1).x;
            segmentLocations.get(i).y = segmentLocations.get(i - 1).y;
        }

        // Move the head in the appropriate heading
        // Get the existing head position
        Point p = segmentLocations.get(0);

        // Move it appropriately
        switch (heading) {
            case UP:
                p.y--;
                break;

            case RIGHT:
                p.x++;
                break;

            case DOWN:
                p.y++;
                break;

            case LEFT:
                p.x--;
                break;
        }

    }


    // Overloaded move method with speed boost
    public void move(boolean speedBoost) {
        // Move the snake normally
        move();

        // If speed boost is active, move a second time
        if (speedBoost) {
            move();
        }
    }

    public void updateHeading(Heading newHeading) {
        // Check that the new heading is not directly opposite to the current heading
        // This prevents the snake from turning back directly on itself
        if (Math.abs(newHeading.ordinal() - this.heading.ordinal()) % 2 == 1) {
            this.heading = newHeading;
        }
    }

    boolean detectDeath() {
        // Simplify the detection logic by calling the specific methods
        return detectWallCollision() || detectTailCollision();
    }

    boolean detectWallCollision() {
        Point head = segmentLocations.get(0);
        return head.x == -1 || head.x > mMoveRange.x || head.y == -1 || head.y > mMoveRange.y;
    }

    boolean detectTailCollision() {
        Point head = segmentLocations.get(0);
        for (int i = 1; i < segmentLocations.size(); i++) {
            if (head.equals(segmentLocations.get(i))) {
                return true;
            }
        }
        return false;
    }



    boolean checkDinner(Point appleLocation) {
        if (segmentLocations.isEmpty()) {
            // The snake has no segments, so it can't "eat" the apple
            return false;
        }

        // Check if the head of the snake is at the same position as the apple
        Point head = segmentLocations.get(0);
        if (head.equals(appleLocation)) {
            // Add a new segment to the snake at an off-screen location (it will be moved into position on the next update)
            segmentLocations.add(new Point(-10, -10));
            return true;
        }

        return false;
    }
    @Override
    public void draw(Canvas canvas, Paint paint) {

        // Don't run this code if ArrayList has nothing in it
        if (!segmentLocations.isEmpty()) {
            // All the code from this method goes here
            // Draw the head
            switch (heading) {
                case RIGHT:
                    canvas.drawBitmap(mBitmapHeadRight,
                            segmentLocations.get(0).x
                                    * mSegmentSize,
                            segmentLocations.get(0).y
                                    * mSegmentSize, paint);
                    break;

                case LEFT:
                    canvas.drawBitmap(mBitmapHeadLeft,
                            segmentLocations.get(0).x
                                    * mSegmentSize,
                            segmentLocations.get(0).y
                                    * mSegmentSize, paint);
                    break;

                case UP:
                    canvas.drawBitmap(mBitmapHeadUp,
                            segmentLocations.get(0).x
                                    * mSegmentSize,
                            segmentLocations.get(0).y
                                    * mSegmentSize, paint);
                    break;

                case DOWN:
                    canvas.drawBitmap(mBitmapHeadDown,
                            segmentLocations.get(0).x
                                    * mSegmentSize,
                            segmentLocations.get(0).y
                                    * mSegmentSize, paint);
                    break;
            }

            // Draw the snake body one block at a time
            for (int i = 1; i < segmentLocations.size(); i++) {
                canvas.drawBitmap(mBitmapBody,
                        segmentLocations.get(i).x
                                * mSegmentSize,
                        segmentLocations.get(i).y
                                * mSegmentSize, paint);
            }
        }
    }

    public boolean isOccupying(Point location) {
        for (Point segment : segmentLocations) {
            if (segment.equals(location)) {
                return true; // The point is occupied by a segment of the snake
            }
        }
        return false; // The point is not occupied by the snake
    }

    // Handle changing direction
    void switchHeading(MotionEvent motionEvent) {

        // Is the tap on the right hand side?
        if (motionEvent.getX() >= halfWayPoint) {
            switch (heading) {
                // Rotate right
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
            // Rotate left
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
}