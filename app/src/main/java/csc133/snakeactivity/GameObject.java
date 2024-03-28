package csc133.snakeactivity;


import android.graphics.Canvas;
import android.graphics.Paint;

public interface GameObject {
    void draw(Canvas canvas, Paint paint);

    void update();
}