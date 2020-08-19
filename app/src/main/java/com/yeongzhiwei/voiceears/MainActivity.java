package com.yeongzhiwei.voiceears;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.yeongzhiwei.voiceears.message.Message;
import com.yeongzhiwei.voiceears.message.MessageAdapter;
import com.yeongzhiwei.voiceears.message.SnappingLinearLayoutManager;
import com.yeongzhiwei.voiceears.mirror.MirrorActivity;
import com.yeongzhiwei.voiceears.presentation.PresentationActivity;
import com.yeongzhiwei.voiceears.setting.SettingsActivity;
import com.yeongzhiwei.voiceears.ttsstt.Authentication;
import com.yeongzhiwei.voiceears.ttsstt.Gender;
import com.yeongzhiwei.voiceears.ttsstt.Recognition;
import com.yeongzhiwei.voiceears.ttsstt.Recognizer;
import com.yeongzhiwei.voiceears.ttsstt.Synthesizer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class MainActivity extends AppCompatActivity {

    //region VARIABLES

    private static final int TEXT_SIZE_SEEK_BAR_MIN_VALUE = 10;
    private static final int PERMISSION_REQUEST_CODE = 10;
    private static final int SETTINGS_REQUEST_CODE = 20;
    private static final int MIRROR_REQUEST_CODE = 30;

    private RecyclerView messageRecyclerView;
    private ImageView scrollDownImageView;
    private SeekBar textSizeSeekBar;
    private ImageButton clearImageButton;
    private EditText synthesizeEditText;

    private LinearLayoutManager messageLinearLayoutManager;
    private MessageAdapter messageAdapter;
    private int messageTextSize = 20;
    @NonNull private List<Message> messages = new ArrayList<>();
    private int currentIncomingMessageIndex = -1;

    private boolean isAutoScrollDown = true;

    private String cognitiveServicesApiKey;
    private String cognitiveServicesRegion;
    private Gender gender;
    private Synthesizer synthesizer;
    private Recognizer recognizer;
    private final AtomicInteger ttsCounter = new AtomicInteger();

    //endregion

    //region ACTIVITY LIFECYCLE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageRecyclerView = findViewById(R.id.recyclerView_message);
        scrollDownImageView = findViewById(R.id.imageView_scrollDown);
        textSizeSeekBar = findViewById(R.id.seekBar_textSize);
        clearImageButton = findViewById(R.id.imageButton_clear);
        synthesizeEditText = findViewById(R.id.editText_synthesizeText);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        messageLinearLayoutManager = new SnappingLinearLayoutManager(this);
        messageRecyclerView.setLayoutManager(messageLinearLayoutManager);
        messageAdapter = new MessageAdapter(messages, messageTextSize);
        messageRecyclerView.setAdapter(messageAdapter);
        if (messageRecyclerView.getItemAnimator() != null) {
            messageRecyclerView.getItemAnimator().setChangeDuration(0); // Prevent flickering when it's constantly refreshed by incoming text
        }

        loadSavedPreferences();
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

    //region ACTIONBAR OPTIONS

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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

    //region SHARED PREFERENCES

    private void loadSavedPreferences() {
        cognitiveServicesApiKey = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesApiKeyKey);
        cognitiveServicesRegion = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesRegionKey);
        gender = Gender.valueOf(PreferencesHelper.loadString(this, PreferencesHelper.Key.genderKey));
        messageTextSize = PreferencesHelper.loadInt(this, PreferencesHelper.Key.textViewSizeKey, messageTextSize);
    }

    private void savePreferences() {
        PreferencesHelper.save(this, PreferencesHelper.Key.textViewSizeKey, messageTextSize);
    }

    //endregion

    //region COGNITIVE SERVICES

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

    private void configureTextToSpeech() {
        synthesizer = new Synthesizer(cognitiveServicesApiKey, cognitiveServicesRegion, gender);
    }

    private void synthesizeText(String text) {
        if (ttsCounter.get() == 0) {
            if (recognizer != null) {
                recognizer.stopSpeechToText();
            }
        }

        new Thread(ttsCounter::incrementAndGet).start();

        int outgoingMessageIndex = addMessage(text, Message.Type.Outgoing);
        scrollDown();

        new Thread(() -> {
            synthesizer.speak(text,
                () -> {
                    MainActivity.this.runOnUiThread(() -> updateMessageType(outgoingMessageIndex, Message.Type.OutgoingActive));
                }, () -> {
                    MainActivity.this.runOnUiThread(() -> updateMessageType(outgoingMessageIndex, Message.Type.Outgoing));

                    new Thread(() -> {
                        if (ttsCounter.decrementAndGet() == 0) {
                            if (recognizer != null) {
                                recognizer.startSpeechToText();
                            }
                        }
                    }).start();
                }, () -> {
                    MainActivity.this.runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                    });
                }
            );
        }).start();
    }

    // call this only after Microphone permission is granted and only once
    private void configureSpeechToText() {
        recognizer = new Recognizer(cognitiveServicesApiKey, cognitiveServicesRegion, new Recognition() {
            @Override
            public void recognizing(String text) {
                recognizeSpeech(text, false);
            }

            @Override
            public void recognized(String text) {
                recognizeSpeech(text, true);
            }
        });
        recognizer.startSpeechToText();
    }

    private void recognizeSpeech(final String text, final boolean isFinal) {
        MainActivity.this.runOnUiThread(() -> {
            if (currentIncomingMessageIndex != -1) {
                updateMessageText(currentIncomingMessageIndex, text);
            } else {
                currentIncomingMessageIndex = addMessage(text, Message.Type.Incoming);
            }
            scrollDownIfEnabled();

            if (isFinal) {
                currentIncomingMessageIndex = -1;
            }
        });
    }

    //endregion

    //region STATE

    // Set variables

    private void setAutoScrollDown(boolean isEnabled) {
        isAutoScrollDown = isEnabled;
        refreshScrollDownImageView();
    }

    private void setMessageTextSize(int size) {
        messageTextSize = size;
        refreshMessageTextSize();
        scrollDownIfEnabled();
    }

    // Update messages list

    private synchronized int addMessage(String text, Message.Type type) {
        int index = messages.size();
        messages.add(new Message(text, type));
        messageAdapter.notifyItemInserted(index);
        return index;
    }

    private void updateMessageText(int index, String text) {
        updateMessage(index, text, null);
    }

    private void updateMessageType(int index, Message.Type type) {
        updateMessage(index, null, type);
    }

    private void updateMessage(int index, String text, Message.Type type) {
        Message message = messages.get(index);

        if (text != null) {
            message.setMessage(text);
        }

        if (type != null) {
            message.setType(type);
        }

        messageAdapter.notifyItemChanged(index);
        scrollDownIfEnabled();
    }

    //endregion

    //region VIEWS

    // Event listeners

    private void addEventListeners() {
        messageRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy < 0) {
                    setAutoScrollDown(false);
                } else {
                    int lastMessageIndex = messages.size() - 1;
                    if (messageLinearLayoutManager != null && messageLinearLayoutManager.findLastVisibleItemPosition() == lastMessageIndex) {
                        View lastMessageView = messageLinearLayoutManager.findViewByPosition(lastMessageIndex);
                        if (lastMessageView != null && recyclerView.getHeight() == lastMessageView.getBottom()) {
                            setAutoScrollDown(true);
                        }
                    }
                }
            }
        });

        scrollDownImageView.setOnClickListener(view -> {
            scrollDown();
        });

        textSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setMessageTextSize(i + TEXT_SIZE_SEEK_BAR_MIN_VALUE);
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
        int lastSpaceIndex = originalText.lastIndexOf(" ");

        if (lastSpaceIndex != -1) {
            if (lastSpaceIndex + 1 != originalText.length()) {
                lastSpaceIndex += 1;
            }
            synthesizeEditText.setText(originalText.substring(0, lastSpaceIndex));
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

    // Refresh views based on state

    private void refreshAllViews() {
        refreshMessageTextSize();
        refreshScrollDownImageView();
        refreshTextSizeSeekBar();
        scrollDown();
    }

    private void refreshMessageTextSize() {
        if (messageAdapter != null && messageRecyclerView != null) {
            messageAdapter.setMessageTextSize(messageTextSize);
            messageRecyclerView.setAdapter(messageAdapter); // Recreate all view items
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
            textSizeSeekBar.setProgress(messageTextSize - TEXT_SIZE_SEEK_BAR_MIN_VALUE);
        }
    }

    private void scrollDownIfEnabled() {
        if (isAutoScrollDown) {
            scrollDown();
        }
    }

    private void scrollDown() {
        if (messageRecyclerView != null && messages.size() != 0) {
            messageRecyclerView.smoothScrollToPosition(messages.size() - 1);
        }
    }

    //endregion

    //region ACTIVITY INTENT

    private void startSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, SETTINGS_REQUEST_CODE);
    }

    private void startMirrorActivity() {
        String message = synthesizeEditText.getText().toString();
        PreferencesHelper.save(this, PreferencesHelper.Key.mirroredTextKey, message);
        Intent intent = new Intent(this, MirrorActivity.class);
        startActivityForResult(intent, MIRROR_REQUEST_CODE);
    }

    private void startPresentationActivity() {
        Intent intent = new Intent(this, PresentationActivity.class);
        startActivity(intent);
    }

    public void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        loadSavedPreferences();

        if (requestCode == SETTINGS_REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                if (!Authentication.authenticate(cognitiveServicesApiKey, cognitiveServicesRegion)) {
                    finish();
                }
            } else if (resultCode == RESULT_OK) {
                configureCognitiveServices();
            }
        }

        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    //endregion

    //region INSTANCE STATE

    private void loadSavedInstanceState(final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            List<Message> savedMessages = savedInstanceState.getParcelableArrayList("messages");
            if (savedMessages != null) {
                messages = savedMessages;
            }
            messageAdapter.setMessages(messages);
        } else {
            String welcomeText = getString(R.string.welcome);
            addMessage(welcomeText, Message.Type.System);
            scrollDown();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        ArrayList<Message> messageArrayList = new ArrayList<>(messages);
        outState.putParcelableArrayList("messages", messageArrayList);
    }

    //endregion

    //region PERMISSIONS

    private void requestPermissionsForCognitiveServices() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // https://stackoverflow.com/questions/30719047/android-m-check-runtime-permission-how-to-determine-if-the-user-checked-nev
        if (requestCode == PERMISSION_REQUEST_CODE) {
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

}
