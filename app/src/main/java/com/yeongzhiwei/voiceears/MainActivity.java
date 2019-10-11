package com.yeongzhiwei.voiceears;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicInteger;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class MainActivity extends AppCompatActivity {
    //region VARIABLES
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    public static int permissionRequestCode = 10;
    public static int settingsRequestCode = 20;
    public static int mirrorRequestCode = 30;
    private static final Integer seekBarMinValue = 10;
    private static final Double minAudioSpeed = 0.75;
    private static final Double maxAudioSpeed = 1.50;
    private static final Double incrementAudioSpeed = 0.25;

    private MenuItem genderMenuItem;
    private MenuItem speedMenuItem;

    private ScrollView messageScrollView;
    private LinearLayout messageLinearLayout;
    private ImageView scrollDownImageView;
    private ImageView loadingImageView;
    private SeekBar textSizeSeekBar;
    private ImageButton clearImageButton;
    private EditText synthesizeEditText;
    private Button synthesizeButton;

    AnimationDrawable loadingAnimationDrawable = null;

    private Boolean isAutoScrollDown = true;
    private Integer messageTextSize = 20;
    private Voice.Gender gender = Voice.Gender.Male;
    private Double audioSpeed = 1.0;
    private Boolean isAutoTTS = false;

    // Azure Cognitive Services
    private static String cognitiveServicesApiKey;
    private static String cognitiveServicesRegion;
    private Synthesizer synthesizer = null;
    private Recognizer recognizer = null;
    private AtomicInteger counter = new AtomicInteger();

    private TextView currentIncomingTextView;

    //endregion

    //region ACTIVITY LIFECYCLE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadSavedPreferences();
        initializeViews();
        initializeVariables();
        refreshAllViews();
        addEventListeners();
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
    protected void onPause() {
        super.onPause();

        savePreferences();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.stopSpeechToTextAndReleaseMicrophone();
        }
    }

    //endregion

    //region SHARED PREFERENCES

    private void savePreferences() {
        PreferencesHelper.save(this, PreferencesHelper.Key.textViewSizeKey, messageTextSize);
        PreferencesHelper.save(this, PreferencesHelper.Key.genderKey, gender.name());
        PreferencesHelper.save(this, PreferencesHelper.Key.audioSpeedKey, (int) (audioSpeed * 100));
        PreferencesHelper.save(this, PreferencesHelper.Key.autoTTSKey, (isAutoTTS) ? 1 : 0);
    }

    private void loadSavedPreferences() {
        cognitiveServicesApiKey = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesApiKeyKey);
        cognitiveServicesRegion = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesRegionKey);
        messageTextSize = PreferencesHelper.loadInt(this, PreferencesHelper.Key.textViewSizeKey, messageTextSize);
        gender = Voice.Gender.valueOf(PreferencesHelper.loadString(this, PreferencesHelper.Key.genderKey, gender.name()));
        audioSpeed = PreferencesHelper.loadInt(this, PreferencesHelper.Key.audioSpeedKey, 100) / 100.0;
        isAutoTTS = ((PreferencesHelper.loadInt(this, PreferencesHelper.Key.autoTTSKey, 0)) > 0) ? true : false;
    }

    //endregion

    //region INITIALIZATION

    private void initializeViews() {
        messageScrollView = findViewById(R.id.scrollView_message);
        messageLinearLayout = findViewById(R.id.linearLayout_message);
        scrollDownImageView = findViewById(R.id.imageView_scrollDown);
        loadingImageView = findViewById(R.id.imageView_loading);
        textSizeSeekBar = findViewById(R.id.seekBar_textSize);
        clearImageButton = findViewById(R.id.imageButton_clear);
        synthesizeEditText = findViewById(R.id.editText_synthesize);
        synthesizeButton = findViewById(R.id.button_synthesize);
    }

    private void initializeVariables() {
        loadingAnimationDrawable = (AnimationDrawable) loadingImageView.getBackground();
    }

    private void configureCognitiveServices() {
        if (cognitiveServicesApiKey == null || cognitiveServicesRegion == null) {
            startSettingsActivity();
            Toast.makeText(MainActivity.this, R.string.toast_blank_key_or_region, Toast.LENGTH_LONG).show();
            return;
        }

        if (! Helper.authenticateApiKey(this)) {
            startSettingsActivity();
            Toast.makeText(MainActivity.this, R.string.toast_invalid_key_or_region, Toast.LENGTH_LONG).show();
            return;
        }

        requestPermissionsForCognitiveServices();
        configureTextToSpeech();
        configureSpeechToText();
    }

    //endregion

    //region COGNITIVE SERVICES

    private void configureTextToSpeech() {
        synthesizer = new Synthesizer(cognitiveServicesApiKey, cognitiveServicesRegion, Voice.getDefaultVoice(gender));
    }

    private void configureSpeechToText() {
        try {
            recognizer = new Recognizer(cognitiveServicesApiKey, cognitiveServicesRegion, this::appendIncomingMessage);
            recognizer.startSpeechToText();
        } catch (Exception ex) {

        }
    }

    private void synthesizeText(String message) {
        setLoading(true);
        new Thread(() -> {
            if (recognizer != null) {
                recognizer.stopSpeechToText();
            }

            Thread runBeforeAudio = new Thread(() -> {
                counter.incrementAndGet();
            });
            runBeforeAudio.start();

            TextView ttsTextView = appendOutgoingMessage(message);
            synthesizer.speakToAudio(message, audioSpeed, () -> {
                MainActivity.this.runOnUiThread(() -> {
                    setLoading(false);
                    ttsTextView.setBackgroundResource(R.drawable.bg_speech_bubble_outgoing_active);
                });
            }, () -> {
                MainActivity.this.runOnUiThread(() -> {
                    ttsTextView.setBackgroundResource(R.drawable.bg_speech_bubble_outgoing);
                });

                Thread runAfterAudio = new Thread(() -> {
                    if (counter.decrementAndGet() == 0) {
                        if (recognizer != null) {
                            recognizer.startSpeechToText();
                        }
                    }
                });
                runAfterAudio.start();
            });
        }).start();
    }

    //endregion

    //region STATE

    private void toggleGender() {
        if (synthesizer == null) {
            return;
        }

        gender = synthesizer.getVoice().toggleVoice();
        refreshGenderIcon();
    }

    private void toggleAudioSpeed() {
        if (audioSpeed >= maxAudioSpeed) {
            audioSpeed = minAudioSpeed;
        } else {
            audioSpeed += incrementAudioSpeed;
        }

        refreshAudioSpeedIcon();
    }

    private void toggleAutoTTS() {
        isAutoTTS = !isAutoTTS;

        refreshAutoTTSViews();
    }

    private void toggleAutoScrollDown(Boolean isEnabled) {
        isAutoScrollDown = isEnabled;

        refreshScrollDownImageView();
    }

    private void setMessageTextSize(Integer size) {
        messageTextSize = size;
        refreshMessageTextSize();
        scrollToBottom();
    }

    //endregion

    //region ACTIONBAR OPTIONS

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // https://stackoverflow.com/questions/38158953/how-to-create-button-in-action-bar-in-android
        getMenuInflater().inflate(R.menu.main_menu, menu);

        genderMenuItem = menu.findItem(R.id.action_gender);
        speedMenuItem = menu.findItem(R.id.action_audio_speed);
        refreshAllViews();

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
        } else if (item.getItemId() == R.id.action_audio_speed) {
            toggleAudioSpeed();
        } else if (item.getItemId() == R.id.action_presentation) {
            startPresentationActivity();
        } else if (item.getItemId() == R.id.action_toggle_auto_tts) {
            toggleAutoTTS();
        }

        return super.onOptionsItemSelected(item);
    }

    //endregion

    //region VIEWS

    // Event listeners

    private void addEventListeners() {
        messageScrollView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            Log.d(LOG_TAG, "scrollView2 getBottom(): " + messageScrollView.getBottom() + ". getHeight(): " + messageScrollView.getHeight() + ". getScrollY(): " + messageScrollView.getScrollY());
            Log.d(LOG_TAG, "linearLayout2 getBottom(): " + messageLinearLayout.getBottom() + ". getHeight(): " + messageLinearLayout.getHeight() + ". getScrollY(): " + messageLinearLayout.getScrollY());

            Log.d(LOG_TAG, "isAutoScrollDown: " + ((isAutoScrollDown) ? "true" : "false"));
            Log.d(LOG_TAG, messageLinearLayout.getHeight() + " > " + messageScrollView.getHeight() + " + " + scrollY + " && " + scrollY + " < " + oldScrollY + " && " + ((isAutoScrollDown) ? "true" : "false"));
            Log.d(LOG_TAG, messageLinearLayout.getHeight() + " == " + messageScrollView.getHeight() + " + " + scrollY);

            if (messageLinearLayout.getHeight() > messageScrollView.getHeight() + scrollY && scrollY < oldScrollY && isAutoScrollDown) {
                toggleAutoScrollDown(false);
            } else if (messageLinearLayout.getHeight() == messageScrollView.getHeight() + scrollY) {
                toggleAutoScrollDown(true);
            }
        });

        scrollDownImageView.setOnClickListener(view -> {
            scrollDown();
        });

        textSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setMessageTextSize(i + seekBarMinValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        clearImageButton.setOnClickListener(view -> {
            removeLastWordFromSynthesizeEditText();
        });

        clearImageButton.setOnLongClickListener(view -> {
            clearSynthesizeEditText();
            return true;
        });

        synthesizeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (isAutoTTS) {
                    synthesizeAndRefreshSynthesizeTextView();
                } else {
                    refreshSynthesizeButton();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        synthesizeButton.setOnClickListener(view -> {
            synthesizeAndRefreshSynthesizeTextView();
        });
    }

    private void removeLastWordFromSynthesizeEditText() {
        String originalText = synthesizeEditText.getText().toString().replaceFirst(" +$", "");
        int lastIndexOfSpace = originalText.lastIndexOf(" ");

        if (lastIndexOfSpace != -1) {
            if (lastIndexOfSpace + 1 != originalText.length()) {
                lastIndexOfSpace += 1;
            }
            synthesizeEditText.setText(originalText.substring(0, lastIndexOfSpace));
        } else {
            synthesizeEditText.setText("");
        }

        synthesizeEditText.setSelection(synthesizeEditText.getText().length());
    }

    private void clearSynthesizeEditText() {
        synthesizeEditText.setText("");
    }

    private void synthesizeAndRefreshSynthesizeTextView() {
        String message = synthesizeEditText.getText().toString().trim();

        if (isAutoTTS) {
            int lastIndex = Math.max(message.lastIndexOf("."), Math.max(message.lastIndexOf("?"), message.lastIndexOf("!")));
            if (lastIndex == 0) {
                synthesizeEditText.setText(""); // forbid [.?!] as first character
            } else if (lastIndex != -1) {
                lastIndex += 1;
                synthesizeEditText.setText(message.substring(lastIndex));
                synthesizeText(message.substring(0, lastIndex).trim());
            }
        } else {
            synthesizeEditText.setText("");
            synthesizeText(message);
        }
    }

    // Update views based on parameters

    private void setLoading(boolean load) {

        if (load) {
            loadingImageView.setVisibility(View.VISIBLE);
            loadingAnimationDrawable.start();
        } else {
            loadingImageView.setVisibility(View.GONE);
            loadingAnimationDrawable.stop();
        }
    }

    private TextView appendOutgoingMessage(final String message) {
        return appendMessage(message);
    }

    private void appendIncomingMessage(final String message, final Boolean isFinal) {
        final TextView textView = appendMessage(message, currentIncomingTextView, true);
        currentIncomingTextView = (isFinal) ? null : textView;
    }

    private TextView appendMessage(final String message) {
        return this.appendMessage(message, null, false);
    }

    private TextView appendMessage(final String message, final TextView textView, final Boolean isLeft) {
        final TextView messageTextView = (textView == null) ? createAndAppendTextView(isLeft) : textView;

        MainActivity.this.runOnUiThread(() -> {
            messageTextView.setText(message);
            scrollToBottom();
        });

        return messageTextView;
    }

    private TextView createAndAppendTextView(final Boolean isLeft) {
        TextView textView = new TextView(this);
        textView.setTextSize(messageTextSize);
        textView.setTextIsSelectable(true);
        textView.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE); // prevents text "dancing" while speech is being recognized and added
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        if (isLeft) {
            textView.setBackgroundResource(R.drawable.bg_speech_bubble_incoming);
            textView.setPadding(60, 4, 24, 4);
            layoutParams.rightMargin = 100;
            layoutParams.gravity = Gravity.START;
        } else {
            textView.setBackgroundResource(R.drawable.bg_speech_bubble_outgoing);
            textView.setPadding(24, 4, 60 , 4);
            layoutParams.leftMargin = 100;
            layoutParams.gravity = Gravity.END;
        }

        textView.setLayoutParams(layoutParams);

        MainActivity.this.runOnUiThread(() -> {
            messageLinearLayout.addView(textView);
            scrollToBottom();
        });

        return textView;
    }

    // Refresh views based on state

    private void refreshAllViews() {
        refreshGenderIcon();
        refreshAudioSpeedIcon();
        refreshAutoTTSViews();
        refreshMessageTextSize();
        refreshScrollDownImageView();
        refreshTextSizeSeekBar();
        refreshSynthesizeButton();
        scrollToBottom();
    }

    private void refreshGenderIcon() {
        if (genderMenuItem != null) {
            if (gender == Voice.Gender.Male) {
                genderMenuItem.setIcon(R.drawable.ic_male);
                genderMenuItem.setTitle(R.string.action_gender_title_male);
            } else {
                genderMenuItem.setIcon(R.drawable.ic_female);
                genderMenuItem.setTitle(R.string.action_gender_title_female);
            }
        }
    }

    private void refreshAudioSpeedIcon() {
        if (speedMenuItem != null) {
            String title = String.format( "%s: %sx", getResources().getString(R.string.action_audio_speed_title), Double.toString(audioSpeed));
            speedMenuItem.setTitle(title);
        }
    }

    private void refreshAutoTTSViews() {
        if (synthesizeButton != null) {
            if (isAutoTTS) {
                synthesizeButton.setVisibility(View.GONE);
                clearImageButton.setVisibility(View.VISIBLE);
            } else {
                synthesizeButton.setVisibility(View.VISIBLE);
                clearImageButton.setVisibility(View.GONE);
            }
        }
    }

    private void refreshMessageTextSize() {
        if (messageLinearLayout != null) {
            int childCount = messageLinearLayout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                TextView textView = (TextView) messageLinearLayout.getChildAt(i);
                textView.setTextSize(messageTextSize);
            }
        }
    }

    private void refreshScrollDownImageView() {
        if (scrollDownImageView != null) {
            if (isAutoScrollDown) {
                scrollDownImageView.setVisibility(View.GONE);
            } else {
                scrollDownImageView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void refreshTextSizeSeekBar() {
        if (textSizeSeekBar != null) {
            textSizeSeekBar.setProgress(messageTextSize - seekBarMinValue);
        }
    }

    private void refreshSynthesizeButton() {
        if (synthesizeEditText != null && synthesizeButton != null) {
            if (synthesizeEditText.getText().toString().trim().length() == 0) {
                synthesizeButton.setEnabled(false);
            } else {
                synthesizeButton.setEnabled(true);
            }
        }
    }

    private void scrollToBottom() {
        // https://stackoverflow.com/questions/28105945/add-view-to-scrollview-and-then-scroll-to-bottom-which-callback-is-needed
        if (messageScrollView != null) {
            messageScrollView.postDelayed(() -> {
                if (isAutoScrollDown) {
                    scrollDown();
                }
            }, 200);
        }
    }

    private void scrollDown() {
        if (messageScrollView != null && messageLinearLayout != null) {
            messageScrollView.smoothScrollBy(0, messageScrollView.getHeight() + messageLinearLayout.getHeight() - messageScrollView.getScrollY());
        }
    }

    //endregion

    //region ACTIVITY INTENT

    private void startSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, settingsRequestCode);
    }

    private void startMirrorActivity() {
        String message = synthesizeEditText.getText().toString();
        PreferencesHelper.save(this, PreferencesHelper.Key.mirroredTextKey, message);
        Intent intent = new Intent(this, MirrorActivity.class);
        startActivityForResult(intent, mirrorRequestCode);
    }

    private void startPresentationActivity() {
        Intent intent = new Intent(this, PresentationActivity.class);
        startActivity(intent);
    }

    public void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        loadSavedPreferences();

        if (requestCode == settingsRequestCode) {
            if (resultCode == RESULT_CANCELED) {
                if (!Helper.authenticateApiKey(this)) {
                    finish();
                }
            } else if (resultCode == RESULT_OK) {
                configureCognitiveServices();
            }
        } else if (requestCode == mirrorRequestCode) {

        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    //endregion

    //region LAYOUT PERSISTENCE ON ROTATION

    private void loadSavedInstanceState(final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String[] messages = savedInstanceState.getStringArray("messages");
            if (messages != null) {
                for (String message : messages) {
                    appendMessage(message);
                }
            }
        } else {
            String welcome = getString(R.string.welcome);
            appendMessage(welcome);
        }
    }

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

    //endregion

    //region PERMISSIONS

    private void requestPermissionsForCognitiveServices() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, permissionRequestCode);
    }

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

    //endregion
}
