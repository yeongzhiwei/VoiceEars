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
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
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
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    static SharedPreferences sharedPreferences;
    public static int permissionRequestCode = 10;
    public static int settingsRequestCode = 20;
    public static int mirrorRequestCode = 30;

    private MenuItem genderMenuItem;
    private ScrollView messageScrollView;
    private LinearLayout messageLinearLayout;
    private SeekBar textSizeSeekBar;
    private ImageView imageViewScrollDown;
    private EditText synthesizeEditText;
    private Button synthesizeButton;

    private final Integer seekBarMinValue = 10;
    private Integer textViewSize = 12; // default
    private Boolean enableAutoScrollDown = true;

    // Cognitive Services
    private static String cognitiveServicesApiKey;
    private static String cognitiveServicesRegion;
    // Text-to-Speech
    private Synthesizer synthesizer = null;
    private Counter counter = new Counter();
    PaintDrawable paintDrawable = null;
    private Voice.Gender gender = Voice.Gender.Male; // default
    // Speech-to-Text
    private Recognizer recognizer = null;
    private HashMap<Integer, TextView> recognizerTextViews = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(PreferencesHelper.sharedPreferencesName, MODE_PRIVATE);

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

        savePreferences();
    }

    private void savePreferences() {
        PreferencesHelper.save(sharedPreferences, PreferencesHelper.Key.textViewSizeKey, textViewSize);
        PreferencesHelper.save(sharedPreferences, PreferencesHelper.Key.genderKey, gender.name());
    }

    private void loadSavedPreferences() {
        cognitiveServicesApiKey = PreferencesHelper.loadString(sharedPreferences, PreferencesHelper.Key.cognitiveServicesApiKeyKey);
        cognitiveServicesRegion = PreferencesHelper.loadString(sharedPreferences, PreferencesHelper.Key.cognitiveServicesRegionKey);
        textViewSize = PreferencesHelper.loadInt(sharedPreferences, PreferencesHelper.Key.textViewSizeKey, textViewSize);
        gender = Voice.Gender.valueOf(PreferencesHelper.loadString(sharedPreferences, PreferencesHelper.Key.genderKey, gender.name()));
    }

    private void initializeViews() {
        messageScrollView = findViewById(R.id.scrollView_textView);
        messageLinearLayout = findViewById(R.id.message_linearLayout);
        textSizeSeekBar = findViewById(R.id.seekBar_textSize);
        imageViewScrollDown = findViewById(R.id.imageView_scrolldown);
        synthesizeEditText = findViewById(R.id.editText_synthesize);
        synthesizeButton = findViewById(R.id.button_synthesize);
    }

    private void configureViews() {
        refreshGenderIcon();

        messageScrollView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            Log.d(LOG_TAG, "scrollView2 getBottom(): " + messageScrollView.getBottom() + ". getHeight(): " + messageScrollView.getHeight() + ". getScrollY(): " + messageScrollView.getScrollY());
            Log.d(LOG_TAG, "linearLayout2 getBottom(): " + messageLinearLayout.getBottom() + ". getHeight(): " + messageLinearLayout.getHeight() + ". getScrollY(): " + messageLinearLayout.getScrollY());

            if (messageLinearLayout.getHeight() > messageScrollView.getHeight() + scrollY && scrollY < oldScrollY && enableAutoScrollDown) {
                enableAutoScrollDown = false;
                imageViewScrollDown.setVisibility(View.VISIBLE);
            } else if (messageLinearLayout.getHeight() == messageScrollView.getHeight() + scrollY) {
                Log.d(LOG_TAG, "ENABLED");
                enableAutoScrollDown = true;
                imageViewScrollDown.setVisibility(View.GONE);
            }
        });

        imageViewScrollDown.setOnClickListener(view -> {
            scrollDown();
        });

        textSizeSeekBar.setProgress(textViewSize - seekBarMinValue);

        textSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                textViewSize = i + seekBarMinValue;
                setTextViewSize();
                scrollToBottom();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

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
            String welcome = getString(R.string.welcome);
            createAndAddTextView(welcome);
        }
    }

    private void configureCognitiveServices() {
        if (cognitiveServicesApiKey == null || cognitiveServicesRegion == null) {
            startSettingsActivity();
            Toast.makeText(MainActivity.this, R.string.toast_blank_key_or_region, Toast.LENGTH_LONG).show();
            return;
        }

        if (! Helper.authenticateApiKey(sharedPreferences)) {
            startSettingsActivity();
            Toast.makeText(MainActivity.this, R.string.toast_invalid_key_or_region, Toast.LENGTH_LONG).show();
            return;
        }

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, permissionRequestCode);


        configureTextToSpeech();
        configureSpeechToText();
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
            recognizer = new Recognizer(cognitiveServicesApiKey, cognitiveServicesRegion, (order, result) -> {
                appendSpeakerMessage(order, result);
            });
            recognizer.startSpeechToText();
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
        newTextView.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE); // prevents text "dancing" while speech is being recognized and added
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
    }

    private void scrollDown() {
        messageScrollView.smoothScrollBy(0, messageScrollView.getHeight() + messageLinearLayout.getHeight() - messageScrollView.getScrollY());
    }

    /* TEXT SIZE SEEKBAR */

    private void setTextViewSize() {
        int childcount = messageLinearLayout.getChildCount();
        for (int i = 0; i < childcount; i++) {
            TextView textView = (TextView) messageLinearLayout.getChildAt(i);
            textView.setTextSize(textViewSize);
        }
    }

    /* ACTIONBAR OPTIONS */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // https://stackoverflow.com/questions/38158953/how-to-create-button-in-action-bar-in-android
        getMenuInflater().inflate(R.menu.main_menu, menu);

        genderMenuItem = menu.findItem(R.id.action_gender);
        refreshGenderIcon();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_mirror) {
            startMirrorActivity();
        } else if (item.getItemId() == R.id.action_gender) {
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

    /* ACTIVITY INTENT */

    private void startSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, settingsRequestCode);
    }

    private void startMirrorActivity() {
        String message = synthesizeEditText.getText().toString();
        PreferencesHelper.save(sharedPreferences, PreferencesHelper.Key.mirroredTextKey, message);
        Intent intent = new Intent(this, MirrorActivity.class);
        startActivityForResult(intent, mirrorRequestCode);
    }

    public void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        loadSavedPreferences();

        if (requestCode == settingsRequestCode) {
            if (resultCode == RESULT_CANCELED) {
                if (!Helper.authenticateApiKey(sharedPreferences)) {
                    finish();
                }
            } else if (resultCode == RESULT_OK) {
                configureCognitiveServices();
            }
        } else if (requestCode == mirrorRequestCode) {

        }

        InputMethodManager imm = (InputMethodManager)   getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
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

    /* PERMISSION */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // https://stackoverflow.com/questions/30719047/android-m-check-runtime-permission-how-to-determine-if-the-user-checked-nev
        if (requestCode == this.permissionRequestCode) {
            for (int i = 0, len = permissions.length; i < len; i++) {
                String permission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    boolean shouldShowRationale = shouldShowRequestPermissionRationale(permission);
                    if (! shouldShowRationale) {
                        Toast.makeText(MainActivity.this, getString(R.string.toast_permission_microphone_denied), Toast.LENGTH_LONG).show();
                    } else if (RECORD_AUDIO.equals(permission)) {
                        new AlertDialog.Builder(MainActivity.this)
                            .setMessage(getString(R.string.alert_permission_microphone_request_message))
                            .setPositiveButton("OK", (dialog, which) -> {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO}, requestCode);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                Toast.makeText(MainActivity.this, getString(R.string.toast_permission_microphone_denied), Toast.LENGTH_LONG).show();
                            })
                            .create()
                            .show();
                    }
                } else if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if (recognizer != null) {
                        recognizer.startSpeechToText();
                    }
                }
            }
        }
    }
}
