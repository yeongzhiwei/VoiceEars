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
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.HashMap;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    // UI
    private ScrollView messageScrollView;
    private LinearLayout messageLinearLayout;
    private SeekBar sizeSeekBar;
    private ImageView imageViewScrollDown;
    private EditText synthesizeEditText;
    private Button synthesizeButton;

    private Integer textViewSize = 24;
    private Boolean enableAutoScrollDown = true;

    // Cognitive Services
    private static String speechSubscriptionKey = "0c2815f15dd145c38b8d6e16f7d0c794";
    private static String speechRegion = "southeastasia";
    // Text-to-Speech
    private Synthesizer synthesizer = null;
    private Counter counter = new Counter();
    PaintDrawable paintDrawable = null;
    // Speech-to-Text
    private Recognizer recognizer = null;
    private HashMap<Integer, TextView> recognizerTextViews = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        configureViews();
        loadSavedInstanceState(savedInstanceState);
        configureTextToSpeech();
        configureSpeechToText();
        configureSeekBar();
    }

    private void initializeViews() {
        messageScrollView = (ScrollView) findViewById(R.id.scroller_textView);
        messageLinearLayout = (LinearLayout) findViewById(R.id.message_linearLayout);
        sizeSeekBar = (SeekBar) findViewById(R.id.seekBar_size);
        imageViewScrollDown = (ImageView) findViewById(R.id.imageView_scrolldown);
        synthesizeEditText = (EditText) findViewById(R.id.editText_synthesize);
        synthesizeButton = (Button) findViewById(R.id.button_synthesize);
    }

    private void configureViews() {
        messageScrollView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (messageLinearLayout.getHeight() - messageScrollView.getHeight() > scrollY) {
                enableAutoScrollDown = false;
                imageViewScrollDown.setVisibility(View.VISIBLE);
            } else {
                enableAutoScrollDown = true;
                imageViewScrollDown.setVisibility(View.GONE);
            }
        });

        imageViewScrollDown.setOnClickListener(view -> {
            scrollDown();
        });
    }

    private void loadSavedInstanceState(final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String[] messages = savedInstanceState.getStringArray("messages");
            for (String message : messages) {
                createAndAddTextView(message);
            }
        } else {
            String welcome = "Welcome to VoiceEars. Start speaking or type and click Speak button. Click the face icon at top-right to toggle the gender.";
            welcome = welcome + "Welcome to VoiceEars. Start speaking or type and click Speak button. Click the face icon at top-right to toggle the gender.Welcome to VoiceEars. Start speaking or type and click Speak button. Click the face icon at top-right to toggle the gender.Welcome to VoiceEars. Start speaking or type and click Speak button. Click the face icon at top-right to toggle the gender.";
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
        Thread runBeforeAudio = new Thread(() -> {
            counter.increment();
        });
        runBeforeAudio.start();

        TextView ttsTextView = appendTyperMessage(message);
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
                recognizer = new Recognizer(speechSubscriptionKey, speechRegion, (order, result) -> {
                    appendSpeakerMessage(order, result);
                });
                recognizer.startSpeechToText();
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, "could not init sdk, " + ex.toString());
        }
    }

    private TextView appendTyperMessage(final String message) {
        return createAndAddTextView("⌨ " + message);
    }

    private void appendSpeakerMessage(final Integer order, final String message) {
        final TextView textView = recognizerTextViews.get(order);

        if (textView == null) {
            TextView newTextView = createAndAddTextView("\uD83D\uDDE3 " + message);
            recognizerTextViews.put(order, newTextView);
        } else {
            MainActivity.this.runOnUiThread(() -> {
                textView.setText("\uD83D\uDDE3 " + message);
                scrollToBottom();
            });
        }
    }

    private TextView createAndAddTextView(final String message) {
        TextView newTextView = new TextView(this);
        newTextView.setText(message);
        newTextView.setTextSize(textViewSize);
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
        // https://stackoverflow.com/questions/28105945/add-view-to-scrollview-and-then-scroll-to-bottom-which-callback-is-needed
        messageScrollView.postDelayed(() -> {
            if (enableAutoScrollDown) {
                scrollDown();
            }
        }, 200);
//        Log.d(LOG_TAG, "scrollView2 getBottom(): " + messageScrollView.getBottom() + ". getHeight(): " + messageScrollView.getHeight() + ". getScrollY(): " + messageScrollView.getScrollY());
//        Log.d(LOG_TAG, "linearLayout2 getBottom(): " + messageLinearLayout.getBottom() + ". getHeight(): " + messageLinearLayout.getHeight() + ". getScrollY(): " + messageLinearLayout.getScrollY());
    }

    private void scrollDown() {
        messageScrollView.smoothScrollBy(0, messageScrollView.getHeight() + messageLinearLayout.getHeight() - messageScrollView.getScrollY());
    }



    private void configureSeekBar() {
        sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                textViewSize = i + 10;
                adjustTextViewSize(textViewSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void adjustTextViewSize(Integer size) {
        int childcount = messageLinearLayout.getChildCount();
        for (int i=0; i < childcount; i++){
            TextView textView = (TextView) messageLinearLayout.getChildAt(i);
            textView.setTextSize(size);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // https://stackoverflow.com/questions/38158953/how-to-create-button-in-action-bar-in-android
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

}
