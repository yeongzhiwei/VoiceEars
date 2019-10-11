package com.yeongzhiwei.voiceears;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class MirrorActivity extends AppCompatActivity {
    //region VARIABLES
    private static final Integer seekBarMinValue = 10;

    private ConstraintLayout parentConstraintLayout;
    private ScrollView mirroredScrollView;
    private TextView mirroredTextView;
    private RelativeLayout settingRelativeLayout;
    private SeekBar textSizeSeekBar;
    private ImageButton modeImageButton;
    private ImageButton clearImageButton;
    private EditText originalEditText;

    private Boolean isMirrorMode = true;
    private String mirroredText;
    private Integer mirroredTextSize = 20;

    private Float defaultEditTextSize;
    private Drawable defaultEditTextDrawable;

    //endregion

    //region ACTIVITY LIFECYCLE
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mirror);

        loadSavedPreferences();
        initializeViews();
        refreshAllViews();
        addEventListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        savePreferences();
    }

    //endregion

    //region SHARED PREFERENCES

    private void savePreferences() {
        PreferencesHelper.save(this, PreferencesHelper.Key.mirroredMirrorMode, isMirrorMode ? 1 : 0);
        PreferencesHelper.save(this, PreferencesHelper.Key.mirroredTextKey, originalEditText.getText().toString());
        PreferencesHelper.save(this, PreferencesHelper.Key.mirroredTextViewSizeKey, mirroredTextSize);
    }

    private void loadSavedPreferences() {
        isMirrorMode = PreferencesHelper.loadInt(this, PreferencesHelper.Key.mirroredMirrorMode, 1) != 0;
        mirroredText = PreferencesHelper.loadString(this, PreferencesHelper.Key.mirroredTextKey, "");
        mirroredTextSize = PreferencesHelper.loadInt(this, PreferencesHelper.Key.mirroredTextViewSizeKey, mirroredTextSize);
    }

    //endregion

    private void initializeViews() {
        parentConstraintLayout = findViewById(R.id.constraintLayout_parent);
        mirroredScrollView = findViewById(R.id.scrollView_mirrored);
        mirroredTextView = findViewById(R.id.textView_mirrored);
        settingRelativeLayout = findViewById(R.id.relativeLayout_setting);
        textSizeSeekBar = findViewById(R.id.seekBar_mirroredTextSize);
        modeImageButton = findViewById(R.id.imageButton_mode);
        clearImageButton = findViewById(R.id.imageButton_clear);
        originalEditText = findViewById(R.id.editText_original);

        defaultEditTextSize = originalEditText.getTextSize();
        defaultEditTextDrawable = originalEditText.getBackground();
    }

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
        textSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setMessageTextSize(i + seekBarMinValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        modeImageButton.setOnClickListener(view -> {
            toggleMirrorMode();
        });

        clearImageButton.setOnClickListener(view -> {
            removeLastWordFromOriginalEditText();
        });

        clearImageButton.setOnLongClickListener(view -> {
            clearOriginalEditText();
            return true;
        });

        originalEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                setMirroredTextViewText(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void removeLastWordFromOriginalEditText() {
        String originalText = originalEditText.getText().toString().replaceFirst(" +$", "");
        int lastIndexOfSpace = originalText.lastIndexOf(" ");
        int lastIndexOfNewline = originalText.lastIndexOf("\n");

        if (lastIndexOfSpace != -1 || lastIndexOfNewline != -1) {
            int endIndex = Math.max(lastIndexOfSpace, lastIndexOfNewline);
            if (endIndex + 1 != originalText.length()) {
                endIndex += 1;
            }
            originalEditText.setText(originalText.substring(0, endIndex));
        } else {
            originalEditText.setText("");
        }

        originalEditText.setSelection(originalEditText.getText().length());
    }

    private void clearOriginalEditText() {
        originalEditText.setText("");
    }

    private void setMirroredTextViewText(CharSequence text) {
        mirroredTextView.setText(text);
    }

    // Refresh views based on state

    private void refreshAllViews() {
        refreshMirrorModeViews();
        refreshMirroredTextViews();
        refreshMirroredTextSize();
        refreshTextSizeSeekBar();
    }

    private void refreshMirrorModeViews() {
        if (mirroredScrollView == null) {
            // Assume that all other views are null if mirroredScrollView is null
            return;
        }

        if (isMirrorMode) {
            // Toggle to Mirror Mode
            mirroredScrollView.setVisibility(View.VISIBLE);

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(parentConstraintLayout);
            constraintSet.connect(originalEditText.getId(), ConstraintSet.BOTTOM, parentConstraintLayout.getId(), ConstraintSet.BOTTOM,0);
            constraintSet.connect(originalEditText.getId(), ConstraintSet.TOP, settingRelativeLayout.getId(), ConstraintSet.BOTTOM,0);
            constraintSet.connect(settingRelativeLayout.getId(), ConstraintSet.BOTTOM, originalEditText.getId(), ConstraintSet.TOP,0);
            constraintSet.connect(settingRelativeLayout.getId(), ConstraintSet.TOP, mirroredScrollView.getId(), ConstraintSet.BOTTOM,0);
            constraintSet.applyTo(parentConstraintLayout);

            originalEditText.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            originalEditText.requestLayout();
            originalEditText.setMaxLines(3);
            originalEditText.setBackground(defaultEditTextDrawable); // add the underbar
            originalEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultEditTextSize);

            modeImageButton.setImageResource(R.drawable.ic_text_borderless);
        } else {
            // Toggle to Text Mode
            mirroredScrollView.setVisibility(View.GONE);

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(parentConstraintLayout);
            constraintSet.connect(originalEditText.getId(), ConstraintSet.BOTTOM, settingRelativeLayout.getId(), ConstraintSet.TOP,0);
            constraintSet.connect(originalEditText.getId(), ConstraintSet.TOP, parentConstraintLayout.getId(), ConstraintSet.TOP,0);
            constraintSet.connect(settingRelativeLayout.getId(), ConstraintSet.BOTTOM, parentConstraintLayout.getId(), ConstraintSet.BOTTOM,0);
            constraintSet.connect(settingRelativeLayout.getId(), ConstraintSet.TOP, originalEditText.getId(), ConstraintSet.BOTTOM,0);
            constraintSet.applyTo(parentConstraintLayout);

            originalEditText.getLayoutParams().height = 0;
            originalEditText.requestLayout();
            originalEditText.setMaxLines(Integer.MAX_VALUE);
            originalEditText.setBackgroundResource(Color.TRANSPARENT); // remove the underbar
            originalEditText.setTextSize(mirroredTextSize);

            modeImageButton.setImageResource(R.drawable.ic_mirror_borderless);
        }
    }

    private void refreshMirroredTextViews() {
        if (mirroredTextView != null && originalEditText != null) {
            mirroredTextView.setText(mirroredText);
            originalEditText.setText(mirroredText);
        }
    }

    private void refreshMirroredTextSize() {
        if (mirroredTextView != null && originalEditText != null) {
            mirroredTextView.setTextSize(mirroredTextSize);

            if (!isMirrorMode) {
                originalEditText.setTextSize(mirroredTextSize);
            }
        }
    }

    private void refreshTextSizeSeekBar() {
        if (textSizeSeekBar != null) {
            textSizeSeekBar.setProgress(mirroredTextSize - seekBarMinValue);
        }
    }

    //endregion
}