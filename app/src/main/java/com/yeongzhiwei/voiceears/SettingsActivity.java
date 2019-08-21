package com.yeongzhiwei.voiceears;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;

    private String cognitiveServicesApiKey;
    private String cognitiveServicesRegion;

    // UI
    private EditText apikeyEditText;
    private EditText regionEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_key), MODE_PRIVATE);

        initializeViews();
        loadSavedPreferences();
        setEditTextHint();
    }

    private void initializeViews() {
        apikeyEditText = (EditText) findViewById(R.id.editText_key);
        regionEditText = (EditText) findViewById(R.id.editText_region);
    }

    private void loadSavedPreferences() {
        cognitiveServicesApiKey = loadSavedCognitiveServicesApiKey();
        cognitiveServicesRegion = loadSavedCognitiveServicesRegion();
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
            saveCognitiveServicesApiKey(newCognitiveServicesApiKey);
        }

        String newCognitiveServicesRegion = regionEditText.getText().toString();
        if (newCognitiveServicesRegion.trim().length() != 0) {
            saveCognitiveServicesRegion(newCognitiveServicesRegion);
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

    private String loadSavedCognitiveServicesApiKey() {
        return sharedPreferences.getString(getString(R.string.saved_cognitive_services_api_key), null);
    }

    private void saveCognitiveServicesApiKey(String newKey) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.saved_cognitive_services_api_key), newKey);
        editor.commit();
    }


    private String loadSavedCognitiveServicesRegion() {
        return sharedPreferences.getString(getString(R.string.saved_cognitive_services_region), null);
    }

    private void saveCognitiveServicesRegion(String newRegion) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.saved_cognitive_services_region), newRegion);
        editor.commit();
    }
}
