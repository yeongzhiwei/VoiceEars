package com.yeongzhiwei.voiceears.presentation;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yeongzhiwei.voiceears.MainActivity;
import com.yeongzhiwei.voiceears.PreferencesHelper;
import com.yeongzhiwei.voiceears.R;
import com.yeongzhiwei.voiceears.ttsstt.Gender;
import com.yeongzhiwei.voiceears.ttsstt.Synthesizer;

import java.util.ArrayList;

public class PresentationActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    public static final String EXTRA_MESSAGE = "com.yeongzhiwei.voiceears.MESSAGE";
    public static final String EXTRA_REQUEST_CODE = "com.yeongzhiwei.voiceears.REQUESTCODE";
    public static final int addRequestCode = 100;
    public static final int editRequestCode = 200;

    private FloatingActionButton playFloatingActionButton;
    private FloatingActionButton addFloatingActionButton;
    private LinearLayout messageLinearLayout;

    private PaintDrawable paintDrawableSelect = null;
    private PaintDrawable paintDrawablePlay = null;

    private Integer messageTextSize = 20;
    private Gender gender = Gender.Male;
    private Boolean isPlaying = false;
    private ArrayList<String> messages = new ArrayList<>();
    private int selectedMessageIndex;

    // Azure Cognitive Services
    private String cognitiveServicesApiKey;
    private String cognitiveServicesRegion;
    private Synthesizer synthesizer = null;

    //region ACTIVITY LIFECYCLE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation);

        loadSavedPreferences();
        initializeViews();
        initializeVariables();
        refreshAllViews();
        addEventListeners();
        configureTextToSpeech();
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
        PreferencesHelper.save(this, PreferencesHelper.Key.presentationMessagesKey, messages);
        PreferencesHelper.save(this, PreferencesHelper.Key.presentationSelectedMessageIndexKey, selectedMessageIndex);
    }

    private void loadSavedPreferences() {
        cognitiveServicesApiKey = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesApiKeyKey, "");
        cognitiveServicesRegion = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesRegionKey, "");
        messageTextSize = PreferencesHelper.loadInt(this, PreferencesHelper.Key.textViewSizeKey, messageTextSize);
        gender = Gender.valueOf(PreferencesHelper.loadString(this, PreferencesHelper.Key.genderKey, gender.name()));
        messages = PreferencesHelper.loadStringArray(this, PreferencesHelper.Key.presentationMessagesKey, new ArrayList<>());
        selectedMessageIndex = PreferencesHelper.loadInt(this, PreferencesHelper.Key.presentationSelectedMessageIndexKey, -1);
    }

    //endregion

    //region INITIALIZATION

    private void initializeViews() {
        playFloatingActionButton = findViewById(R.id.fab_play);
        addFloatingActionButton = findViewById(R.id.fab_add);
        messageLinearLayout = findViewById(R.id.linearLayout_message);
    }

    private void initializeVariables() {
        paintDrawableSelect = new PaintDrawable(ContextCompat.getColor(this, R.color.colorPrimary));
        paintDrawableSelect.setCornerRadius(8);

        paintDrawablePlay = new PaintDrawable(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        paintDrawablePlay.setCornerRadius(8);
    }

    private void configureTextToSpeech() {
        synthesizer = new Synthesizer(cognitiveServicesApiKey, cognitiveServicesRegion, gender);
    }

    //endregion

    //region COGNITIVE SERVICES

    private void synthesizeText() {
        if (selectedMessageIndex < 0 || selectedMessageIndex >= messages.size()) {
            return;
        }

        isPlaying = true;
        refreshButtons();

        String message = messages.get(selectedMessageIndex);

        new Thread(() -> {
            synthesizer.speak(message, () -> {
                PresentationActivity.this.runOnUiThread(() -> {
                    refreshTextViewsBackground();
                });
            }, () -> {
                PresentationActivity.this.runOnUiThread(() -> {
                    isPlaying = false;
                    selectedMessageIndex += 1;
                    refreshTextViewsBackground();
                    refreshButtons();
                });
            }, () -> {
                PresentationActivity.this.runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                });
            });
        }).start();
    }

    //endregion

    //region STATE

    private void addMessage(String message) {
        String[] newMessages = message.replaceAll("\n", "").split("[.!?]");
        for (String newMessage : newMessages) {
            addSingleMessage(newMessage.trim());
        }
        Toast.makeText(getApplicationContext(), "Added a message.", Toast.LENGTH_SHORT).show();

        refreshTextViews();
    }

    private void addSingleMessage(String message) {
        if (selectedMessageIndex >= 0 && selectedMessageIndex < messages.size()) {
            selectedMessageIndex += 1;
        } else {
            selectedMessageIndex = messages.size();
        }
        messages.add(selectedMessageIndex, message);
    }

    private void editCurrentMessage(String newMessage) {
        messages.set(selectedMessageIndex, newMessage);
        Toast.makeText(getApplicationContext(), "Edited a message.", Toast.LENGTH_SHORT).show();

        refreshTextViews();
    }

    private void deleteCurrentMessage() {
        String message = messages.get(selectedMessageIndex);
        String alertMessage = message.length() < 20 ? message : message.substring(0, 20) + "...";

        new AlertDialog.Builder(PresentationActivity.this)
                .setMessage("Do you want to delete this message?\n\n" + alertMessage)
                .setPositiveButton("Yes", (dialog, which) -> {
                    messages.remove(selectedMessageIndex);
                    refreshTextViews();
                    Toast.makeText(getApplicationContext(), "Deleted a message.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", (dialog, which) -> {

                })
                .create()
                .show();
    }

    private void deleteAllMessages() {
        new AlertDialog.Builder(PresentationActivity.this)
                .setMessage("Do you want to delete all messages?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    messages.clear();
                    selectedMessageIndex = 0;
                    refreshTextViews();
                    Toast.makeText(getApplicationContext(), "Deleted all messages.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", (dialog, which) -> {

                })
                .create()
                .show();
    }

    //endregion

    //region VIEWS

    private void addEventListeners() {
        playFloatingActionButton.setOnClickListener(view -> {
            synthesizeText();
        });

        addFloatingActionButton.setOnClickListener(view -> {
            startPresentationMessageActivityAddMessage();
        });
    }

    private void refreshAllViews() {
        refreshTextViews();
        refreshButtons();
    }

    private void refreshTextViews() {
        if (messageLinearLayout != null) {
            messageLinearLayout.removeAllViews();
            for (int i = 0; i < messages.size(); i++) {
                addTextViewToLinearLayout(messages.get(i));
            }

            refreshTextViewsBackground();
            refreshButtons();
        }
    }

    private void addTextViewToLinearLayout(String message) {
        TextView newTextView = new TextView(this);
        newTextView.setText(message);
        newTextView.setTextSize(messageTextSize);
        newTextView.setTextIsSelectable(false);
        newTextView.setBackground(null);
        newTextView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        newTextView.setOnClickListener(view -> {
            selectedMessageIndex = messageLinearLayout.indexOfChild(view);
            refreshTextViews();
        });
        registerForContextMenu(newTextView);

        PresentationActivity.this.runOnUiThread(() -> {
            messageLinearLayout.addView(newTextView);
        });
    }

    private void refreshTextViewsBackground() {
        if (messageLinearLayout != null) {
            for (int i = 0; i < messageLinearLayout.getChildCount(); i++) {
                messageLinearLayout.getChildAt(i).setBackground(null);
            }

            if (selectedMessageIndex < 0 || selectedMessageIndex >= messages.size()) {
                return;
            }

            if (isPlaying) {
                messageLinearLayout.getChildAt(selectedMessageIndex).setBackground(paintDrawablePlay);
            } else {
                messageLinearLayout.getChildAt(selectedMessageIndex).setBackground(paintDrawableSelect);
            }
        }
    }

    private void refreshButtons() {
        boolean isEnabled = !isPlaying && selectedMessageIndex >= 0 && selectedMessageIndex < messageLinearLayout.getChildCount();
        setFABEnabled(isEnabled, playFloatingActionButton, getDrawable(R.drawable.ic_play));
        setFABEnabled(!isPlaying, addFloatingActionButton, getDrawable(R.drawable.ic_add));
    }

    private void setFABEnabled(boolean enabled, FloatingActionButton fab, Drawable originalIcon) {
        // https://stackoverflow.com/questions/7228985/android-imagebutton-with-disabled-ui-feel
        fab.setEnabled(enabled);

        Drawable res = originalIcon.mutate();
        if (enabled) {
            res.setColorFilter(null);
        } else {
            res.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
        }
        fab.setImageDrawable(res);
    }

    //endregion

    //region CONTEXT MENU

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        selectedMessageIndex = messageLinearLayout.indexOfChild(view);
        refreshTextViews();

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_context_presentation, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.context_edit:
                startPresentationMessageActivityEditMessage();
                return true;
            case R.id.context_delete:
                deleteCurrentMessage();
                return true;
            case R.id.context_delete_all:
                deleteAllMessages();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    //endregion

    //region ACTIVITY INTENT

    private void startPresentationMessageActivityEditMessage() {
        Intent intent = new Intent(this, PresentationMessageActivity.class);
        intent.putExtra(EXTRA_MESSAGE, messages.get(selectedMessageIndex));
        intent.putExtra(EXTRA_REQUEST_CODE, editRequestCode);
        startActivityForResult(intent, editRequestCode);
    }

    private void startPresentationMessageActivityAddMessage() {
        Intent intent = new Intent(this, PresentationMessageActivity.class);
        intent.putExtra(EXTRA_REQUEST_CODE, addRequestCode);
        startActivityForResult(intent, addRequestCode);
    }

    public void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            String message = data.getStringExtra(EXTRA_MESSAGE);
            if (message == null) {
                message = "";
            }
            if (requestCode == editRequestCode) {
                editCurrentMessage(message);
            } else if (requestCode == addRequestCode) {
                addMessage(message);
            }
        } else {
            Toast.makeText(getApplicationContext(), "Cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    //endregion
}
