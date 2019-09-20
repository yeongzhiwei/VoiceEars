package com.yeongzhiwei.voiceears;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static android.Manifest.permission.RECORD_AUDIO;

/*
TODO
- Add second Activity to edit/create TextView
- Implement button clicks
- Change PaintDrawable to one with border and add paintDrawableDefault
 */

public class PresentationActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private LinearLayout messageLinearLayout;
    private Button playButton;
    private SeekBar textSizeSeekBar;
    private ImageButton deleteButton;
    private ImageButton editButton;
    private ImageButton upButton;
    private ImageButton downButton;
    private ImageButton addButton;

    PaintDrawable paintDrawableSelect = null;
    PaintDrawable paintDrawablePlay = null;

    private final int seekBarMinValue = 10;
    private int textViewSize = 12; // default
    private ArrayList<String> messages = new ArrayList<>();
    private int selectedMessageIndex;

    // Cognitive Services
    private String cognitiveServicesApiKey;
    private String cognitiveServicesRegion;

    // Text-to-Speech
    private Synthesizer synthesizer = null;
    private Voice.Gender gender = Voice.Gender.Male; // default
    private double audioSpeed = 1.0; // default
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation);
        loadSampleMessages();
        loadSavedPreferences();
        initializeVariables();
        initializeViews();
        configureViews();
        configureTextToSpeech();
    }

    private void loadSampleMessages() {
        String[] samples = {
                "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum", "Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old. Richard McClintock, a Latin professor at Hampden-Sydney College in Virginia, looked up one of the more obscure Latin words, consectetur, from a Lorem Ipsum passage, and going through the cites of the word in classical literature, discovered the undoubtable source. Lorem Ipsum comes from sections 1.10.32 and 1.10.33 of \"de Finibus Bonorum et Malorum\" (The Extremes of Good and Evil) by Cicero, written in 45 BC. This book is a treatise on the theory of ethics, very popular during the Renaissance.", "c", "d", "e"
        };

        for (int i = 0; i < samples.length; i++) {
            messages.add(samples[i]);
        }

        PreferencesHelper.save(this, PreferencesHelper.Key.presentationMessagesKey, messages);
        Log.d(LOG_TAG, "Save success");
    }

    @Override
    protected void onPause() {
        super.onPause();

        savePreferences();
    }

    private void savePreferences() {
        PreferencesHelper.save(this, PreferencesHelper.Key.textViewSizeKey, textViewSize);
        PreferencesHelper.save(this, PreferencesHelper.Key.presentationMessagesKey, messages);
        PreferencesHelper.save(this, PreferencesHelper.Key.presentationSelectedMessageIndexKey, selectedMessageIndex);
    }

    private void loadSavedPreferences() {
        cognitiveServicesApiKey = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesApiKeyKey, "");
        cognitiveServicesRegion = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesRegionKey, "");
        textViewSize = PreferencesHelper.loadInt(this, PreferencesHelper.Key.textViewSizeKey, textViewSize);
        gender = Voice.Gender.valueOf(PreferencesHelper.loadString(this, PreferencesHelper.Key.genderKey, gender.name()));
        audioSpeed = PreferencesHelper.loadInt(this, PreferencesHelper.Key.audioSpeedKey, 100) / 100.0;
        messages = PreferencesHelper.loadStringArray(this, PreferencesHelper.Key.presentationMessagesKey, new ArrayList<String>());
        selectedMessageIndex = PreferencesHelper.loadInt(this, PreferencesHelper.Key.presentationSelectedMessageIndexKey, -1);
    }

    private void initializeVariables() {
        paintDrawableSelect = new PaintDrawable(ContextCompat.getColor(this, R.color.colorPrimary));
        paintDrawableSelect.setCornerRadius(8);

        paintDrawablePlay = new PaintDrawable(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        paintDrawablePlay.setCornerRadius(8);
    }

    private void initializeViews() {
        messageLinearLayout = findViewById(R.id.linearLayout_message);
        playButton = findViewById(R.id.button_play);
        textSizeSeekBar = findViewById(R.id.seekBar_textSize);
        deleteButton = findViewById(R.id.imageButton_delete);
        editButton = findViewById(R.id.imageButton_edit);
        addButton = findViewById(R.id.imageButton_add);
    }

    private void configureViews() {
        for (int i = 0; i < messages.size(); i++) {
            addTextViewToLinearLayout(messages.get(i));
        }

        refreshButtons();

        textSizeSeekBar.setProgress(textViewSize - seekBarMinValue);

        textSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                textViewSize = i + seekBarMinValue;
                setTextViewSize();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                PreferencesHelper.save(PresentationActivity.this, PreferencesHelper.Key.textViewSizeKey, textViewSize);
            }
        });

        playButton.setOnClickListener(view -> {
            synthesizeText();
        });

        deleteButton.setOnClickListener(view -> {
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
        });

        editButton.setOnClickListener(view -> {
            // TODO
        });

        addButton.setOnClickListener(view -> {
            // TODO
        });
    }

    private void configureTextToSpeech() {
        synthesizer = new Synthesizer(cognitiveServicesApiKey, cognitiveServicesRegion);
    }

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

    /* UPDATE VIEWS */

    private void refreshTextViews() {
        messageLinearLayout.removeAllViews();
        for (int i = 0; i < messages.size(); i++) {
            addTextViewToLinearLayout(messages.get(i));
        }

        refreshTextViewsBackground();
        refreshButtons();
    }

    private void refreshTextViewsBackground() {
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

    private void refreshButtons() {
        playButton.setEnabled(!isPlaying && selectedMessageIndex >= 0 && selectedMessageIndex < messageLinearLayout.getChildCount());
        editButton.setEnabled(!isPlaying);
        upButton.setEnabled(!isPlaying);
        downButton.setEnabled(!isPlaying);
        addButton.setEnabled(!isPlaying);
    }

    public void addTextViewToLinearLayout(String message) {
        TextView newTextView = new TextView(this);
        newTextView.setText(message);
        newTextView.setTextSize(textViewSize);
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

    private void setTextViewSize() {
        int childcount = messageLinearLayout.getChildCount();
        for (int i = 0; i < childcount; i++) {
            TextView textView = (TextView) messageLinearLayout.getChildAt(i);
            textView.setTextSize(textViewSize);
        }
    }
}
