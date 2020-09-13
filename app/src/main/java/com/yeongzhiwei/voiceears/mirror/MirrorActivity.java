package com.yeongzhiwei.voiceears.mirror;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.yeongzhiwei.voiceears.PreferencesHelper;
import com.yeongzhiwei.voiceears.R;

public class MirrorActivity extends AppCompatActivity {
    //region VARIABLES
    private static final int TEXT_SIZE_SEEK_BAR_MIN_VALUE = 10;

    private EditText largeEditText;
    private ScrollView mirroredScrollView;
    private TextView mirroredTextView;
    private Button toggleModeButton;
    private Button clearButton;
    private SeekBar textSizeSeekBar;
    private EditText originalEditText;

    private Boolean isMirrorMode = true;
    private String mirroredText;
    private Integer mirroredTextSize = 20;

    //endregion

    //region ACTIVITY LIFECYCLE
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mirror);

        largeEditText = findViewById(R.id.editText_large);
        mirroredScrollView = findViewById(R.id.scrollView_mirrored);
        mirroredTextView = findViewById(R.id.textView_mirrored);
        toggleModeButton = findViewById(R.id.button_toggleMode);
        clearButton = findViewById(R.id.button_clear);
        textSizeSeekBar = findViewById(R.id.seekBar_mirroredTextSize);
        originalEditText = findViewById(R.id.editText_original);

        loadSavedPreferences();
        refreshAllViews();
        addEventListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();

        savePreferences();
    }

    //endregion

    //region SHARED PREFERENCES

    private void loadSavedPreferences() {
        isMirrorMode = PreferencesHelper.loadInt(this, PreferencesHelper.Key.mirroredMirrorMode, 1) != 0;
        mirroredText = PreferencesHelper.loadString(this, PreferencesHelper.Key.mirroredTextKey, "");
        mirroredTextSize = PreferencesHelper.loadInt(this, PreferencesHelper.Key.mirroredTextViewSizeKey, mirroredTextSize);
    }

    private void savePreferences() {
        PreferencesHelper.save(this, PreferencesHelper.Key.mirroredMirrorMode, isMirrorMode ? 1 : 0);
        PreferencesHelper.save(this, PreferencesHelper.Key.mirroredTextKey, mirroredText);
        PreferencesHelper.save(this, PreferencesHelper.Key.mirroredTextViewSizeKey, mirroredTextSize);
    }

    //endregion

    //region STATE

    private void toggleMirrorMode() {
        isMirrorMode = !isMirrorMode;

        refreshMirrorModeViews();
    }

    private void setMessageTextSize(Integer size) {
        mirroredTextSize = size;

        refreshMirroredTextSize();
    }

    //endregion

    //region VIEWS

    // Event listeners

    private void addEventListeners() {
        toggleModeButton.setOnClickListener(v -> {
            toggleMirrorMode();
        });

        clearButton.setOnClickListener(v -> {
            largeEditText.setText("");
            originalEditText.setText("");
            mirroredText = "";
        });

        textSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setMessageTextSize(i + TEXT_SIZE_SEEK_BAR_MIN_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        largeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                mirroredText = charSequence.toString();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        originalEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                mirroredText = charSequence.toString();
                mirroredTextView.setText(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    // Refresh views based on state

    private void refreshAllViews() {
        refreshMirroredTextSize();
        refreshMirrorModeViews();
        refreshTextSizeSeekBar();
    }

    private void refreshMirrorModeViews() {
        if (isMirrorMode) {
            largeEditText.setVisibility(View.GONE);
            mirroredScrollView.setVisibility(View.VISIBLE);
            originalEditText.setVisibility(View.VISIBLE);

            originalEditText.setText(mirroredText);
            originalEditText.requestFocus();
            originalEditText.setSelection(largeEditText.getSelectionEnd());
        } else {
            largeEditText.setVisibility(View.VISIBLE);
            mirroredScrollView.setVisibility(View.GONE);
            originalEditText.setVisibility(View.GONE);

            largeEditText.setText(mirroredText);
            largeEditText.requestFocus();
            largeEditText.setSelection(originalEditText.getSelectionEnd());
        }
    }

    private void refreshMirroredTextSize() {
        largeEditText.setTextSize(mirroredTextSize);
        mirroredTextView.setTextSize(mirroredTextSize);
    }

    private void refreshTextSizeSeekBar() {
        textSizeSeekBar.setProgress(mirroredTextSize - TEXT_SIZE_SEEK_BAR_MIN_VALUE);
    }

    //endregion
}