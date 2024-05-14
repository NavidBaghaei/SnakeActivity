package csc133.snakeactivity;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.KeyEvent;

// SnakeActivity class represents the main activity of the game
public class SnakeActivity extends Activity {

    // Declare an instance of SnakeGame
    SnakeGame mSnakeGame;

    // Set up the game
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the status bar and navigation bar in fullscreen mode
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        // Get the pixel dimensions of the screen
        Display display = getWindowManager().getDefaultDisplay();

        // Initialize the result into a Point object
        Point size = new Point();
        display.getSize(size);

        // Create a new instance of the SnakeGame class
        mSnakeGame = new SnakeGame(this, size);

        // Make snakeEngine the view of the Activity
        setContentView(mSnakeGame);
    }

    // Start the thread in snakeEngine when the activity resumes
    @Override
    protected void onResume() {
        super.onResume();
        mSnakeGame.resume();
    }

    // Stop the thread in snakeEngine when the activity pauses
    @Override
    protected void onPause() {
        super.onPause();
        mSnakeGame.pause();
    }

    // Handle key events to control snake movement
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                // Update snake heading when up arrow key is pressed
                mSnakeGame.getSnake().updateHeading(Snake.Heading.UP);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                // Update snake heading when down arrow key is pressed
                mSnakeGame.getSnake().updateHeading(Snake.Heading.DOWN);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                // Update snake heading when left arrow key is pressed
                mSnakeGame.getSnake().updateHeading(Snake.Heading.LEFT);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // Update snake heading when right arrow key is pressed
                mSnakeGame.getSnake().updateHeading(Snake.Heading.RIGHT);
                return true;
            case KeyEvent.KEYCODE_SPACE:
                // Handle space key if needed
        }
        return super.onKeyDown(keyCode, event);
    }
}