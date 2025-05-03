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
    private SoundPool soundPool;
    private int soundBounce, soundDrop;
    private MediaPlayer bgMusic;

    private final int MAX_BALLS=10 ;
    private int score = 0;
    private boolean gameOver = false;
    private int bx = 50;
    private int bigX = 500;
    private int bigY = 400;
    private int vx = 3;
    private int vy = 4;
    private int currentColor = android.graphics.Color.WHITE;
    private int holeX, holeY;
    private static final int HOLE_SIZE = 120;
    private final CopyOnWriteArrayList<Ball> balls = new CopyOnWriteArrayList<>();
    private GameView gameView;
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
        //–– SoundPool for short effects ––
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(attrs)
                .build();
        soundBounce = soundPool.load(this, R.raw.bounce, 1);
        soundDrop   = soundPool.load(this, R.raw.drop,   1);

//–– MediaPlayer for looping background ––
        bgMusic = MediaPlayer.create(this, R.raw.background);
        bgMusic.setLooping(true);
        bgMusic.setVolume(1.0f, 1.0f);  // tweak as you like
        bgMusic.start();

        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
        } catch (Exception e) {
            Log.e("MainActivity", "Error inflating layout", e);
            finish();
            return;
        }

        gameView = findViewById(R.id.gameView);
        dragView = findViewById(R.id.dragView);
        scoreLabel = findViewById(R.id.scoreLabel);

        if (gameView == null || dragView == null) {
            Log.e("MainActivity", "View initialization failed");
            finish();
            return;
        }

        dragView.setMainActivity(this);
        dragView.setBalls(balls);
        gameView.setBalls(balls);

        gameView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                gameView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                resetHole();  // Call resetHole after layout is complete
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
        resetHole();
        gameView.invalidate();
        dragView.invalidate();
    }

    private boolean isPaused = false;
    private void togglePause() {
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

        // Handle collision with left wall
        if (bigX < 0) {
            bx += 7;                  // Increase size first
            bigX = 0;                 // Set to left boundary (fixed at 0)
            vx = (int) (-vx * 1.1f);
           // soundPool.play(soundBounce, 0.35f, 0.35f, 0, 0, 1f);// Reverse and increase velocity
            spawnNewBall();           // Spawn a small ball
        }
        // Handle collision with right wall
        else if (bigX > gameView.getWidth() - bx) {
            bx += 7;                  // Increase size first
            bigX = gameView.getWidth() - bx;
           // soundPool.play(soundBounce, 0.35f, 0.35f, 0, 0, 1f);// Set to new right boundary
            vx = (int) (-vx * 1.1f);  // Reverse and increase velocity
            spawnNewBall();           // Spawn a small ball
        }

        // Handle collision with top wall
        if (bigY < 0) {
            bx += 7;                  // Increase size first
            bigY = 0;                 // Set to top boundary (fixed at 0)
            vy = (int) (-vy * 1.1f);
            //soundPool.play(soundBounce, 0.35f, 0.35f, 0, 0, 1f);// Reverse and increase velocity
            spawnNewBall();           // Spawn a small ball
        }
        // Handle collision with bottom wall
        else if (bigY > gameView.getHeight() - bx) {
            bx += 7;                  // Increase size first
            bigY = gameView.getHeight() - bx;  // Set to new bottom boundary
            vy = (int) (-vy * 1.1f);
           // soundPool.play(soundBounce, 0.35f, 0.35f, 0, 0, 1f);// Reverse and increase velocity
            spawnNewBall();           // Spawn a small ball
        }

        // Update small balls (unchanged logic)
        for (Ball ball : balls) {
            if (!ball.stuck && !ball.beingDragged) {
                ball.x += ball.vx;
                ball.y += ball.vy;

                if (ball.x < 0 || ball.x > gameView.getWidth() - 20) {
                    ball.vx = -ball.vx;
                }

                if (checkCollision((int) ball.x, (int) ball.y, bigX, bigY)) {
                    ball.vx = -ball.vx;
                    ball.vy = -ball.vy;
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

    private boolean checkCollision(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy) < 50;
    }

    private boolean checkHoleCollision(Ball ball) {
        return ball.x + 60 > holeX && ball.x < holeX + HOLE_SIZE &&
                ball.y + 60 > holeY && ball.y < holeY + HOLE_SIZE;
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