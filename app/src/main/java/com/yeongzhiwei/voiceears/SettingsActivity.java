package com.yeongzhiwei.voiceears;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {
    private String api_key;
    private String region;

    // UI
    private EditText apikeyEditText;
    private EditText regionEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeViews();
        retrieveIntentData();
        refreshLayout();
    }

    private void initializeViews() {
        apikeyEditText = (EditText) findViewById(R.id.editText_key);
        regionEditText = (EditText) findViewById(R.id.editText_region);
    }

    private void retrieveIntentData() {
        Intent intent = getIntent();
        api_key = intent.getStringExtra(MainActivity.EXTRA_API_KEY);
        region = intent.getStringExtra(MainActivity.EXTRA_REGION);
    }

    private void refreshLayout() {
        if (api_key != null) {
            apikeyEditText.setText(api_key);
        }

        if (region != null) {
            regionEditText.setText(region);
        }
    }

//    public void closeActivity (View view) {
//        finish();
//    }
}
