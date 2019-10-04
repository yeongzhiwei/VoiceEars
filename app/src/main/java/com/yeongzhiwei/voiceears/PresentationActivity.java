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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class PresentationActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    public static String EXTRA_MESSAGE = "com.yeongzhiwei.voiceears.MESSAGE";
    public static String EXTRA_REQUEST_CODE = "com.yeongzhiwei.voiceears.REQUESTCODE";
    public static int addRequestCode = 100;
    public static int editRequestCode = 200;
    private static final Integer seekBarMinValue = 10;

    private LinearLayout messageLinearLayout;
    private Button playButton;
    private SeekBar textSizeSeekBar;
    private ImageButton deleteImageButton;
    private ImageButton editImageButton;
    private ImageButton addImageButton;

    PaintDrawable paintDrawableSelect = null;
    PaintDrawable paintDrawablePlay = null;

    private Integer messageTextSize = 12;
    private Voice.Gender gender = Voice.Gender.Male;
    private Double audioSpeed = 1.0;
    private Boolean isPlaying = false;
    private ArrayList<String> messages = new ArrayList<>();
    private Integer selectedMessageIndex;

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
        gender = Voice.Gender.valueOf(PreferencesHelper.loadString(this, PreferencesHelper.Key.genderKey, gender.name()));
        audioSpeed = PreferencesHelper.loadInt(this, PreferencesHelper.Key.audioSpeedKey, 100) / 100.0;
        messages = PreferencesHelper.loadStringArray(this, PreferencesHelper.Key.presentationMessagesKey, new ArrayList<String>());
        selectedMessageIndex = PreferencesHelper.loadInt(this, PreferencesHelper.Key.presentationSelectedMessageIndexKey, -1);
    }

    //endregion

    //region INITIALIZATION

    private void initializeViews() {
        messageLinearLayout = findViewById(R.id.linearLayout_message);
        playButton = findViewById(R.id.button_play);
        textSizeSeekBar = findViewById(R.id.seekBar_textSize);
        deleteImageButton = findViewById(R.id.imageButton_delete);
        editImageButton = findViewById(R.id.imageButton_edit);
        addImageButton = findViewById(R.id.imageButton_add);
    }

    private void initializeVariables() {
        paintDrawableSelect = new PaintDrawable(ContextCompat.getColor(this, R.color.colorPrimary));
        paintDrawableSelect.setCornerRadius(8);

        paintDrawablePlay = new PaintDrawable(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        paintDrawablePlay.setCornerRadius(8);
    }

    private void configureTextToSpeech() {
        synthesizer = new Synthesizer(cognitiveServicesApiKey, cognitiveServicesRegion);
    }

    //endregion

    //region COGNITIVE SERVICES

    private void synthesizeText() {
        if (selectedMessageIndex < 0 || selectedMessageIndex >= messages.size()) {
            return;
        }

        String message = messages.get(selectedMessageIndex);

        new Thread(() -> {
            synthesizer.speakToAudio(message, audioSpeed, () -> {
                PresentationActivity.this.runOnUiThread(() -> {
                    isPlaying = true;
                    refreshTextViewsBackground();
                    refreshButtons();
                });
            }, () -> {
                PresentationActivity.this.runOnUiThread(() -> {
                    isPlaying = false;
                    selectedMessageIndex += 1;
                    refreshTextViewsBackground();
                    refreshButtons();
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

    private void deleteCurrentMessage() {
        String message = messages.get(selectedMessageIndex);
        String alertMessage = message.length() < 20 ? message : message.substring(0, 100) + "...";

        new AlertDialog.Builder(PresentationActivity.this)
                .setMessage("Do you want to delete this message?\n\n" + alertMessage)
                .setPositiveButton("Yes", (dialog, which) -> {
                    messages.remove(selectedMessageIndex);
                    refreshTextViews();
                    Toast.makeText(PresentationActivity.this, "Deleted a message.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", (dialog, which) -> {

                })
                .create()
                .show();
    }

    //endregion

    //region VIEWS

    private void addEventListeners() {
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
                PreferencesHelper.save(PresentationActivity.this, PreferencesHelper.Key.textViewSizeKey, messageTextSize);
            }
        });

        playButton.setOnClickListener(view -> {
            synthesizeText();
        });

        deleteImageButton.setOnClickListener(view -> {
            deleteCurrentMessage();
        });

        editImageButton.setOnClickListener(view -> {
            startPresentationMessageActivityEditMessage();
        });

        addImageButton.setOnClickListener(view -> {
            startPresentationMessageActivityAddMessage();
        });
    }

    private void refreshAllViews() {
        refreshTextViews();
        refreshTextSizeSeekBar();
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
            int childcount = messageLinearLayout.getChildCount();
            for (int i = 0; i < childcount; i++) {
                TextView textView = (TextView) messageLinearLayout.getChildAt(i);
                textView.setTextSize(messageTextSize);
            }
        }
    }

    private void refreshTextSizeSeekBar() {
        if (textSizeSeekBar != null) {
            textSizeSeekBar.setProgress(messageTextSize - seekBarMinValue);
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
        setImageButtonEnabled(isEnabled, deleteImageButton, getDrawable(R.drawable.trashbin));
        setImageButtonEnabled(isEnabled, editImageButton, getDrawable(R.drawable.edit));
        setImageButtonEnabled(!isPlaying, addImageButton, getDrawable(R.drawable.add));
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
            if (requestCode == editRequestCode) {
                messages.set(selectedMessageIndex, message);
                Toast.makeText(PresentationActivity.this, "Edited a message.", Toast.LENGTH_SHORT).show();
            } else if (requestCode == addRequestCode) {
                if (selectedMessageIndex >= 0 && selectedMessageIndex < messages.size()) {
                    selectedMessageIndex += 1;
                } else {
                    selectedMessageIndex = messages.size();
                }
                messages.add(selectedMessageIndex, message);
                Toast.makeText(PresentationActivity.this, "Added a message.", Toast.LENGTH_SHORT).show();
            }
            refreshTextViews();
        } else {
            Toast.makeText(PresentationActivity.this, "Cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    //endregion
}
