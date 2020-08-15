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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yeongzhiwei.voiceears.mirror.MirrorActivity;
import com.yeongzhiwei.voiceears.presentation.PresentationActivity;
import com.yeongzhiwei.voiceears.setting.SettingsActivity;
import com.yeongzhiwei.voiceears.ttsstt.Authentication;
import com.yeongzhiwei.voiceears.ttsstt.Gender;
import com.yeongzhiwei.voiceears.ttsstt.Recognition;
import com.yeongzhiwei.voiceears.ttsstt.Recognizer;
import com.yeongzhiwei.voiceears.ttsstt.Synthesizer;

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

    private ScrollView messageScrollView;
    private LinearLayout messageLinearLayout;
    private ImageView scrollDownImageView;
    private ImageView loadingImageView;
    private SeekBar textSizeSeekBar;
    private ImageButton clearImageButton;
    private EditText synthesizeEditText;

    AnimationDrawable loadingAnimationDrawable = null;

    private Boolean isAutoScrollDown = true;
    private Integer messageTextSize = 20;
    private Gender gender = Gender.Male;

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

    //endregion

    //region SHARED PREFERENCES

    private void savePreferences() {
        PreferencesHelper.save(this, PreferencesHelper.Key.textViewSizeKey, messageTextSize);
        PreferencesHelper.save(this, PreferencesHelper.Key.genderKey, gender.name());
    }

    private void loadSavedPreferences() {
        cognitiveServicesApiKey = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesApiKeyKey);
        cognitiveServicesRegion = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesRegionKey);
        messageTextSize = PreferencesHelper.loadInt(this, PreferencesHelper.Key.textViewSizeKey, messageTextSize);
        gender = Gender.valueOf(PreferencesHelper.loadString(this, PreferencesHelper.Key.genderKey, gender.name()));
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
        synthesizeEditText = findViewById(R.id.editText_synthesizeText);
    }

    private void initializeVariables() {
        loadingAnimationDrawable = (AnimationDrawable) loadingImageView.getBackground();
    }

    private void configureCognitiveServices() {
        if (cognitiveServicesApiKey == null || cognitiveServicesRegion == null) {
            startSettingsActivity();
            Toast.makeText(getApplicationContext(), R.string.toast_blank_key_or_region, Toast.LENGTH_LONG).show();
            return;
        }

        if (!Authentication.authenticate(cognitiveServicesApiKey, cognitiveServicesRegion)) {
            startSettingsActivity();
            Toast.makeText(getApplicationContext(), R.string.toast_invalid_key_or_region, Toast.LENGTH_LONG).show();
            return;
        }

        requestPermissionsForCognitiveServices();
        configureTextToSpeech();
    }

    //endregion

    //region COGNITIVE SERVICES

    private void configureTextToSpeech() {
        synthesizer = new Synthesizer(cognitiveServicesApiKey, cognitiveServicesRegion, gender);
    }

    private void configureSpeechToText() {
        // call this only after Microphone permission is granted and only once
        try {
            recognizer = new Recognizer(cognitiveServicesApiKey, cognitiveServicesRegion, new Recognition() {
                @Override
                public void recognizing(String text) {
                    appendIncomingMessage(text, false);
                }

                @Override
                public void recognized(String text) {
                    appendIncomingMessage(text, true);
                }
            });
            recognizer.startSpeechToText();
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Error: " + ex.getMessage());
        }
    }

    private void synthesizeText(String message) {
        if (counter.get() == 0) {
            setLoading(true);

            if (recognizer != null) {
                recognizer.stopSpeechToText();
            }
        }

        new Thread(() -> {
            new Thread(() -> {
                counter.incrementAndGet();
            }).start();

            TextView ttsTextView = appendOutgoingMessage(message);
            scrollDown();

            synthesizer.speak(message, () -> {
                MainActivity.this.runOnUiThread(() -> {
                    setLoading(false);
                    ttsTextView.setBackgroundResource(R.drawable.bg_speech_bubble_outgoing_active);
                });
            }, () -> {
                MainActivity.this.runOnUiThread(() -> {
                    ttsTextView.setBackgroundResource(R.drawable.bg_speech_bubble_outgoing);
                });

                new Thread(() -> {
                    if (counter.decrementAndGet() == 0) {
                        if (recognizer != null) {
                            recognizer.startSpeechToText();
                        }
                    }
                }).start();
            }, () -> {
                MainActivity.this.runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                });
            });
        }).start();
    }

    //endregion

    //region STATE

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

        refreshAllViews();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_mirror) {
            startMirrorActivity();
        } else if (item.getItemId() == R.id.action_settings) {
            startSettingsActivity();
        } else if (item.getItemId() == R.id.action_presentation) {
            startPresentationActivity();
        }

        return super.onOptionsItemSelected(item);
    }

    //endregion

    //region VIEWS

    // Event listeners

    private void addEventListeners() {
        messageScrollView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
//            Log.d(LOG_TAG, "scrollView2 getBottom(): " + messageScrollView.getBottom() + ". getHeight(): " + messageScrollView.getHeight() + ". getScrollY(): " + messageScrollView.getScrollY());
//            Log.d(LOG_TAG, "linearLayout2 getBottom(): " + messageLinearLayout.getBottom() + ". getHeight(): " + messageLinearLayout.getHeight() + ". getScrollY(): " + messageLinearLayout.getScrollY());
//
//            Log.d(LOG_TAG, "isAutoScrollDown: " + ((isAutoScrollDown) ? "true" : "false"));
//            Log.d(LOG_TAG, messageLinearLayout.getHeight() + " > " + messageScrollView.getHeight() + " + " + scrollY + " && " + scrollY + " < " + oldScrollY + " && " + ((isAutoScrollDown) ? "true" : "false"));
//            Log.d(LOG_TAG, messageLinearLayout.getHeight() + " == " + messageScrollView.getHeight() + " + " + scrollY);

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

        synthesizeEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                synthesizeAndRefreshSynthesizeTextView();
                return true;
            }
            return false;
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
        clearSynthesizeEditText();
        synthesizeText(message);
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
        return appendMessage(message, SpeechBubble.Outgoing, null);
    }

    private void appendIncomingMessage(final String message, final Boolean isFinal) {
        final TextView textView = appendMessage(message, SpeechBubble.Incoming, currentIncomingTextView);
        currentIncomingTextView = (isFinal) ? null : textView;
    }

    private TextView appendMessage(final String message, final SpeechBubble speechBubble) {
        return this.appendMessage(message, speechBubble, null);
    }

    private TextView appendMessage(final String message, final SpeechBubble speechBubble, final TextView textView) {
        final TextView messageTextView = (textView == null) ? createAndAppendTextView(speechBubble) : textView;

        MainActivity.this.runOnUiThread(() -> {
            messageTextView.setText(message);
            scrollToBottom();
        });

        return messageTextView;
    }

    private TextView createAndAppendTextView(final SpeechBubble speechBubble) {
        TextView textView = new TextView(this);
        textView.setTextSize(messageTextSize);
        textView.setTextIsSelectable(true);
        textView.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE); // prevents text "dancing" while speech is being recognized and added
        textView.setTag(speechBubble.toString());
        LinearLayout.LayoutParams layoutParams = null;

        if (speechBubble == SpeechBubble.Incoming) {
            textView.setBackgroundResource(R.drawable.bg_speech_bubble_incoming);
            textView.setPadding(60, 4, 24, 4);
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.rightMargin = 100;
            layoutParams.gravity = Gravity.START;
        } else if (speechBubble == SpeechBubble.Outgoing) {
            textView.setBackgroundResource(R.drawable.bg_speech_bubble_outgoing);
            textView.setPadding(24, 4, 60 , 4);
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.leftMargin = 100;
            layoutParams.gravity = Gravity.END;
        } else if (speechBubble == SpeechBubble.System) {
            textView.setBackgroundResource(R.drawable.bg_speech_bubble_system);
            textView.setPadding(60, 4, 60, 4);
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        textView.setLayoutParams(layoutParams);

        MainActivity.this.runOnUiThread(() -> {
            messageLinearLayout.addView(textView);
        });

        return textView;
    }

    // Refresh views based on state

    private void refreshAllViews() {
        refreshMessageTextSize();
        refreshScrollDownImageView();
        refreshTextSizeSeekBar();
        scrollToBottom();
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
            int bottom = messageLinearLayout.getBottom() + messageScrollView.getPaddingBottom();
            int sy = messageScrollView.getScrollY();
            int sh = messageScrollView.getHeight();
            int delta = bottom - (sy + sh);

            messageScrollView.smoothScrollBy(0, delta);
        }
        toggleAutoScrollDown(true);
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
                if (!Authentication.authenticate(cognitiveServicesApiKey, cognitiveServicesRegion)) {
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
            String[] speechBubbles = savedInstanceState.getStringArray("speechBubbles");
            if (messages != null) {
                for (int i = 0; i < messages.length; i++) {
                    appendMessage(messages[i], SpeechBubble.valueOf(speechBubbles[i]));
                }
            }
        } else {
            String welcome = getString(R.string.welcome);
            appendMessage(welcome, SpeechBubble.System);
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
        String[] speechBubbles = new String[childCount];
        for (int i = 0; i < childCount; i++) {
            TextView textView = (TextView) messageLinearLayout.getChildAt(i);
            messages[i] = textView.getText().toString();
            speechBubbles[i] = (String) textView.getTag();
        }

        outState.putStringArray("messages", messages);
        outState.putStringArray("speechBubbles", speechBubbles);
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
        if (requestCode == permissionRequestCode) {
            for (int i = 0, len = permissions.length; i < len; i++) {
                String permission = permissions[i];

                if (RECORD_AUDIO.equals(permission)) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        if (!shouldShowRequestPermissionRationale(permission)) {
                            Toast.makeText(getApplicationContext(), getString(R.string.toast_permission_microphone_denied), Toast.LENGTH_LONG).show();
                        } else {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage(getString(R.string.alert_permission_microphone_request_message))
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO}, requestCode);
                                    })
                                    .setNegativeButton("Cancel", (dialog, which) -> {
                                        Toast.makeText(getApplicationContext(), getString(R.string.toast_permission_microphone_denied), Toast.LENGTH_LONG).show();
                                    })
                                    .create()
                                    .show();
                        }
                    } else if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        configureSpeechToText();
                    }
                }

            }
        }
    }

    //endregion

    private enum SpeechBubble {
        Incoming, Outgoing, System
    }
}
