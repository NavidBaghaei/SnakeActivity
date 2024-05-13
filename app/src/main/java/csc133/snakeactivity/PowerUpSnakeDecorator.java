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
        this.powerUpEndTime = active ? System.currentTimeMillis() + duration : 0;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        Snake decoratedSnake = (Snake) getDecorated();
        updateSnakeHeading(decoratedSnake);

        List<Point> segments = decoratedSnake.getSegments();
        if (segments.isEmpty()) return;

        int color = Color.argb(128, 255, 215, 0);
        if (powerUpActive && System.currentTimeMillis() < powerUpEndTime) {
            applyColorFilter(color);
        } else {
            removeColorFilter();
        }

        for (int i = 0; i < segments.size(); i++) {
            Point currentSegment = segments.get(i);
            Drawable drawable;
            if (i == 0) {
                drawable = headDrawable;
            } else {
                drawable = bodyDrawable;
            }
            drawDrawable(canvas, drawable, currentSegment, decoratedSnake.getHeading());
        }
    }

    private void updateSnakeHeading(Snake decoratedSnake) {
        if (powerUpActive && System.currentTimeMillis() < powerUpEndTime) {
            decoratedSnake.updateHeading(decoratedSnake.getHeading());
        }
    }

    private void applyColorFilter(int color) {
        headDrawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        bodyDrawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
    }

    private void removeColorFilter() {
        headDrawable.setColorFilter(null);
        bodyDrawable.setColorFilter(null);
    }

    private void drawDrawable(Canvas canvas, Drawable drawable, Point position, Snake.Heading heading) {
        if (drawable != null) {
            int left = position.x * segmentSize;
            int top = position.y * segmentSize;
            int right = left + segmentSize;
            int bottom = top + segmentSize;
            drawable.setBounds(left, top, right, bottom);

            switch (heading) {
                case UP:
                    drawable.draw(canvas);
                    break;
                case DOWN:
                    canvas.save();
                    canvas.rotate(180, left + segmentSize / 2, top + segmentSize / 2);
                    drawable.draw(canvas);
                    canvas.restore();
                    break;
                case LEFT:
                    canvas.save();
                    canvas.scale(-1, 1, left + segmentSize / 2, top + segmentSize / 2);
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