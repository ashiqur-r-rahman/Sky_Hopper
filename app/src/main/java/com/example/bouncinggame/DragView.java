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

/**
 * DragView is a custom view that allows dragging balls and interacts with MainActivity's GameView.
 */
public class DragView extends View {
    private Paint paint;
    private List<MainActivity.Ball> balls; // Changed to List<Ball>
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
        balls = new java.util.ArrayList<>(); // Initialize to avoid null pointer
    }

    public void setBalls(List<MainActivity.Ball> balls) {
        this.balls = balls;
    }


    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (MainActivity.Ball ball : balls) {
            if (ball.stuck || ball.beingDragged) {
                paint.setShader(new RadialGradient(
                        ball.x + 30, ball.y + 30, // Center of the gradient
                        30, // Radius
                        Color.parseColor("#FF10F0"), // Start color: dark pink
                        Color.parseColor("#88007B"),  // End color: darker pink for gradient effect
                        Shader.TileMode.CLAMP
                ));
                canvas.drawOval(ball.x, ball.y, ball.x + 60, ball.y + 60, paint);
                paint.setShader(null);

                if (ball.beingDragged) {
                    paint.setColor(Color.BLACK);
                    float launchX = ball.initialX + 30;
                    float launchY = 0 + 30; // Corrected: use initialY, not bottom of screen
                    float touchX = ball.x + 30;
                    float touchY = ball.y + 30;
                    canvas.drawLine(launchX, launchY, touchX, touchY, paint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                for (MainActivity.Ball ball : balls) {
                    if (ball.stuck && !ball.beingDragged &&
                            event.getX() >= ball.x && event.getX() <= ball.x + 60 &&
                            event.getY() >= ball.y && event.getY() <= ball.y + 60) {
                        ball.beingDragged = true;
                        ball.startDragX = (int) event.getX();
                        ball.startDragY = (int) event.getY();
                        mainActivity.draggedBall = ball;
                        Log.d("DragView", "Started dragging ball at x: " + ball.x + ", y: " + ball.y);
                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                for (MainActivity.Ball ball : balls) {
                    if (ball.beingDragged) {
                        ball.x = event.getX() - 30;
                        ball.y = event.getY() - 30;
                        float dragX = (ball.startDragX - event.getX()) / 8.0f;
                        float dragY = (ball.startDragY - event.getY()) / 8.0f;
                        mainActivity.tempVx = dragX;
                        mainActivity.tempVy = dragY;
                        Log.d("DragView", "Dragging ball to x: " + ball.x + ", y: " + ball.y);
                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                for (MainActivity.Ball ball : balls) {
                    if (ball.beingDragged) {
                        ball.beingDragged = false;
                        ball.stuck = false;
                        float dragX = (ball.startDragX - event.getX()) / 8.0f;
                        float dragY = (ball.startDragY - event.getY()) / 8.0f;
                        ball.vx = dragX;
                        ball.vy = dragY;
                        ball.x = ball.initialX;
                        ball.y = mainActivity.getGameViewHeight() - 60;
                        mainActivity.draggedBall = null;
                        Log.d("DragView", "Released ball at x: " + ball.x + ", y: " + ball.y + ", vx: " + ball.vx + ", vy: " + ball.vy);
                        invalidate();
                    }
                }
                break;
        }
        return true;
    }
}