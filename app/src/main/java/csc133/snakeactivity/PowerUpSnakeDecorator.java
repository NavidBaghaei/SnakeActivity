package csc133.snakeactivity;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.List;

public class PowerUpSnakeDecorator extends SnakeDecorator {
    // Flag to indicate if power-up is active
    private boolean powerUpActive;
    // Time when the power-up will end
    private long powerUpEndTime;
    // Drawable for the snake head
    private final Drawable headDrawable;
    // Drawable for the snake body
    private final Drawable bodyDrawable;
    // Size of each segment
    private final int segmentSize;

    // Constructor
    public PowerUpSnakeDecorator(ISnake snake, Drawable headDrawable, Drawable bodyDrawable, int segmentSize) {
        super(snake);
        this.powerUpActive = false;
        this.powerUpEndTime = 0;
        this.headDrawable = headDrawable;
        this.bodyDrawable = bodyDrawable;
        this.segmentSize = segmentSize;
    }

    // Method to set the power-up state and duration
    public void setPowerUpActive(boolean active, long duration) {
        this.powerUpActive = active;
        this.powerUpEndTime = active ? System.currentTimeMillis() + duration : 0;
    }

    // Override method to draw the snake with power-up effect
    @Override
    public void draw(Canvas canvas, Paint paint) {
        Snake decoratedSnake = (Snake) getDecorated();
        updateSnakeHeading(decoratedSnake);

        List<Point> segments = decoratedSnake.getSegments();
        if (segments.isEmpty()) return;

        int color = Color.argb(128, 255, 215, 0); // Yellow color for power-up effect
        if (powerUpActive && System.currentTimeMillis() < powerUpEndTime) {
            applyColorFilter(color); // Apply color filter for power-up effect
        } else {
            removeColorFilter(); // Remove color filter if power-up effect is not active
        }

        // Draw snake segments
        for (int i = 0; i < segments.size(); i++) {
            Point currentSegment = segments.get(i);
            Drawable drawable;
            if (i == 0) {
                drawable = headDrawable; // Use head drawable for the snake head
            } else {
                drawable = bodyDrawable; // Use body drawable for the snake body segments
            }
            drawDrawable(canvas, drawable, currentSegment, decoratedSnake.getHeading());
        }
    }

    // Method to update snake heading if power-up is active
    private void updateSnakeHeading(Snake decoratedSnake) {
        if (powerUpActive && System.currentTimeMillis() < powerUpEndTime) {
            decoratedSnake.updateHeading(decoratedSnake.getHeading());
        }
    }

    // Method to apply color filter for power-up effect
    private void applyColorFilter(int color) {
        headDrawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        bodyDrawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
    }

    // Method to remove color filter
    private void removeColorFilter() {
        headDrawable.setColorFilter(null);
        bodyDrawable.setColorFilter(null);
    }

    // Method to draw the drawable on the canvas with appropriate rotation
    private void drawDrawable(Canvas canvas, Drawable drawable, Point position, Snake.Heading heading) {
        if (drawable != null) {
            int left = position.x * segmentSize;
            int top = position.y * segmentSize;
            int right = left + segmentSize;
            int bottom = top + segmentSize;
            drawable.setBounds(left, top, right, bottom);

            // Rotate drawable based on snake heading
            switch (heading) {
                case UP:
                    drawable.draw(canvas);
                    break;
                case DOWN:
                    canvas.save();
                    canvas.rotate(180, left + (float) segmentSize / 2, top + (float) segmentSize / 2);
                    drawable.draw(canvas);
                    canvas.restore();
                    break;
                case LEFT:
                    canvas.save();
                    canvas.scale(-1, 1, left + (float) segmentSize / 2, top + (float) segmentSize / 2);
                    drawable.draw(canvas);
                    canvas.restore();
                    break;
                case RIGHT:
                    drawable.draw(canvas);
                    break;
            }
        }
    }
}