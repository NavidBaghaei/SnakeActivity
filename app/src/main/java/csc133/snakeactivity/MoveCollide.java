package csc133.snakeactivity;

import android.graphics.Point;
import java.util.ArrayList;

public abstract class MoveCollide {
    // ArrayList to store the locations of snake segments
    protected ArrayList<Point> segmentLocations;
    // Size of each segment
    protected int mSegmentSize;
    // Range of movement for the snake
    protected Point mMoveRange;
    // Current heading of the snake
    protected Heading heading;

    // Enum for movement heading
    public enum Heading {
        UP, RIGHT, DOWN, LEFT
    }

    // Method to move the snake
    public void move() {
        if (segmentLocations.isEmpty()) {
            // If there are no segments, there's nothing to move
            return;
        }

        // Move the body from the back to the front
        for (int i = segmentLocations.size() - 1; i > 0; i--) {
            segmentLocations.get(i).x = segmentLocations.get(i - 1).x;
            segmentLocations.get(i).y = segmentLocations.get(i - 1).y;
        }

        // Move the head in the appropriate heading
        Point p = segmentLocations.get(0);
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

    // Method to move the snake with optional speed boost
    public void move(boolean speedBoost) {
        // Move the snake normally
        move();

        // If speed boost is active, move a second time
        if (speedBoost) {
            move();
        }
    }

    // Method to detect if the snake has collided with a wall or its own tail
    public boolean detectDeath() {
        return !segmentLocations.isEmpty() && (detectWallCollision() || detectTailCollision());
    }

    // Method to detect wall collision
    public boolean detectWallCollision() {
        if (segmentLocations.isEmpty()) {
            return false; // No collision if there are no segments
        }

        // Get the position of the snake's head
        Point head = segmentLocations.get(0);
        // Check if the head has collided with any of the walls
        return head.x == -1 || head.x > mMoveRange.x || head.y == -1 || head.y > mMoveRange.y;
    }

    // Method to detect tail collision
    public boolean detectTailCollision() {
        Point head = segmentLocations.get(0);
        // Iterate through the segments starting from the second one
        for (int i = 1; i < segmentLocations.size(); i++) {
            // If the head collides with any segment, return true
            if (head.equals(segmentLocations.get(i))) {
                return true;
            }
        }
        return false; // No tail collision detected
    }
}