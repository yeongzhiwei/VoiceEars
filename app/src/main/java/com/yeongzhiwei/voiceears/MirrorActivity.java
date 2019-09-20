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
    private ConstraintLayout constraintLayout;
    private ScrollView scrollView_mirrored;
    private TextView textView_mirrored;
    private RelativeLayout relativeLayout_setting;
    private SeekBar seekBar_mirroredTextSize;
    private ImageButton imageButton_mode;
    private ImageButton imageButton_clear;
    private EditText editText_original;

    private final Integer seekBarMinValue = 10;
    private Integer mirroredTextViewSize = 12; // default
    private String mirroredText;

    private Boolean isMirrorMode = true;

    private Float defaultEditTextSize;
    private Drawable defaultEditTextDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mirror);

        loadSavedPreferences();
        initializeViews();
        configureViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        savePreferences();
    }

    private void savePreferences() {
        mirroredText = editText_original.getText().toString();
        PreferencesHelper.save(this, PreferencesHelper.Key.mirroredTextKey, mirroredText);
        PreferencesHelper.save(this, PreferencesHelper.Key.mirroredTextViewSizeKey, mirroredTextViewSize);
        PreferencesHelper.save(this, PreferencesHelper.Key.mirroredMirrorMode, isMirrorMode ? 1 : 0);
    }

    private void loadSavedPreferences() {
        mirroredText = PreferencesHelper.loadString(this, PreferencesHelper.Key.mirroredTextKey, "");
        mirroredTextViewSize = PreferencesHelper.loadInt(this, PreferencesHelper.Key.mirroredTextViewSizeKey, mirroredTextViewSize);
        isMirrorMode = PreferencesHelper.loadInt(this, PreferencesHelper.Key.mirroredMirrorMode, 1) != 0;
    }

    private void initializeViews() {
        constraintLayout = findViewById(R.id.parent_layout);
        scrollView_mirrored = findViewById(R.id.scrollView_mirrored);
        textView_mirrored = findViewById(R.id.textView_mirrored);
        relativeLayout_setting = findViewById(R.id.relativeLayout_setting);
        seekBar_mirroredTextSize = findViewById(R.id.seekBar_mirroredTextSize);
        imageButton_mode = findViewById(R.id.imageButton_mode);
        imageButton_clear = findViewById(R.id.imageButton_clear);
        editText_original = findViewById(R.id.editText_original);

        defaultEditTextSize = editText_original.getTextSize();
        defaultEditTextDrawable = editText_original.getBackground();
    }

    private void configureViews() {
        setMode(isMirrorMode);
        editText_original.setText(mirroredText);
        textView_mirrored.setText(mirroredText);
        textView_mirrored.setTextSize(mirroredTextViewSize);

        seekBar_mirroredTextSize.setProgress(mirroredTextViewSize - seekBarMinValue);

        seekBar_mirroredTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mirroredTextViewSize = i + seekBarMinValue;
                adjustMirroredTextViewSize(mirroredTextViewSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        imageButton_mode.setOnClickListener(view -> {
            isMirrorMode = scrollView_mirrored.getVisibility() == View.GONE;
            setMode(isMirrorMode);
        });

        imageButton_clear.setOnClickListener(view -> {
            clearOriginalEditText();
        });

        editText_original.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                setMirroredTextViewText(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void setMode(Boolean isMirrorMode) {
        if (isMirrorMode) {
            // Toggle to Mirror Mode
            scrollView_mirrored.setVisibility(View.VISIBLE);

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);
            constraintSet.connect(editText_original.getId(), ConstraintSet.BOTTOM, constraintLayout.getId(), ConstraintSet.BOTTOM,0);
            constraintSet.connect(editText_original.getId(), ConstraintSet.TOP, relativeLayout_setting.getId(), ConstraintSet.BOTTOM,0);
            constraintSet.connect(relativeLayout_setting.getId(), ConstraintSet.BOTTOM, editText_original.getId(), ConstraintSet.TOP,0);
            constraintSet.connect(relativeLayout_setting.getId(), ConstraintSet.TOP, scrollView_mirrored.getId(), ConstraintSet.BOTTOM,0);
            constraintSet.applyTo(constraintLayout);

            editText_original.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            editText_original.requestLayout();
            editText_original.setMaxLines(3);
            editText_original.setBackground(defaultEditTextDrawable); // add the underbar
            editText_original.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultEditTextSize);

            imageButton_mode.setImageResource(R.drawable.text_borderless);
        } else {
            // Toggle to Text Mode
            scrollView_mirrored.setVisibility(View.GONE);

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);
            constraintSet.connect(editText_original.getId(), ConstraintSet.BOTTOM, relativeLayout_setting.getId(), ConstraintSet.TOP,0);
            constraintSet.connect(editText_original.getId(), ConstraintSet.TOP, constraintLayout.getId(), ConstraintSet.TOP,0);
            constraintSet.connect(relativeLayout_setting.getId(), ConstraintSet.BOTTOM, constraintLayout.getId(), ConstraintSet.BOTTOM,0);
            constraintSet.connect(relativeLayout_setting.getId(), ConstraintSet.TOP, editText_original.getId(), ConstraintSet.BOTTOM,0);
            constraintSet.applyTo(constraintLayout);

            editText_original.getLayoutParams().height = 0;
            editText_original.requestLayout();
            editText_original.setMaxLines(Integer.MAX_VALUE);
            editText_original.setBackgroundResource(Color.TRANSPARENT); // remove the underbar
            editText_original.setTextSize(mirroredTextViewSize);

            imageButton_mode.setImageResource(R.drawable.mirror_borderless);
        }
    }

    private void clearOriginalEditText() {
        editText_original.setText("");
    }

    private void adjustMirroredTextViewSize(Integer size) {
        textView_mirrored.setTextSize(size);

        if (!isMirrorMode) {
            editText_original.setTextSize(size);
        }
    }

    private void setMirroredTextViewText(CharSequence text) {
        textView_mirrored.setText(text);
    }
}