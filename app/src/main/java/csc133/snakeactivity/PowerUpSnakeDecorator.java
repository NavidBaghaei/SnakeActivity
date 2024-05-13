package csc133.snakeactivity;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.List;
import csc133.snakeactivity.Snake; // Import the Heading enum from the Snake class

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

        // Update the snake's heading if the power-up is active
        if (powerUpActive && System.currentTimeMillis() < powerUpEndTime) {
            decoratedSnake.updateHeading(decoratedSnake.getHeading()); // Keep the same heading
        }

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

            // Draw the head of the snake based on its heading
            drawHead(canvas, segments.get(0), decoratedSnake.getHeading());

            // Draw the body segments of the snake
            for (int i = 1; i < segments.size(); i++) {
                // Determine the direction of the body segment relative to the previous segment
                Point currentSegment = segments.get(i);
                Point previousSegment = segments.get(i - 1);
                Snake.Heading bodyHeading = determineBodyHeading(previousSegment, currentSegment);

                // Draw the body segment based on the determined direction
                drawBodySegment(canvas, bodyDrawable, currentSegment, bodyHeading);
            }
        }
    }

    // Helper method to draw the snake's head based on its heading
    private void drawHead(Canvas canvas, Point position, Snake.Heading heading) {
        switch (heading) {
            case UP:
                drawDrawable(canvas, headDrawable, position);
                break;
            case DOWN:
                // Rotate the head image by 180 degrees for the DOWN direction
                canvas.save();
                canvas.rotate(180, position.x * segmentSize + segmentSize / 2, position.y * segmentSize + segmentSize / 2);
                drawDrawable(canvas, headDrawable, position);
                canvas.restore();
                break;
            case LEFT:
                // Flip the head image horizontally for the LEFT direction
                canvas.save();
                canvas.scale(-1, 1, position.x * segmentSize + segmentSize / 2, position.y * segmentSize + segmentSize / 2);
                drawDrawable(canvas, headDrawable, position);
                canvas.restore();
                break;
            case RIGHT:
                // Draw the head image as is for the RIGHT direction
                drawDrawable(canvas, headDrawable, position);
                break;
        }
    }

    private void drawBodySegment(Canvas canvas, Drawable drawable, Point position, Snake.Heading heading) {
        // Get the decorated snake object
        Snake decoratedSnake = (Snake) getDecorated();
        List<Point> segments = decoratedSnake.getSegments();

        switch (heading) {
            case UP:
                // Check the previous segment's position to determine the correct orientation
                Point previousPositionUp = new Point(position.x, position.y + 1);
                if (segments.contains(previousPositionUp)) {
                    // Draw the body segment facing down
                    canvas.save();
                    canvas.rotate(180, position.x * segmentSize + segmentSize / 2, position.y * segmentSize + segmentSize / 2);
                    drawDrawable(canvas, drawable, position);
                    canvas.restore();
                } else {
                    drawDrawable(canvas, drawable, position);
                }
                break;
            case DOWN:
                // Check the previous segment's position to determine the correct orientation
                Point previousPositionDown = new Point(position.x, position.y - 1);
                if (segments.contains(previousPositionDown)) {
                    // Draw the body segment facing up
                    drawDrawable(canvas, drawable, position);
                } else {
                    canvas.save();
                    canvas.rotate(180, position.x * segmentSize + segmentSize / 2, position.y * segmentSize + segmentSize / 2);
                    drawDrawable(canvas, drawable, position);
                    canvas.restore();
                }
                break;
            case LEFT:
                // Check the previous segment's position to determine the correct orientation
                Point previousPositionLeft = new Point(position.x - 1, position.y);
                if (segments.contains(previousPositionLeft)) {
                    // Draw the body segment facing left (flipped horizontally)
                    drawDrawable(canvas, drawable, position);
                } else {
                    canvas.save();
                    canvas.scale(-1, 1, position.x * segmentSize + segmentSize / 2, position.y * segmentSize + segmentSize / 2);
                    drawDrawable(canvas, drawable, position);
                    canvas.restore();
                }
                break;
            case RIGHT:
                // Check the previous segment's position to determine the correct orientation
                Point previousPositionRight = new Point(position.x - 1, position.y);
                if (segments.contains(previousPositionRight)) {
                    // Draw the body segment facing left (flipped horizontally)
                    canvas.save();
                    canvas.scale(-1, 1, position.x * segmentSize + segmentSize / 2, position.y * segmentSize + segmentSize / 2);
                    drawDrawable(canvas, drawable, position);
                    canvas.restore();
                } else {
                    drawDrawable(canvas, drawable, position);
                }
                break;
        }
    }

    // Helper method to determine the heading of a body segment based on its position relative to the previous segment
    private Snake.Heading determineBodyHeading(Point previousSegment, Point currentSegment) {
        if (currentSegment.x > previousSegment.x) {
            return Snake.Heading.RIGHT;
        } else if (currentSegment.x < previousSegment.x) {
            return Snake.Heading.LEFT;
        } else if (currentSegment.y > previousSegment.y) {
            return Snake.Heading.DOWN;
        } else {
            return Snake.Heading.UP;
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