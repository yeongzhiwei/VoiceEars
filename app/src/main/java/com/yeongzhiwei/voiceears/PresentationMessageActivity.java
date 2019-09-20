package com.yeongzhiwei.voiceears;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PresentationMessageActivity extends AppCompatActivity {
    private EditText messageEditText;
    private Button saveButton;
    private Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation_message);

        initializeViews();
        configureViews();
        getIntentAndUpdateViews();
    }

    private void initializeViews() {
        messageEditText = findViewById(R.id.editText_message);
        saveButton = findViewById(R.id.button_save);
        cancelButton = findViewById(R.id.button_cancel);
    }

    private void configureViews() {
        refreshSaveButton();

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
            String message = messageEditText.getText().toString();

            Intent returnIntent = new Intent();
            returnIntent.putExtra(PresentationActivity.EXTRA_MESSAGE, message);
            setResult(RESULT_OK, returnIntent);
            finish();
        });

        cancelButton.setOnClickListener(view -> {
            finish();
        });
    }

    private void refreshSaveButton() {
        saveButton.setEnabled(messageEditText.getText().toString().trim().length() != 0);
    }

    private void getIntentAndUpdateViews() {
        Intent intent = getIntent();
        String message = intent.getStringExtra(PresentationActivity.EXTRA_MESSAGE);
        int requestCode = intent.getIntExtra(PresentationActivity.EXTRA_REQUEST_CODE, -1);

        if (requestCode == PresentationActivity.addRequestCode) {
            messageEditText.setHint("New message");
            saveButton.setText("Create");
        } else if (requestCode == PresentationActivity.editRequestCode) {
            messageEditText.setText(message);
            saveButton.setText("Save");
        }
    }
}
