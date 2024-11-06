package com.example.carbonfootprintcalculator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class StartHomeActivity extends AppCompatActivity {

    private Button loginButton, signUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_home);

        // Initialize DummyDataInserter to insert data only once
        DummyDataInserter dataInserter = new DummyDataInserter(this);
        dataInserter.insertDummyData();

        // Initialize buttons
        loginButton = findViewById(R.id.startHome_login);
        signUpButton = findViewById(R.id.startHome_signUp);

        // Set click listener for login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Login Activity
                Intent intent = new Intent(StartHomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // Set click listener for sign-up button
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Registration Activity
                Intent intent = new Intent(StartHomeActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}
