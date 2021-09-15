package com.kpu410.realbike;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class InfoActivity extends AppCompatActivity {

    TextView infoID = findViewById(R.id.infoID);
    TextView infoName = findViewById(R.id.infoName);
    TextView infoEmail = findViewById(R.id.infoEmail);
    TextView infoDistance = findViewById(R.id.infoDistance);
    TextView infoTime = findViewById(R.id.infoTime);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

    }
}