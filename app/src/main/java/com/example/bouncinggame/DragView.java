package com.example.bouncinggame;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DragView is a custom view that allows dragging balls and interacts with MainActivity's GameView.
 */
public class DragView extends View {
    private Paint paint;
    private List<MainActivity.Ball> balls;
    private MainActivity mainActivity;

    public DragView(Context context) {
        super(context);
        init();
    }

    public DragView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setMainActivity(MainActivity activity) {
        this.mainActivity = activity;
    }

    private void init() {
        paint = new Paint();
        // Initialize with a thread-safe list to prevent crashes, though it will be replaced.
        balls = new CopyOnWriteArrayList<>();
    }

    public void setBalls(List<MainActivity.Ball> balls) {
        this.balls = balls;
    }


    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (balls == null) return;

        // Use the constant from MainActivity for consistent sizing
        final int diameter = MainActivity.SMALL_BALL_DIAMETER;
        final float radius = diameter / 2.0f;

        for (MainActivity.Ball ball : balls) {
            // Only draw balls that are stuck at the top or are currently being dragged
            if (ball.stuck || ball.beingDragged) {
                paint.setShader(new RadialGradient(
                        ball.x + radius, ball.y + radius, // Center of the gradient
                        radius, // Radius
                        Color.parseColor("#FF10F0"), // Start color: bright pink
                        Color.parseColor("#88007B"),  // End color: darker pink for gradient effect
                        Shader.TileMode.CLAMP
                ));
                // Draw the oval using the correct diameter
                canvas.drawOval(ball.x, ball.y, ball.x + diameter, ball.y + diameter, paint);
                paint.setShader(null);

                // This part draws the trajectory line when dragging
                if (ball.beingDragged) {
                    paint.setColor(Color.RED); // Changed to red for better visibility
                    paint.setStrokeWidth(5);
                    float launchX = ball.initialX + radius;
                    float launchY = 0 + radius;
                    float touchX = ball.x + radius;
                    float touchY = ball.y + radius;
                    canvas.drawLine(launchX, launchY, touchX, touchY, paint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mainActivity == null || balls == null) return false;

        // Use the constant for consistent sizing in touch events
        final int diameter = MainActivity.SMALL_BALL_DIAMETER;
        final float radius = diameter / 2.0f;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Find which ball was touched
                for (MainActivity.Ball ball : balls) {
                    // Check if the touch is within the bounds of a stuck ball
                    if (ball.stuck && !ball.beingDragged &&
                            event.getX() >= ball.x && event.getX() <= ball.x + diameter &&
                            event.getY() >= ball.y && event.getY() <= ball.y + diameter) {

                        ball.beingDragged = true;
                        ball.startDragX = (int) event.getX();
                        ball.startDragY = (int) event.getY();
                        mainActivity.draggedBall = ball; // Notify MainActivity which ball is being dragged
                        invalidate(); // Redraw to show trajectory line
                        return true; // Consume the event
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mainActivity.draggedBall != null) {
                    mainActivity.draggedBall.x = event.getX() - radius;
                    mainActivity.draggedBall.y = event.getY() - radius;

                    // Calculate temporary velocity for trajectory preview
                    float dragX = (mainActivity.draggedBall.startDragX - event.getX()) / 8.0f;
                    float dragY = (mainActivity.draggedBall.startDragY - event.getY()) / 8.0f;
                    mainActivity.tempVx = dragX;
                    mainActivity.tempVy = dragY;

                    invalidate(); // Redraw this view
                    mainActivity.gameView.invalidate(); // Redraw GameView to show the trajectory
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mainActivity.draggedBall != null) {
                    // Finalize the launch velocity
                    float finalVx = (mainActivity.draggedBall.startDragX - event.getX()) / 8.0f;
                    float finalVy = (mainActivity.draggedBall.startDragY - event.getY()) / 8.0f;

                    // Apply the velocity to the ball
                    mainActivity.draggedBall.vx = finalVx;
                    mainActivity.draggedBall.vy = finalVy;

                    // The ball is no longer stuck or being dragged
                    mainActivity.draggedBall.beingDragged = false;
                    mainActivity.draggedBall.stuck = false;

                    // Set its starting position to the bottom of the GameView for launching
                    mainActivity.draggedBall.x = mainActivity.draggedBall.initialX;
                    mainActivity.draggedBall.y = mainActivity.getGameViewHeight() - diameter;

                    // Clear the dragged ball reference
                    mainActivity.draggedBall = null;
                    mainActivity.tempVx = 0;
                    mainActivity.tempVy = 0;

                    invalidate(); // Redraw this view
                    mainActivity.gameView.invalidate(); // Redraw GameView to hide trajectory and show moving ball
                    return true;
                }
                break;
        }
        return true;
    }
}