package com.yeongzhiwei.voiceears.setting;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.yeongzhiwei.voiceears.PreferencesHelper;
import com.yeongzhiwei.voiceears.R;
import com.yeongzhiwei.voiceears.ttsstt.Gender;

public class SettingsActivity extends AppCompatActivity {

    private EditText apikeyEditText;
    private EditText regionEditText;
    private RadioGroup genderRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        apikeyEditText = findViewById(R.id.editText_key);
        regionEditText = findViewById(R.id.editText_region);
        genderRadioGroup = findViewById(R.id.radioGroup_gender);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
        }

        for (Gender gender : Gender.values()) {
            RadioButton radioButton = new MaterialRadioButton(genderRadioGroup.getContext());
            radioButton.setText(gender.name());
            radioButton.setId(gender.ordinal());
            genderRadioGroup.addView(radioButton);
        }

        refreshUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                cancel();
                return true;
            case R.id.action_save:
                save();
                return true;
            default:
                // Do nothing
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshUI() {
        String cognitiveServicesApiKey = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesApiKeyKey, "");
        apikeyEditText.setText(cognitiveServicesApiKey);

        String cognitiveServicesRegion = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesRegionKey, "");
        regionEditText.setText(cognitiveServicesRegion);

        String gender = PreferencesHelper.loadString(this, PreferencesHelper.Key.genderKey, Gender.Male.name());
        RadioButton radioButton = genderRadioGroup.findViewById(Gender.valueOf(gender).ordinal());
        radioButton.setChecked(true);
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

        int genderRadioButtonId = genderRadioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = genderRadioGroup.findViewById(genderRadioButtonId);
        String gender = radioButton.getText().toString();
        PreferencesHelper.save(this, PreferencesHelper.Key.genderKey, gender);
    }

    public void save() {
        saveSettings();

        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    public void cancel() {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
    }

}
