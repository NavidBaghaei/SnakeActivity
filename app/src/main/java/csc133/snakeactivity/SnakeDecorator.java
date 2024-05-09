package csc133.snakeactivity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.MotionEvent;

public class SnakeDecorator extends Snake {
    protected Snake decoratedSnake;

    public SnakeDecorator(Snake decoratedSnake) {
        super(decoratedSnake.getContext(), decoratedSnake.getMoveRange(), decoratedSnake.getSegmentSize());
        this.decoratedSnake = decoratedSnake;
    }

    @Override
    public void move() {
        decoratedSnake.move();
    }

    @Override
    public boolean checkDinner(Point appleLocation) {
        return decoratedSnake.checkDinner(appleLocation);
    }

    @Override
    public void update() {
        decoratedSnake.update();
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        decoratedSnake.draw(canvas, paint);
    }

    @Override
    public boolean isOccupied(Point location) {
        return decoratedSnake.isOccupied(location);
    }

    @Override
    public void switchHeading(MotionEvent motionEvent) {
        decoratedSnake.switchHeading(motionEvent);
    }

    @Override
    public void reduceLength(int lengthToRemove) {
        decoratedSnake.reduceLength(lengthToRemove);
    }

    @Override
    public int removeCollidedSegments(PointF sharkPoint) {
        return decoratedSnake.removeCollidedSegments(sharkPoint);
    }

    @Override
    public int getSegmentSize() {
        return decoratedSnake.getSegmentSize();
    }

    @Override
    public Point getHeadLocation() {
        return decoratedSnake.getHeadLocation();
    }
}