package com.example.bouncinggame;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Find the start button and set click listener
        findViewById(R.id.start_button).setOnClickListener(v -> {
            // Start MainActivity
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            // Optional: Finish WelcomeActivity to prevent going back to it
            finish();
        });
    }
}