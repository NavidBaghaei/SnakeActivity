package csc133.snakeactivity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.graphics.Point;
import android.graphics.PointF;

public class InvincibleSnake extends SnakeDecorator {
    private boolean isInvincible;
    private long invincibleEndTime;

    public InvincibleSnake(Snake decoratedSnake) {
        super(decoratedSnake);
        makeInvincible();
    }

    private void makeInvincible() {
        isInvincible = true;
        // Set invincible for 5 seconds
        long invincibilityDuration = 5000;
        invincibleEndTime = System.currentTimeMillis() + invincibilityDuration;
        // Use a Handler to turn off invincibility after the duration
        new Handler().postDelayed(() -> isInvincible = false, invincibilityDuration);
    }

    @Override
    public boolean checkCollisionWithHead(PointF sharkPoint, int sharkWidth, int sharkHeight) {
        if (!isInvincible) {
            return super.checkCollisionWithHead(sharkPoint, sharkWidth, sharkHeight);
        }
        return false;  // Ignore collisions when invincible
    }


    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (isInvincible && System.currentTimeMillis() < invincibleEndTime) {
            paint.setColor(Color.YELLOW);  // Change color to indicate invincibility
        } else {
            paint.setColor(Color.WHITE);  // Reset color after invincibility ends
        }
        super.draw(canvas, paint);  // Continue drawing the snake normally
    }

    // Ensure the invincibility flag can be accessed or modified if necessary
    public boolean isInvincible() {
        return isInvincible;
    }

    public void setInvincible(boolean invincible) {
        isInvincible = invincible;
    }
}
