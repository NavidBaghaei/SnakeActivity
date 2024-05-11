package csc133.snakeactivity;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Canvas;

public class PowerUpSnakeDecorator extends SnakeDecorator {
    private boolean isPowerUpActive;
    private int originalColor;
    private long powerUpEndTime;

    public PowerUpSnakeDecorator(ISnake snake) {
        super(snake);
        this.originalColor = Color.WHITE; // Assuming the default color is white
        this.isPowerUpActive = false;
        this.powerUpEndTime = 0;
    }

    public synchronized void setPowerUpActive(boolean active, long duration) {
        this.isPowerUpActive = active;
        if (active) {
            this.powerUpEndTime = System.currentTimeMillis() + duration;
        } else {
            this.powerUpEndTime = 0;
        }
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (isPowerUpActive && System.currentTimeMillis() < powerUpEndTime) {
            originalColor = paint.getColor(); // Store original color
            paint.setColor(Color.GREEN); // Change to power-up color
        }
        super.draw(canvas, paint);
        if (isPowerUpActive && System.currentTimeMillis() >= powerUpEndTime) {
            paint.setColor(originalColor); // Restore original color
            isPowerUpActive = false; // Reset power-up state
        }
    }
}