package com.yeongzhiwei.voiceears.setting;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.yeongzhiwei.voiceears.PreferencesHelper;
import com.yeongzhiwei.voiceears.R;

public class SettingsActivity extends AppCompatActivity {
    private String cognitiveServicesApiKey;
    private String cognitiveServicesRegion;

    private EditText apikeyEditText;
    private EditText regionEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        loadSavedPreferences();
        initializeViews();
        refreshEditTextHint();
    }

    private void loadSavedPreferences() {
        cognitiveServicesApiKey = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesApiKeyKey);
        cognitiveServicesRegion = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesRegionKey);
    }

    private void initializeViews() {
        apikeyEditText = findViewById(R.id.editText_key);
        regionEditText = findViewById(R.id.editText_region);
    }

    private void refreshEditTextHint() {
        if (cognitiveServicesApiKey != null) {
            apikeyEditText.setHint("***" + cognitiveServicesApiKey.substring(Math.max(0, cognitiveServicesApiKey.length() - 5)));
        }

        if (cognitiveServicesRegion != null) {
            regionEditText.setHint(cognitiveServicesRegion);
        }
    }

    private void updateCognitiveServicesVariables() {
        String newCognitiveServicesApiKey = apikeyEditText.getText().toString();
        if (newCognitiveServicesApiKey.trim().length() != 0) {
            cognitiveServicesApiKey = newCognitiveServicesApiKey;
        }

        String newCognitiveServicesRegion = regionEditText.getText().toString();
        if (newCognitiveServicesRegion.trim().length() != 0) {
            cognitiveServicesRegion = newCognitiveServicesRegion;
        }
    }

    private void savePreferences() {
        PreferencesHelper.save(this, PreferencesHelper.Key.cognitiveServicesApiKeyKey, cognitiveServicesApiKey);
        PreferencesHelper.save(this, PreferencesHelper.Key.cognitiveServicesRegionKey, cognitiveServicesRegion);
    }

    public void onSave(View view) {
        updateCognitiveServicesVariables();
        savePreferences();

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
