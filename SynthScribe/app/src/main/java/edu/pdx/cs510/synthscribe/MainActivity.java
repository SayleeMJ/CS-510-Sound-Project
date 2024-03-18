package edu.pdx.cs510.synthscribe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button newTrackButton = findViewById(R.id.newTrackButton);
        Button myTrackButton = findViewById(R.id.myTracksButton);

        newTrackButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SynthesizerActivity.class);
            startActivity(intent);
        });

        myTrackButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SavedCompositionsActivity.class);
            startActivity(intent);
        });

    }
}

