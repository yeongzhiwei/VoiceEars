package com.yeongzhiwei.voiceears.setting;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.yeongzhiwei.voiceears.PreferencesHelper;
import com.yeongzhiwei.voiceears.R;
import com.yeongzhiwei.voiceears.ttsstt.Gender;

public class SettingsActivity extends AppCompatActivity {

    private EditText apikeyEditText;
    private EditText regionEditText;
    private Spinner genderSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        apikeyEditText = findViewById(R.id.editText_key);
        regionEditText = findViewById(R.id.editText_region);
        genderSpinner = findViewById(R.id.spinner_gender);

        initGenderSpinner();
        refreshUI();
    }

    private void initGenderSpinner() {
        ArrayAdapter<Gender> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Gender.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);
    }

    private void refreshUI() {
        String cognitiveServicesApiKey = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesApiKeyKey);
        if (cognitiveServicesApiKey != null) {
            apikeyEditText.setHint("***" + cognitiveServicesApiKey.substring(Math.max(0, cognitiveServicesApiKey.length() - 5)));
        }

        String cognitiveServicesRegion = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesRegionKey, "");
        regionEditText.setHint(cognitiveServicesRegion);

        String gender = PreferencesHelper.loadString(this, PreferencesHelper.Key.genderKey, Gender.Male.name());
        genderSpinner.setSelection(Gender.valueOf(gender).ordinal());
    }

    private void saveSettings() {
        String cognitiveServicesApiKey = apikeyEditText.getText().toString().trim();
        if (cognitiveServicesApiKey.length() != 0) {
            PreferencesHelper.save(this, PreferencesHelper.Key.cognitiveServicesApiKeyKey, cognitiveServicesApiKey);
        }

        String cognitiveServicesRegion = regionEditText.getText().toString().trim();
        if (cognitiveServicesRegion.length() != 0) {
            PreferencesHelper.save(this, PreferencesHelper.Key.cognitiveServicesRegionKey, cognitiveServicesRegion);
        }

        String gender = genderSpinner.getSelectedItem().toString();
        PreferencesHelper.save(this, PreferencesHelper.Key.genderKey, gender);
    }

    public void onSave(View view) {
        saveSettings();

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
