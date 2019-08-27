package com.yeongzhiwei.voiceears;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class MirrorActivity extends AppCompatActivity {
    private TextView textView_mirrored;
    private SeekBar seekBar_mirroredTextSize;
    private ImageButton imageButton_clear;
    private EditText editText_original;

    private Integer textViewSize;
    private final Integer seekBarMinValue = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mirror);

        initializeViews();
        configureViews();
    }

    private void initializeViews() {
        textView_mirrored = findViewById(R.id.textView_mirrored);
        seekBar_mirroredTextSize = findViewById(R.id.seekBar_mirroredTextSize);
        imageButton_clear = findViewById(R.id.imageButton_clear);
        editText_original = findViewById(R.id.editText_original);
    }

    private void configureViews() {
        seekBar_mirroredTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                textViewSize = i + seekBarMinValue;
                adjustMirroredTextViewSize(textViewSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                saveTextViewSize(textViewSize);
            }
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

    private void clearOriginalEditText() {
        editText_original.setText("");
    }

    private void adjustMirroredTextViewSize(Integer size) {
        textView_mirrored.setTextSize(size);
    }

    private void setMirroredTextViewText(CharSequence text) {
        textView_mirrored.setText(text);
    }
}
