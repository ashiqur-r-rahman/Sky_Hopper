package com.example.bouncinggame;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameView extends View {
    private Paint paint;
    private CopyOnWriteArrayList<MainActivity.Ball> balls;
    private MainActivity mainActivity;
    private Bitmap backgroundBitmap;
    private AnimatedImageDrawable holeDrawable; // Replace holeBitmap with AnimatedImageDrawable
    private Path clipPath;

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        this.mainActivity = (MainActivity) context;
        paint = new Paint();
        clipPath = new Path();
        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.background);

        // Load the GIF as AnimatedImageDrawable
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                holeDrawable = (AnimatedImageDrawable) getResources().getDrawable(R.drawable.hole, null);
            }
            // Optionally set to repeat indefinitely
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                holeDrawable.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
            }
        } catch (Exception e) {
            Log.e("GameView", "Failed to load hole.gif", e);
        }
    }

    public void setBalls(CopyOnWriteArrayList<MainActivity.Ball> balls) {
        this.balls = balls;
    }

    private void drawTrajectory(Canvas canvas, float startX, float startY, float vx, float vy) {
        paint.setColor(Color.RED);
        paint.setStrokeWidth(3);
        float currentX = startX;
        float currentY = startY;
        float dirX = vx;
        float dirY = vy;
        int maxReflections = 5;

        // Hole parameters (center + radius)
        float holeCenterX = mainActivity.getHoleX() + 120;
        float holeCenterY = mainActivity.getHoleY() + 120;
        float holeRadius  = 60;

        // Precompute a = |v|^2 for quadratic
        float a = dirX*dirX + dirY*dirY;

        for (int i = 0; i < maxReflections; i++) {
            // 1) compute t to each wall
            float tLeft   = dirX < 0 ? -currentX / dirX : Float.MAX_VALUE;
            float tRight  = dirX > 0 ? (getWidth() - currentX) / dirX : Float.MAX_VALUE;
            float tTop    = dirY < 0 ? -currentY / dirY : Float.MAX_VALUE;
            float tBottom = dirY > 0 ? (getHeight() - currentY) / dirY : Float.MAX_VALUE;
            float tWall   = Math.min(Math.min(tLeft, tRight), Math.min(tTop, tBottom));
            if (tWall == Float.MAX_VALUE) break;

            // 2) solve quadratic for ray–circle intersection:
            //    (current + t·dir − holeCenter)^2 = r^2
            float dx0 = currentX - holeCenterX;
            float dy0 = currentY - holeCenterY;
            float b = 2 * (dirX*dx0 + dirY*dy0);
            float c = dx0*dx0 + dy0*dy0 - holeRadius*holeRadius;
            float disc = b*b - 4*a*c;

            // find smallest positive tHole
            float tHole = Float.MAX_VALUE;
            if (disc >= 0) {
                float sqrtD = (float)Math.sqrt(disc);
                float t1 = (-b - sqrtD) / (2*a);
                float t2 = (-b + sqrtD) / (2*a);
                if (t1 >= 0) tHole = t1;
                else if (t2 >= 0) tHole = t2;
            }

            // 3) if hole hit occurs before any wall, draw only to the hole and stop
            if (tHole <= tWall) {
                float hitX = currentX + dirX * tHole;
                float hitY = currentY + dirY * tHole;
                canvas.drawLine(currentX, currentY, hitX, hitY, paint);
                break;
            }

            // 4) otherwise draw to the wall, then reflect and loop
            float nextX = currentX + dirX * tWall;
            float nextY = currentY + dirY * tWall;
            canvas.drawLine(currentX, currentY, nextX, nextY, paint);

            // if we hit the top, stop (as before)
            if (tWall == tTop) {
                break;
            }

            // update pos & reflect velocity
            currentX = nextX;
            currentY = nextY;
            if (tWall == tLeft || tWall == tRight) {
                dirX = -dirX;
            } else {  // bottom
                dirY = -dirY;
            }
        }
    }


    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background
        if (backgroundBitmap != null && !backgroundBitmap.isRecycled()) {
            canvas.drawBitmap(backgroundBitmap, null, new Rect(0, 0, getWidth(), getHeight()), paint);
        } else {
            canvas.drawColor(Color.GRAY);
        }

        // Draw trajectory if dragging
        if (mainActivity.draggedBall != null) {
            float launchX = mainActivity.draggedBall.initialX + 30;
            float launchY = getHeight() - 30;
            float vx = mainActivity.tempVx;
            float vy = mainActivity.tempVy;
            drawTrajectory(canvas, launchX, launchY, vx, vy);
        }

        // Draw big ball
        paint.setColor(mainActivity.getCurrentColor());
        paint.setShader(null);
        canvas.drawOval(mainActivity.getBigX(), mainActivity.getBigY(),
                mainActivity.getBigX() + mainActivity.getBx(),
                mainActivity.getBigY() + mainActivity.getBx(), paint);

        // Draw small balls
        if (balls != null) {
            Log.d("GameView", "Number of balls: " + balls.size());
            for (MainActivity.Ball ball : balls) {
                if (!ball.stuck) {
                    if (ball.x >= 0 && ball.x + 60 <= getWidth() && ball.y >= 0 && ball.y + 60 <= getHeight()) {
                        Log.d("BallPosition", "Drawing ball at x: " + ball.x + ", y: " + ball.y);
                        paint.setShader(new RadialGradient(
                                ball.x + 30, ball.y + 30,
                                30,
                                Color.parseColor("#FF10F0"),
                                Color.parseColor("#88007B"),
                                Shader.TileMode.CLAMP
                        ));
                        canvas.drawOval(ball.x, ball.y, ball.x + 60, ball.y + 60, paint);
                        paint.setShader(null);
                    } else {
                        Log.w("BallPosition", "Ball out of bounds at x: " + ball.x + ", y: " + ball.y);
                    }
                } else {
                    Log.d("BallPosition", "Skipping stuck ball at x: " + ball.x + ", y: " + ball.y);
                }
            }
        } else {
            Log.e("GameView", "Balls list is null");
        }

        // Draw animated GIF (hole)
        if (holeDrawable != null) {
            canvas.save();
            clipPath.reset();
            clipPath.addCircle(mainActivity.getHoleX() + 120, mainActivity.getHoleY() + 120, 60, Path.Direction.CW);
            canvas.clipPath(clipPath);

            // Set bounds for the drawable
            holeDrawable.setBounds(mainActivity.getHoleX(), mainActivity.getHoleY(),
                    mainActivity.getHoleX() + 240, mainActivity.getHoleY() + 240);
            holeDrawable.draw(canvas);

            canvas.restore();
        } else {
            // Fallback if GIF fails to load
            paint.setColor(Color.BLACK);
            canvas.drawOval(mainActivity.getHoleX(), mainActivity.getHoleY(),
                    mainActivity.getHoleX() + 240, mainActivity.getHoleY() + 240, paint);
        }

        // Draw game over text
        if (mainActivity.isGameOver()) {
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setColor(Color.RED);
            paint.setTextSize(90);
            canvas.drawText("GAME OVER!", 250, 300, paint);
            canvas.drawText("Final Score: " + mainActivity.getScore(), 250, 375, paint);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Start the GIF animation
        if (holeDrawable != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                holeDrawable.start();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Stop the GIF animation and clean up
        if (holeDrawable != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                holeDrawable.stop();
            }
            holeDrawable.setCallback(null); // Prevent memory leaks
            holeDrawable = null;
        }
        // Recycle background bitmap
        if (backgroundBitmap != null && !backgroundBitmap.isRecycled()) {
            backgroundBitmap.recycle();
            backgroundBitmap = null;
        }
    }

    public void recycleBitmaps() {
        // Only recycle background bitmap as holeDrawable handles its own cleanup
        if (backgroundBitmap != null && !backgroundBitmap.isRecycled()) {
            backgroundBitmap.recycle();
            backgroundBitmap = null;
        }
    }
}