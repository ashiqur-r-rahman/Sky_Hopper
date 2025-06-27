package com.example.bouncinggame;

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends AppCompatActivity {
    // --- GAMEPLAY CONSTANTS ---
    public static final int SMALL_BALL_DIAMETER = 80;
    private static final int INITIAL_BIG_BALL_SIZE = 50;
    private final int MAX_BALLS = 10;

    private SoundPool soundPool;
    private int soundBounce, soundDrop;
    private MediaPlayer bgMusic;

    private int score = 0;
    private boolean gameOver = false;
    private int bigX = 500;
    private int bigY = 400;
    private int bx = INITIAL_BIG_BALL_SIZE;
    private int vx = 3;
    private int vy = 4;
    private int currentColor = android.graphics.Color.WHITE;
    private int holeX, holeY;
    private static final int HOLE_SIZE = 120;
    private final CopyOnWriteArrayList<Ball> balls = new CopyOnWriteArrayList<>();
    GameView gameView;
    private DragView dragView;
    private TextView scoreLabel;
    public Ball draggedBall;
    public float tempVx, tempVy;

    public static class Ball {
        float x, y;
        float vx, vy;
        int color;
        boolean stuck;
        boolean beingDragged;
        int startDragX, startDragY;
        int initialX;

        Ball(int x, int y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.vx = 0;
            this.vy = 0;
            this.initialX = x;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(attrs)
                .build();
        soundBounce = soundPool.load(this, R.raw.bounce, 1);
        soundDrop = soundPool.load(this, R.raw.drop, 1);
        bgMusic = MediaPlayer.create(this, R.raw.background);
        bgMusic.setLooping(true);
        bgMusic.setVolume(1.0f, 1.0f);
        bgMusic.start();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameView = findViewById(R.id.gameView);
        dragView = findViewById(R.id.dragView);
        scoreLabel = findViewById(R.id.scoreLabel);

        dragView.setMainActivity(this);
        dragView.setBalls(balls);
        gameView.setBalls(balls);

        gameView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                gameView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                resetHole();
                startGameLoop();
            }
        });

        Button restartButton = findViewById(R.id.restartButton);
        Button pauseButton = findViewById(R.id.pauseButton);
        restartButton.setOnClickListener(v -> resetGame());
        pauseButton.setOnClickListener(v -> togglePause());
    }

    private void resetGame() {
        score = 0;
        balls.clear();
        gameOver = false;
        bx = INITIAL_BIG_BALL_SIZE; // Reset big ball size on restart
        resetHole();
        gameView.invalidate();
        dragView.invalidate();
    }

    private boolean isPaused = false;
    private void togglePause() {
        // CHANGE: This now ONLY toggles the pause state. It no longer resets the ball size.
        isPaused = !isPaused;
    }

    private void startGameLoop() {
        new Timer().schedule(new TimerTask() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                if (!gameOver && !isPaused) {
                    updateGame();
                    runOnUiThread(() -> {
                        scoreLabel.setText("Score: " + score);
                        gameView.invalidate();
                        dragView.invalidate();
                    });
                }
            }
        }, 0, 20);
    }

    private void updateGame() {
        bigX += vx;
        bigY += vy;
        if (bigX < 0) {
            bx += 7; bigX = 0; vx = (int) (-vx * 1.1f); spawnNewBall();
        } else if (bigX > gameView.getWidth() - bx) {
            bx += 7; bigX = gameView.getWidth() - bx; vx = (int) (-vx * 1.1f); spawnNewBall();
        }
        if (bigY < 0) {
            bx += 7; bigY = 0; vy = (int) (-vy * 1.1f); spawnNewBall();
        } else if (bigY > gameView.getHeight() - bx) {
            bx += 7; bigY = gameView.getHeight() - bx; vy = (int) (-vy * 1.1f); spawnNewBall();
        }

        for (Ball ball : balls) {
            if (ball.stuck || ball.beingDragged) continue;

            ball.x += ball.vx;
            ball.y += ball.vy;

            if (ball.y < 0) {
                score -= 5;
                balls.remove(ball);
                continue;
            }

            if (ball.x < 0 || ball.x > gameView.getWidth() - SMALL_BALL_DIAMETER) {
                ball.vx = -ball.vx;
            }

            float bigBallCenterX = bigX + bx / 2.0f;
            float bigBallCenterY = bigY + bx / 2.0f;
            float bigBallRadius = bx / 2.0f;
            float smallBallRadius = SMALL_BALL_DIAMETER / 2.0f;
            float smallBallCenterX = ball.x + smallBallRadius;
            float smallBallCenterY = ball.y + smallBallRadius;

            float dx = smallBallCenterX - bigBallCenterX;
            float dy = smallBallCenterY - bigBallCenterY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            float sumOfRadii = smallBallRadius + bigBallRadius;

            if (distance < sumOfRadii) {
                soundPool.play(soundBounce, 0.35f, 0.35f, 0, 0, 1f);
                float normalX = dx / distance;
                float normalY = dy / distance;
                float dotProduct = ball.vx * normalX + ball.vy * normalY;
                ball.vx -= 2 * dotProduct * normalX;
                ball.vy -= 2 * dotProduct * normalY;
                float overlap = sumOfRadii - distance;
                ball.x += overlap * normalX;
                ball.y += overlap * normalY;
            }

            if (ball.y > gameView.getHeight()) {
                ball.y = 10;
                ball.vy = 0;
                ball.stuck = true;
            }

            if (checkHoleCollision(ball)) {
                balls.remove(ball);
                soundPool.play(soundDrop, 0.75f, 0.75f, 0, 0, 1f);
                score += 10;
                resetHole();
            }
        }

        if (balls.size() >= MAX_BALLS) gameOver = true;
    }

    public int getGameViewHeight() {
        return gameView.getHeight();
    }

    private void spawnNewBall() {
        if (balls.size() < MAX_BALLS) {
            int startX = new Random().nextInt(dragView.getWidth() - 20);
            int startY = 5;
            Ball newBall = new Ball(startX, startY, android.graphics.Color.WHITE);
            newBall.stuck = true;
            balls.add(newBall);
            gameView.invalidate();
            currentColor = android.graphics.Color.rgb(
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256)
            );
        }
    }

    private void resetHole() {
        int width = gameView.getWidth();
        int height = gameView.getHeight();
        if (width > HOLE_SIZE && height > HOLE_SIZE) {
            holeX = new Random().nextInt(width - HOLE_SIZE);
            holeY = new Random().nextInt(height - HOLE_SIZE);
        } else {
            Log.e("MainActivity", "GameView dimensions are not set properly");
            holeX = 0;
            holeY = 0;
        }
    }

    private boolean checkHoleCollision(Ball ball) {
        return ball.x + SMALL_BALL_DIAMETER > holeX && ball.x < holeX + HOLE_SIZE &&
                ball.y + SMALL_BALL_DIAMETER > holeY && ball.y < holeY + HOLE_SIZE;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bgMusic != null) {
            bgMusic.stop();
            bgMusic.release();
            bgMusic = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    public int getBigX() { return bigX; }
    public int getBigY() { return bigY; }
    public int getBx() { return bx; }
    public int getCurrentColor() { return currentColor; }
    public int getHoleX() { return holeX; }
    public int getHoleY() { return holeY; }
    public boolean isGameOver() { return gameOver; }
    public int getScore() { return score; }
}