package com.yeongzhiwei.voiceears;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    // UI
    private Button synthesizeButton;
    private LinearLayout messageLinearLayout;
    private EditText synthesizeEditText;
    private ScrollView messageScrollView;

    // Cognitive Services
    private static String speechSubscriptionKey = "0c2815f15dd145c38b8d6e16f7d0c794";
    private static String speechRegion = "southeastasia";
    // Text-to-Speech
    private Synthesizer synthesizer = null;
    private Counter counter = new Counter();
    PaintDrawable paintDrawable = null;
    // Speech-to-Text
    private Recognizer recognizer = null;
    private TextView recognizerTextView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        loadSavedInstanceState(savedInstanceState);
        configureTextToSpeech();
//        configureSpeechToText();
    }

    private void initializeViews() {
        synthesizeButton = (Button) findViewById(R.id.button_synthesize);
        messageLinearLayout = (LinearLayout) findViewById(R.id.message_linearLayout);
        synthesizeEditText = (EditText) findViewById(R.id.editText_synthesize);
        messageScrollView = (ScrollView) findViewById(R.id.scroller_textView);
    }

    private void loadSavedInstanceState(final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String[] messages = savedInstanceState.getStringArray("messages");
            for (String message : messages) {
                createAndAddTextView(message);
            }
        } else {
            String welcome = "Welcome to VoiceEars. Start speaking or type and click Voice button.";
            createAndAddTextView(welcome);
        }
    }

    private void configureTextToSpeech() {
        if (synthesizer == null) {
            synthesizer = new Synthesizer(speechSubscriptionKey);
        }

        paintDrawable = new PaintDrawable(ContextCompat.getColor(this, R.color.colorPrimary));
        paintDrawable.setCornerRadius(8);

        synthesizeButton.setOnClickListener(view -> {
            if (recognizer != null) {
                recognizer.stopSpeechToText();
            }

            String message = synthesizeEditText.getText().toString().trim();
            synthesizeEditText.setText("");
            synthesizeText(message);
        });

        synthesizeButton.setEnabled(false);

        synthesizeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence == null | charSequence.toString().trim().length() == 0) {
                    synthesizeButton.setEnabled(false);
                } else {
                    synthesizeButton.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void synthesizeText(String message) {
        TextView ttsTextView = appendTyperMessage(message);
        Thread runBeforeAudio = new Thread(() -> {
            counter.increment();
        });
        runBeforeAudio.start();
        synthesizer.speakToAudio(message, () -> {
            MainActivity.this.runOnUiThread(() -> {
                ttsTextView.setBackground(paintDrawable);
            });
        }, () -> {
            MainActivity.this.runOnUiThread(() -> {
                ttsTextView.setBackground(null);
            });
            Thread runAfterAudio = new Thread(() -> {
                if (counter.decrement() == 0) {
                    if (recognizer != null) {
                        recognizer.startSpeechToText();
                    }
                }
            });
            runAfterAudio.start();
        });
    }

    private void configureSpeechToText() {
        try {
            int requestCode = 5; // unique code for the permission request
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, requestCode);

            if (recognizer == null) {
                recognizer = new Recognizer(speechSubscriptionKey, speechRegion, result -> {
                    appendSpeakerMessage(result);
                }, () -> {
                    recognizerTextView = null;
                });
                recognizer.startSpeechToText();
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, "could not init sdk, " + ex.toString());
            createAndAddTextView("Could not initialize: " + ex.toString());
        }
    }

    private TextView appendTyperMessage(final String message) {
        return createAndAddTextView("⌨ " + message);
    }

    private void appendSpeakerMessage(final String message) {
        if (recognizerTextView == null) {
            recognizerTextView = createAndAddTextView("\uD83D\uDDE3 " + message);
        } else {
            TextView recognizerTextViewCopy = recognizerTextView;
            MainActivity.this.runOnUiThread(() -> {
                recognizerTextViewCopy.setText("\uD83D\uDDE3 " + message);
                scrollToBottom();
            });
        }
    }

    private TextView createAndAddTextView(final String message) {
        TextView newTextView = new TextView(this);
        newTextView.setText(message);
        newTextView.setTextSize(24);
        newTextView.setTextIsSelectable(true);
        newTextView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        MainActivity.this.runOnUiThread(() -> {
            messageLinearLayout.addView(newTextView);
            scrollToBottom();
        });

        return newTextView;
    }

    private void scrollToBottom() {
//        int bottom = messageLinearLayout.getBottom() + messageScrollView.getBottom();
//        int delta = bottom - (messageScrollView.getScrollY() + messageScrollView.getHeight());
//        messageScrollView.smoothScrollBy(0, delta);
        messageScrollView.smoothScrollBy(0, messageLinearLayout.getHeight());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save the data when rotating the orientation
        super.onSaveInstanceState(outState);

        final int childCount = messageLinearLayout.getChildCount();
        if (childCount == 0) {
            return;
        }

        String[] messages = new String[childCount];
        for (int i = 0; i < childCount; i++) {
            TextView textView = (TextView) messageLinearLayout.getChildAt(i);
            messages[i] = textView.getText().toString();
        }

        outState.putStringArray("messages", messages);
    }

    // https://stackoverflow.com/questions/38158953/how-to-create-button-in-action-bar-in-android
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mymenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.button_gender) {
            if (synthesizer != null) {
                Voice.Gender gender = synthesizer.getVoice().toggleVoice();

                if (gender == Voice.Gender.Male) {
                    item.setIcon(R.drawable.boy);
                } else {
                    item.setIcon(R.drawable.girl);
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
