package com.yeongzhiwei.voiceears.presentation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.yeongzhiwei.voiceears.R;

public class PresentationMessageActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText messageEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation_message);

        toolbar = findViewById(R.id.toolbar);
        messageEditText = findViewById(R.id.editText_message);

        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
        }

        refreshUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_presentation_message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
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
        Intent intent = getIntent();
        int requestCode = intent.getIntExtra(PresentationActivity.EXTRA_REQUEST_CODE, -1);

        if (requestCode == PresentationActivity.ADD_REQUEST_CODE) {
            toolbar.setTitle(R.string.presentation_message_title_create);
        } else if (requestCode == PresentationActivity.EDIT_REQUEST_CODE) {
            toolbar.setTitle(R.string.presentation_message_title_edit);
            String message = intent.getStringExtra(PresentationActivity.EXTRA_MESSAGE);
            messageEditText.setText(message);
        }
    }

    public void save() {
        String message = messageEditText.getText().toString().trim();

        Intent returnIntent = new Intent();
        returnIntent.putExtra(PresentationActivity.EXTRA_MESSAGE, message);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

}
