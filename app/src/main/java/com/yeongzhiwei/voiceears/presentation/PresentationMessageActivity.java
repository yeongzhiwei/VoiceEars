package com.yeongzhiwei.voiceears.presentation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import com.yeongzhiwei.voiceears.R;

public class PresentationMessageActivity extends AppCompatActivity {
    private EditText messageEditText;
    private Button saveButton;
    private Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation_message);

        initializeViews();
        refreshSaveButton();
        addEventListeners();
        getIntentAndUpdateViews();
    }

    private void initializeViews() {
        messageEditText = findViewById(R.id.editText_message);
        saveButton = findViewById(R.id.button_save);
        cancelButton = findViewById(R.id.button_cancel);
    }

    private void addEventListeners() {
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                refreshSaveButton();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        saveButton.setOnClickListener(view -> {
            saveAndReturnIntent();
        });

        cancelButton.setOnClickListener(view -> {
            finish();
        });
    }

    private void refreshSaveButton() {
        saveButton.setEnabled(messageEditText.getText().toString().trim().length() != 0);
    }

    private void saveAndReturnIntent() {
        String message = messageEditText.getText().toString();

        Intent returnIntent = new Intent();
        returnIntent.putExtra(PresentationActivity.EXTRA_MESSAGE, message);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private void getIntentAndUpdateViews() {
        Intent intent = getIntent();
        String message = intent.getStringExtra(PresentationActivity.EXTRA_MESSAGE);
        int requestCode = intent.getIntExtra(PresentationActivity.EXTRA_REQUEST_CODE, -1);

        if (requestCode == PresentationActivity.addRequestCode) {
            messageEditText.setHint(R.string.presentation_message_editText_hint_new);
            saveButton.setText(R.string.presentation_message_button_create);
        } else if (requestCode == PresentationActivity.editRequestCode) {
            messageEditText.setText(message);
            saveButton.setText(R.string.presentation_message_button_save);
        }
    }
}
