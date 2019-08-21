package com.yeongzhiwei.voiceears;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    public static Integer settingsRequestCode = 6; // arbitrary number
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
    private static String cognitiveServicesApiKey;
    private static String cognitiveServicesRegion;
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

        sharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_key), MODE_PRIVATE);
        loadSavedPreferences();

        initializeViews();
        configureViews();
        loadSavedInstanceState(savedInstanceState);
        configureCognitiveServices();
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
        refreshGenderIcon();

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

    private void loadSavedPreferences() {
        cognitiveServicesApiKey = loadSavedCognitiveServicesApiKey();
        cognitiveServicesRegion = loadSavedCognitiveServicesRegion();
        textViewSize = loadSavedTextViewSize();
        gender = Voice.Gender.valueOf(loadSavedGender());
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

    private void configureCognitiveServices() {
        if (cognitiveServicesApiKey.length() == 0 || cognitiveServicesRegion.length() == 0) {
            startSettingsActivity();
            Toast.makeText(MainActivity.this, R.string.toast_blank_key_or_region, Toast.LENGTH_LONG).show();
            return;
        }

        if (! authenticateApiKey()) {
            startSettingsActivity();
            Toast.makeText(MainActivity.this, R.string.toast_invalid_key_or_region, Toast.LENGTH_LONG).show();
            return;
        }

        configureTextToSpeech();
        configureSpeechToText();
    }

    private Boolean authenticateApiKey() {
        final Authentication authentication = new Authentication(cognitiveServicesApiKey, cognitiveServicesRegion);
        return authentication.getAccessToken() != null;
    }

    private void configureTextToSpeech() {
        synthesizer = new Synthesizer(cognitiveServicesApiKey, cognitiveServicesRegion, Voice.getDefaultVoice(gender));

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

            recognizer = new Recognizer(cognitiveServicesApiKey, cognitiveServicesRegion, (order, result) -> {
                appendSpeakerMessage(order, result);
            });
        } catch (Exception ex) {
            String message = "Error: Could not configure Speech to text SDK, " + ex.toString();
            createAndAddTextView(message);
            Log.e(LOG_TAG, message);
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
        for (int i = 0; i < childcount; i++) {
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
        if (item.getItemId() == R.id.action_gender) {
            toggleGender();
        } else if (item.getItemId() == R.id.action_settings) {
            startSettingsActivity();
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleGender() {
        if (synthesizer == null) {
            return;
        }

        gender = synthesizer.getVoice().toggleVoice();
        refreshGenderIcon();
        saveGender(gender.name());
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

    private void startSettingsActivity() {
        Intent messageIntent = new Intent(this, SettingsActivity.class);
        startActivityForResult(messageIntent, settingsRequestCode);
    }

    public void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == settingsRequestCode) {
            if (resultCode == RESULT_CANCELED) {
                if (! authenticateApiKey()) {
                    finish();
                }
            } else if (resultCode == RESULT_OK) {
                cognitiveServicesApiKey = loadSavedCognitiveServicesApiKey();
                cognitiveServicesRegion = loadSavedCognitiveServicesRegion();
                configureCognitiveServices();
            }
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

    private String loadSavedCognitiveServicesApiKey() {
        return sharedPreferences.getString(getString(R.string.saved_cognitive_services_api_key), "");
    }

    private String loadSavedCognitiveServicesRegion() {
        return sharedPreferences.getString(getString(R.string.saved_cognitive_services_region), "");
    }

    private int loadSavedTextViewSize() {
        int defaultTextViewSize = getResources().getInteger(R.integer.default_textView_size);
        return sharedPreferences.getInt(getString(R.string.saved_textView_size_key), defaultTextViewSize);
    }

    private void saveTextViewSize(int newSize) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.saved_textView_size_key), newSize);
        editor.commit();
    }

    private String loadSavedGender() {
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
                        Toast.makeText(MainActivity.this, getString(R.string.toast_permission_microphone_denied), Toast.LENGTH_LONG).show();
                    } else if (RECORD_AUDIO.equals(permission)) {
                        new AlertDialog.Builder(MainActivity.this)
                            .setMessage(getString(R.string.permission_microphone_request_message))
                            .setPositiveButton("OK", (dialog, which) -> {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO}, requestCode);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                Toast.makeText(MainActivity.this, getString(R.string.toast_permission_microphone_denied), Toast.LENGTH_LONG).show();
                            })
                            .create()
                            .show();
                    }
                }
            }
        }
    }
}
