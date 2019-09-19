package com.yeongzhiwei.voiceears;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {
    static SharedPreferences sharedPreferences;

    private String cognitiveServicesApiKey;
    private String cognitiveServicesRegion;

    private EditText apikeyEditText;
    private EditText regionEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences(PreferencesHelper.sharedPreferencesName, MODE_PRIVATE);

        initializeViews();
        loadSavedPreferences();
        setEditTextHint();
    }

    private void initializeViews() {
        apikeyEditText = findViewById(R.id.editText_key);
        regionEditText = findViewById(R.id.editText_region);
    }

    private void loadSavedPreferences() {
        cognitiveServicesApiKey = PreferencesHelper.loadString(sharedPreferences, PreferencesHelper.Key.cognitiveServicesApiKeyKey);
        cognitiveServicesRegion = PreferencesHelper.loadString(sharedPreferences, PreferencesHelper.Key.cognitiveServicesRegionKey);
    }

    private void setEditTextHint() {
        if (cognitiveServicesApiKey != null) {
            apikeyEditText.setHint("***" + cognitiveServicesApiKey.substring(Math.max(0, cognitiveServicesApiKey.length() - 5)));
        }

        if (cognitiveServicesRegion != null) {
            regionEditText.setHint(cognitiveServicesRegion);
        }
    }

    public void onSave(View view) {
        String newCognitiveServicesApiKey = apikeyEditText.getText().toString();
        if (newCognitiveServicesApiKey.trim().length() != 0) {
            PreferencesHelper.save(sharedPreferences, PreferencesHelper.Key.cognitiveServicesApiKeyKey, newCognitiveServicesApiKey);
        }

        String newCognitiveServicesRegion = regionEditText.getText().toString();
        if (newCognitiveServicesRegion.trim().length() != 0) {
            PreferencesHelper.save(sharedPreferences, PreferencesHelper.Key.cognitiveServicesRegionKey, newCognitiveServicesRegion);
        }

        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    public void onCancel(View view) {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
    }
}
