package csc133.snakeactivity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.MotionEvent;

public class SnakeDecorator implements ISnake {
    protected ISnake decoratedSnake;
    private boolean powerUpActive = false;  // State to track whether the power-up is active
    private long powerUpEndTime;  // Timestamp indicating when the power-up effect should expire

    public SnakeDecorator(ISnake snake) {
        this.decoratedSnake = snake;
        this.powerUpEndTime = 0;  // Initialize the power-up end time to 0
    }

    // Method to activate or deactivate the power-up
    public void setPowerUpActive(boolean active, long duration) {
        this.powerUpActive = active;
        if (active) {
            this.powerUpEndTime = System.currentTimeMillis() + duration;  // Set the end time of the power-up
        } else {
            this.powerUpEndTime = 0;  // Reset the power-up end time
        }
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (powerUpActive && System.currentTimeMillis() < powerUpEndTime) {
            paint.setColor(Color.YELLOW); // Change the color when power-up is active
        }
        decoratedSnake.draw(canvas, paint);
        if (powerUpActive && System.currentTimeMillis() >= powerUpEndTime) {
            paint.setColor(Color.BLUE); // Reset the paint color back for other uses
            powerUpActive = false;  // Reset the power-up state
        }
    }

    @Override
    public void update() {
        decoratedSnake.update();
    }

    @Override
    public boolean checkDinner(Point location) {
        return decoratedSnake.checkDinner(location);
    }

    @Override
    public Point getHeadLocation() {
        return decoratedSnake.getHeadLocation();
    }

    @Override
    public void switchHeading(MotionEvent motionEvent) {
        decoratedSnake.switchHeading(motionEvent);
    }

    @Override
    public boolean isOccupied(Point location) {
        return decoratedSnake.isOccupied(location);
    }

    @Override
    public boolean checkSuperAppleDinner(Point superAppleLocation) {
        return decoratedSnake.checkSuperAppleDinner(superAppleLocation);
    }

    @Override
    public boolean detectDeath() {
        return decoratedSnake.detectDeath();
    }

    @Override
    public int removeCollidedSegments(PointF sharkPoint, int sharkWidth, int sharkHeight){
        return decoratedSnake.removeCollidedSegments(sharkPoint, sharkWidth, sharkHeight);
    }

    @Override
    public void reduceLength(int lengthToRemove) {
        decoratedSnake.reduceLength(lengthToRemove);
    }

    @Override
    public boolean checkCollisionWithHead(PointF sharkPoint, int sharkWidth, int sharkHeight) {
        return decoratedSnake.checkCollisionWithHead(sharkPoint, sharkWidth, sharkHeight);
    }

    @Override
    public void reset(int w, int h) {
        decoratedSnake.reset(w, h);
    }

    @Override
    public int getSegmentCount() {
        return decoratedSnake.getSegmentCount();
    }

    @Override
    public void removeSegments(int count) {
        decoratedSnake.removeSegments(count);
    }

    @Override
    public void updateHeading(MoveCollide.Heading newHeading) {
        decoratedSnake.updateHeading(newHeading);  // Delegate to the decorated snake
    }
}