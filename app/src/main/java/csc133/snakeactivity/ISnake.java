package csc133.snakeactivity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.MotionEvent;

public interface ISnake extends GameObject {
    void draw(Canvas canvas, Paint paint);  // For drawing the snake, where you will change the color
    void update();  // For updating the snake's state each frame
    boolean checkDinner(Point location);  // To check if the snake has eaten an apple
    Point getHeadLocation();  // To retrieve the location of the snake's head
    void switchHeading(MotionEvent motionEvent);  // To change the snake's direction based on user input
    boolean isOccupied(Point location);
    boolean checkSuperAppleDinner(Point superAppleLocation);
    boolean detectDeath();
    int removeCollidedSegments(PointF sharkPoint, int sharkWidth, int sharkHeight);
    void reduceLength(int lengthToRemove);
    boolean checkCollisionWithHead(PointF sharkPoint, int sharkWidth, int sharkHeight);
    void reset(int w, int h);
    // Inside ISnake interface
    int getSegmentCount();
    void removeSegments(int count);
    void updateHeading(MoveCollide.Heading newHeading);

}

// Assuming Heading is an enum used in the Snake class for direction handling
enum Heading {
    UP, DOWN, LEFT, RIGHT;
}
