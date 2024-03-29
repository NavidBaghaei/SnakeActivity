package csc133.snakeactivity;

import android.graphics.Point;

public interface Collidable {
    boolean detectDeath();

    boolean detectWallCollision();

    boolean detectTailCollision();
}