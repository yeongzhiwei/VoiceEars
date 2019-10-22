package com.yeongzhiwei.voiceears;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class PresentationActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    public static String EXTRA_MESSAGE = "com.yeongzhiwei.voiceears.MESSAGE";
    public static String EXTRA_REQUEST_CODE = "com.yeongzhiwei.voiceears.REQUESTCODE";
    public static int addRequestCode = 100;
    public static int editRequestCode = 200;

    private FloatingActionButton addFloatingActionButton;
    private LinearLayout messageLinearLayout;
    private Button playButton;

    PaintDrawable paintDrawableSelect = null;
    PaintDrawable paintDrawablePlay = null;

    private Integer messageTextSize = 20;
    private Synthesizer.Gender gender = Synthesizer.Gender.Male;
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
        gender = Synthesizer.Gender.valueOf(PreferencesHelper.loadString(this, PreferencesHelper.Key.genderKey, gender.name()));
        messages = PreferencesHelper.loadStringArray(this, PreferencesHelper.Key.presentationMessagesKey, new ArrayList<>());
        selectedMessageIndex = PreferencesHelper.loadInt(this, PreferencesHelper.Key.presentationSelectedMessageIndexKey, -1);
    }

    //endregion

    //region INITIALIZATION

    private void initializeViews() {
        addFloatingActionButton = findViewById(R.id.fab_add);
        messageLinearLayout = findViewById(R.id.linearLayout_message);
        playButton = findViewById(R.id.button_play);
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
        isPlaying = true;
        refreshButtons();

        if (selectedMessageIndex < 0 || selectedMessageIndex >= messages.size()) {
            return;
        }

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

    private void setMessageTextSize(Integer size) {
        messageTextSize = size;
        refreshMessageTextSize();
    }

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
        String alertMessage = message.length() < 20 ? message : message.substring(0, 100) + "...";

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
        playButton.setOnClickListener(view -> {
            synthesizeText();
        });

//        deleteImageButton.setOnClickListener(view -> {
//            deleteCurrentMessage();
//        });
//
//        deleteImageButton.setOnLongClickListener(view -> {
//            deleteAllMessages();
//            return true;
//        });
//
//        editImageButton.setOnClickListener(view -> {
//            startPresentationMessageActivityEditMessage();
//        });

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

    public void addTextViewToLinearLayout(String message) {
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

        PresentationActivity.this.runOnUiThread(() -> {
            messageLinearLayout.addView(newTextView);
        });
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
        playButton.setEnabled(isEnabled);
//        setImageButtonEnabled(isEnabled, deleteImageButton, getDrawable(R.drawable.ic_trashbin));
//        setImageButtonEnabled(isEnabled, editImageButton, getDrawable(R.drawable.ic_edit));
//        setImageButtonEnabled(!isPlaying, addImageButton, getDrawable(R.drawable.ic_add));
    }

    public void setImageButtonEnabled(boolean enabled, ImageButton item, Drawable originalIcon) {
        // https://stackoverflow.com/questions/7228985/android-imagebutton-with-disabled-ui-feel
        item.setEnabled(enabled);

        Drawable res = originalIcon.mutate();
        if (enabled) {
            res.setColorFilter(null);
        } else {
            res.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
        }
        item.setImageDrawable(res);
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
