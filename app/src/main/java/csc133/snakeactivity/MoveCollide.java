package csc133.snakeactivity;

import android.graphics.Point;
import java.util.ArrayList;

public abstract class MoveCollide {
    protected ArrayList<Point> segmentLocations;
    protected int mSegmentSize;
    protected Point mMoveRange;
    protected Heading heading;

    // Enum for movement heading
    public enum Heading {
        UP, RIGHT, DOWN, LEFT
    }

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

    public void move(boolean speedBoost) {
        // Move the snake normally
        move();

        // If speed boost is active, move a second time
        if (speedBoost) {
            move();
        }
    }

    public boolean detectDeath() {
        return detectWallCollision() || detectTailCollision();
    }

    public boolean detectWallCollision() {
        Point head = segmentLocations.get(0);
        return head.x == -1 || head.x > mMoveRange.x || head.y == -1 || head.y > mMoveRange.y;
    }

    public boolean detectTailCollision() {
        Point head = segmentLocations.get(0);
        for (int i = 1; i < segmentLocations.size(); i++) {
            if (head.equals(segmentLocations.get(i))) {
                return true;
            }
        }
        return false;
    }
}
