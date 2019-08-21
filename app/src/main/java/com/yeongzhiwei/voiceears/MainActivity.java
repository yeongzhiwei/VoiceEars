package com.yeongzhiwei.voiceears;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.widget.Toast;


import java.util.HashMap;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_API_KEY = "com.yeongzhiwei.voiceears.COGNITIVE_SERVICES_KEY";
    public final static String EXTRA_REGION = "com.yeongzhiwei.voiceears.COGNITIVE_SERVICES_REGION";

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private SharedPreferences sharedPreferences;
    int requestCode = 5; // unique code for the permission request

    // UI
    private MenuItem genderMenuItem;
    private ScrollView messageScrollView;
    private LinearLayout messageLinearLayout;
    private SeekBar sizeSeekBar;
    private ImageView imageViewScrollDown;
    private EditText synthesizeEditText;
    private Button synthesizeButton;

    private Integer textViewSize;
    private Boolean enableAutoScrollDown = true;
    private final Integer seekBarMinValue = 10;

    // Cognitive Services
    private static String speechSubscriptionKey = "0c2815f15dd145c38b8d6e16f7d0c794";
    private static String speechRegion = "southeastasia";
    // Text-to-Speech
    private Synthesizer synthesizer = null;
    private Counter counter = new Counter();
    PaintDrawable paintDrawable = null;
    private Voice.Gender gender;
    // Speech-to-Text
    private Recognizer recognizer = null;
    private HashMap<Integer, TextView> recognizerTextViews = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        textViewSize = loadTextViewSize();

        initializeViews();
        configureViews();
        loadSavedInstanceState(savedInstanceState);
        configureTextToSpeech();
        configureSpeechToText();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (recognizer != null) {
            recognizer.startSpeechToText();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (recognizer != null) {
            recognizer.stopSpeechToText();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.stopSpeechToTextAndReleaseMicrophone();
        }
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

        sizeSeekBar.setProgress(textViewSize - seekBarMinValue);

        sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                textViewSize = i + seekBarMinValue;
                adjustTextViewSize(textViewSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveTextViewSize(textViewSize);
            }
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

    private void loadSavedInstanceState(final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String[] messages = savedInstanceState.getStringArray("messages");
            for (String message : messages) {
                createAndAddTextView(message);
            }
        } else {
            String welcome = "Welcome to VoiceEars. Start speaking or type and click Speak button. Click the face icon at top-right to toggle the gender.";
            createAndAddTextView(welcome);
        }
    }

    private void configureTextToSpeech() {
        if (synthesizer == null) {
            gender = Voice.Gender.valueOf(loadGender());
            refreshGenderIcon();

            synthesizer = new Synthesizer(speechSubscriptionKey, Voice.getDefaultVoice(gender));
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
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, requestCode);

            if (recognizer == null) {
                recognizer = new Recognizer(speechSubscriptionKey, speechRegion, (order, result) -> {
                    appendSpeakerMessage(order, result);
                });
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, "could not init sdk, " + ex.toString());
        }
    }

    /* ADD TEXTVIEW TO LINEARLAYOUT IN SCROLLVIEW */

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

    /* SCROLLING IN SCROLLVIEW */

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

    /* TEXT SIZE SEEKBAR */

    private void adjustTextViewSize(Integer size) {
        int childcount = messageLinearLayout.getChildCount();
        for (int i=0; i < childcount; i++){
            TextView textView = (TextView) messageLinearLayout.getChildAt(i);
            textView.setTextSize(size);
        }
    }

    /* ACTIONBAR OPTIONS */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // https://stackoverflow.com/questions/38158953/how-to-create-button-in-action-bar-in-android
        getMenuInflater().inflate(R.menu.mymenu, menu);

        genderMenuItem = menu.findItem(R.id.action_gender);
        refreshGenderIcon();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_gender) {
            if (synthesizer != null) {
                gender = synthesizer.getVoice().toggleVoice();
                refreshGenderIcon();
                saveGender(gender.name());
            }
        } else if (itemId == R.id.action_settings) {
            Intent messageIntent = new Intent(this, SettingsActivity.class);
            messageIntent.putExtra(EXTRA_API_KEY, "haha");
            messageIntent.putExtra(EXTRA_REGION, "southeastasia");
            startActivity(messageIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshGenderIcon() {
        if (genderMenuItem == null) {
            return;
        }

        if (gender == Voice.Gender.Male) {
            genderMenuItem.setIcon(R.drawable.boy);
        } else {
            genderMenuItem.setIcon(R.drawable.girl);
        }
    }

    /* LAYOUT PERSISTENCE ON ROTATION */

    @Override
    public void onSaveInstanceState(Bundle outState) {
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

    /*  KEY-VALUE PERSISTENT STORAGE */

    private int loadTextViewSize() {
        int defaultTextViewSize = getResources().getInteger(R.integer.default_textView_size);
        return sharedPreferences.getInt(getString(R.string.saved_textView_size_key), defaultTextViewSize);
    }

    private void saveTextViewSize(int newSize) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.saved_textView_size_key), newSize);
        editor.commit();
    }

    private String loadGender() {
        String defaultGender = getResources().getString(R.string.default_gender);
        return sharedPreferences.getString(getString(R.string.saved_gender_key), defaultGender);
    }

    private void saveGender(String newGender) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.saved_gender_key), newGender);
        editor.commit();
    }

    /* PERMISSION */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // https://stackoverflow.com/questions/30719047/android-m-check-runtime-permission-how-to-determine-if-the-user-checked-nev
        if (requestCode == this.requestCode) {
            for (int i = 0, len = permissions.length; i < len; i++) {
                String permission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    boolean shouldShowRationale = shouldShowRequestPermissionRationale(permission);
                    if (! shouldShowRationale) {
                        Toast.makeText(MainActivity.this, getString(R.string.permission_microphone_denied_warning), Toast.LENGTH_LONG).show();
                    } else if (RECORD_AUDIO.equals(permission)) {
                        new AlertDialog.Builder(MainActivity.this)
                            .setMessage(getString(R.string.permission_microphone_request_message))
                            .setPositiveButton("OK", (dialog, which) -> {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO}, requestCode);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                Toast.makeText(MainActivity.this, getString(R.string.permission_microphone_denied_warning), Toast.LENGTH_LONG).show();
                            })
                            .create()
                            .show();
                    }
                }
            }
        }
    }
}
