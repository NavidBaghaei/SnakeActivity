package csc133.snakeactivity;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.List;

public class PowerUpSnakeDecorator extends SnakeDecorator {
    private boolean powerUpActive;
    private long powerUpEndTime;
    private Drawable headDrawable;
    private Drawable bodyDrawable;
    private int segmentSize;

    public PowerUpSnakeDecorator(ISnake snake, Drawable headDrawable, Drawable bodyDrawable, int segmentSize) {
        super(snake);
        this.powerUpActive = false;
        this.powerUpEndTime = 0;
        this.headDrawable = headDrawable;
        this.bodyDrawable = bodyDrawable;
        this.segmentSize = segmentSize;
    }

    public void setPowerUpActive(boolean active, long duration) {
        this.powerUpActive = active;
        if (active) {
            this.powerUpEndTime = System.currentTimeMillis() + duration;
        } else {
            this.powerUpEndTime = 0;
        }
    }




    @Override
    public void draw(Canvas canvas, Paint paint) {
        // Get the decorated snake object
        Snake decoratedSnake = (Snake) getDecorated();

        // Get the segments of the snake
        List<Point> segments = decoratedSnake.getSegments();

        // Ensure that segments list is not empty
        if (!segments.isEmpty()) {
            // Calculate the color with reduced opacity (lower alpha value)
            int color = Color.argb(128, 255, 215, 0); // Set alpha to 128 (50% opacity)

            // Apply color filter with reduced opacity if power-up is active
            if (powerUpActive && System.currentTimeMillis() < powerUpEndTime) {
                headDrawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
                bodyDrawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
            } else {
                headDrawable.setColorFilter(null);
                bodyDrawable.setColorFilter(null);
            }

            // Draw the head of the snake
            drawDrawable(canvas, headDrawable, segments.get(0));

            // Draw the body segments of the snake
            for (int i = 1; i < segments.size(); i++) {
                drawDrawable(canvas, bodyDrawable, segments.get(i));
            }
        }
    }


    // Helper method to draw a drawable on the canvas at a specific position
    private void drawDrawable(Canvas canvas, Drawable drawable, Point position) {
        int left = position.x * segmentSize;
        int top = position.y * segmentSize;
        int right = left + segmentSize;
        int bottom = top + segmentSize;
        drawable.setBounds(left, top, right, bottom);
        drawable.draw(canvas);
    }


}