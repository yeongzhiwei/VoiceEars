package com.yeongzhiwei.voiceears;

import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;

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

    private final Integer seekBarMinValue = 10;
    private Integer textViewSize = 12; // default
    private TextView selectedTextView;
    private Voice.Gender gender = Voice.Gender.Male; // default
    private double audioSpeed = 1.0; // default

    // Cognitive Services
    private String cognitiveServicesApiKey;
    private String cognitiveServicesRegion;

    // Text-to-Speech
    private Synthesizer synthesizer = null;

    private ArrayList<String> messages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation);

        loadSavedPreferences();
        initializeViews();
        configureViews();
        configureTextToSpeech();
        loadSampleMessages();

        refreshPlayButton();
    }

    private void loadSampleMessages() {
        String[] samples = {
                "I was born deaf to deaf parents and was enrolled in Singapore School for the Deaf that used sign language as a medium of instruction.",
                "At 12, I went to a mainstream school with sign language resource teacher as support.",
                "Then I studied accountancy in polytechnic and worked in the related field.",
                "Few years ago, I took Introduction to Computer Science course online and it sparked my interest in technology.",
                "I decided to leave my job to dedicate my time to learning tech and pursue my passion. I have completed more than 30 courses online and built a few apps to date.",

                "I am currently studying Bachelor of Science in Information and Communication Technology at Singapore University of Social Sciences and will graduate next year.",
                "I am always hungry for knowledge and see myself learning new technology for life.",
                "Online courses with subtitles empowered me in such a radical way that I want to bring it forward and leverage my skills to empower people with disabilities.",

                "When it is time to take a break, I like to travel – trying out new food, admiring the nature and learning the culture.",

                "My summer internship journey with Pratima’s team in OCP has been nothing but amazing.",
                "I obtained Azure Fundamentals within a month, flew to Sydney with interns from Singapore for amazing intern learning week.",
                "I met lots of awesome friends from South East Asia, Australia and New Zealand.",
                "After that, I passed Azure Administrator exam with great difficulty and successfully hosted Codess along with the organizing committee last Saturday.",
                "We had Andrea Della Mattea gracing the event as a keynote speaker and with Pratima, on the panel discussing about demystifying stereotypes in the tech industry.",
                "These are in addition to my usual role in OCP.",

                "Finally, developing this app is the main highlight of my stint here. This is for empowering Deaf in their conversations with the hearing peers.",
                "During my lunch with fellow interns, I had to show what I typed on my phone to them one by one.",
                "Not any more. This app uses Azure Cognitive Service – Neural Text to Speech to synthesize and play the audio from typed text.",
                "Besides this, this also captures the surrounding audio, recognize and convert the speech into text. All in a single view.",
                "Watch a quick demo and that concludes my brief introduction.",

                "Now Adrian & I will share with you briefly on the Deaf Culture & Community. We will keep it short so we can get started on learning signs.",

                "Can you tell who among us is deaf from just this picture?",

                "This manifests in many ways in our everyday life. I’ve had a bicycle rider behind ringing the bell incessantly on the pavement. I’ve had rough taps on my shoulder with “angry” faces in a crowded place (probably after saying excuse me several times). I’ve been approached countless times for directions.",

                "In general, we call ourselves either Deaf or hard-of-hearing. Seldom hearing-impaired, never deaf-and-mute or deaf-and-dumb. I call myself Deaf.",

                "Deaf person has usually moderate or profound hearing loss. I have profound hearing loss in both ears.",

                "We have different modes of communication depending on the degree of hearing loss and the environment in which we were brought up.",
                "These are my preferred modes of communications.",
                "When you meet deaf or hard-of-hearing people, ask them their preferred modes of communication."
        };

        for (int i = 0; i < samples.length; i++) {
            messages.add(samples[i]);
        }

        PreferencesHelper.save(this, PreferencesHelper.Key.presentationMessagesKey, messages);
        Log.d(LOG_TAG, "Save success");

        ArrayList<String> test = PreferencesHelper.loadStringArray(this, PreferencesHelper.Key.presentationMessagesKey, new ArrayList<String>());
        Log.d(LOG_TAG, "Loaded " + String.format("%d", test.size()) + " messages");
    }

    private void loadSavedPreferences() {
        cognitiveServicesApiKey = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesApiKeyKey, "");
        cognitiveServicesRegion = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesRegionKey, "");
        textViewSize = PreferencesHelper.loadInt(this, PreferencesHelper.Key.textViewSizeKey, textViewSize);
        gender = Voice.Gender.valueOf(PreferencesHelper.loadString(this, PreferencesHelper.Key.genderKey, gender.name()));
        audioSpeed = PreferencesHelper.loadInt(this, PreferencesHelper.Key.audioSpeedKey, 100) / 100.0;
    }

    private void initializeViews() {
        messageLinearLayout = findViewById(R.id.linearLayout_message);
        playButton = findViewById(R.id.button_play);
        textSizeSeekBar = findViewById(R.id.seekBar_textSize);
        deleteButton = findViewById(R.id.imageButton_delete);
        editButton = findViewById(R.id.imageButton_edit);
        upButton = findViewById(R.id.imageButton_up);
        downButton = findViewById(R.id.imageButton_down);
        addButton = findViewById(R.id.imageButton_add);
    }

    private void configureViews() {
        paintDrawableSelect = new PaintDrawable(ContextCompat.getColor(this, R.color.colorPrimary));
        paintDrawableSelect.setCornerRadius(8);

        paintDrawablePlay = new PaintDrawable(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        paintDrawablePlay.setCornerRadius(8);

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
            // TODO
        });

        editButton.setOnClickListener(view -> {
            // TODO
        });

        upButton.setOnClickListener(view -> {
            // TODO
        });

        downButton.setOnClickListener(view -> {
            // TODO
        });

        addButton.setOnClickListener(view -> {
            // TODO
        });
    }

    private void configureTextToSpeech() {
        synthesizer = new Synthesizer(cognitiveServicesApiKey, cognitiveServicesRegion);
    }

    /* UPDATE VIEWS */

    private void refreshPlayButton() {
        playButton.setEnabled(selectedTextView != null);
    }

    private void setTextViewSize() {
        int childcount = messageLinearLayout.getChildCount();
        for (int i = 0; i < childcount; i++) {
            TextView textView = (TextView) messageLinearLayout.getChildAt(i);
            textView.setTextSize(textViewSize);
        }
    }

    public void addTextViewToLinearLayout(String message) {
        TextView newTextView = createNewTextView(message);

        PresentationActivity.this.runOnUiThread(() -> {
            messageLinearLayout.addView(newTextView, messageLinearLayout.getChildCount() - 1);
        });

        if (selectedTextView == null) {
            selectTextView(newTextView);
        }
    }

    private TextView createNewTextView(final String message) {
        TextView newTextView = new TextView(this);
        newTextView.setText(message);
        newTextView.setTextSize(textViewSize);
        newTextView.setTextIsSelectable(false);
        newTextView.setBackground(null);
        newTextView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        newTextView.setOnClickListener(view -> {
            selectTextView(newTextView);
        });

        return newTextView;
    }

    public void selectTextView(TextView textView) {
        if (selectedTextView != null) {
            selectedTextView.setBackground(null);
        }

        selectedTextView = textView;
        if (selectedTextView != null) {
            selectedTextView.setBackground(paintDrawableSelect);
        }
        refreshPlayButton();
    }

    public void selectNextTextView() {
        int selectedTextViewIndex = messageLinearLayout.indexOfChild(selectedTextView);
        if (selectedTextViewIndex + 2 < messageLinearLayout.getChildCount()) {
            TextView nextTextView = (TextView) messageLinearLayout.getChildAt(selectedTextViewIndex + 1);
            selectTextView(nextTextView);
        } else {
            selectTextView(null);
        }
    }

    /* TEXT TO SPEECH */
    private void synthesizeText() {
        if (selectedTextView == null) {
            return;
        }

        String message = selectedTextView.getText().toString();

        new Thread(() -> {
            synthesizer.speakToAudio(message, audioSpeed, () -> {
                PresentationActivity.this.runOnUiThread(() -> {
                    playButton.setEnabled(false);
                    selectedTextView.setBackground(paintDrawablePlay);
                });
            }, () -> {
                PresentationActivity.this.runOnUiThread(() -> {
                    playButton.setEnabled(true);
                    selectedTextView.setBackground(paintDrawableSelect);
                    selectNextTextView();
                });
            });
        }).start();
    }
}
