package csc133.snakeactivity;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Canvas;

public class PowerUpSnakeDecorator extends SnakeDecorator {
    private boolean isPowerUpActive;

    public PowerUpSnakeDecorator(ISnake snake) {
        super(snake);
    }

    public void setPowerUpActive(boolean active) {
        this.isPowerUpActive = active;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (isPowerUpActive) {
            paint.setColor(Color.GREEN); // Change to any power-up color
        }
        super.draw(canvas, paint);
        paint.setColor(Color.WHITE); // Reset to default after drawing
    }
}

