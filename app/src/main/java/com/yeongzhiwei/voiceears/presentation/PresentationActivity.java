package com.yeongzhiwei.voiceears.presentation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yeongzhiwei.voiceears.PreferencesHelper;
import com.yeongzhiwei.voiceears.R;
import com.yeongzhiwei.voiceears.ttsstt.Gender;
import com.yeongzhiwei.voiceears.ttsstt.Synthesizer;

import java.util.ArrayList;

public class PresentationActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "com.yeongzhiwei.voiceears.MESSAGE";
    public static final String EXTRA_REQUEST_CODE = "com.yeongzhiwei.voiceears.REQUESTCODE";
    public static final int CREATE_REQUEST_CODE = 100;
    public static final int EDIT_REQUEST_CODE = 200;

    private FloatingActionButton playFloatingActionButton;
    private FloatingActionButton addFloatingActionButton;

    private PresentationAdapter presentationAdapter;
    @NonNull private ArrayList<String> messages = new ArrayList<>();
    private int playingMessageIndex = -1;
    private int selectedMessageIndex = -1;

    private MenuItem editMenuItem;
    private MenuItem deleteMenuItem;

    private String cognitiveServicesApiKey;
    private String cognitiveServicesRegion;
    private Gender gender;
    private Synthesizer synthesizer;

    //region ACTIVITY LIFECYCLE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation);

        playFloatingActionButton = findViewById(R.id.fab_play);
        addFloatingActionButton = findViewById(R.id.fab_add);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
        }

        loadSavedPreferences();

        RecyclerView messageRecyclerView = findViewById(R.id.recyclerView_message);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        messageRecyclerView.setLayoutManager(linearLayoutManager);
        presentationAdapter = new PresentationAdapter(messages, playingMessageIndex, selectedMessageIndex, new PresentationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int index) {
                setSelectedMessageIndex(index);
            }
        });
        messageRecyclerView.setAdapter(presentationAdapter);

        refreshPlayButton();
        addEventListeners();
        configureTextToSpeech();
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
        getMenuInflater().inflate(R.menu.menu_presentation, menu);
        editMenuItem = menu.findItem(R.id.action_edit);
        deleteMenuItem = menu.findItem(R.id.action_delete);
        refreshMenuItems();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_edit:
                startPresentationMessageActivityEditMessage();
                return true;
            case R.id.action_delete:
                deleteSelectedMessage();
                return true;
            case R.id.action_delete_all:
                deleteAllMessages();
                return true;
            default:
                // Do nothing
        }

        return super.onOptionsItemSelected(item);
    }

    //endregion

    //region SHARED PREFERENCES

    private void loadSavedPreferences() {
        cognitiveServicesApiKey = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesApiKeyKey, "");
        cognitiveServicesRegion = PreferencesHelper.loadString(this, PreferencesHelper.Key.cognitiveServicesRegionKey, "");
        gender = Gender.valueOf(PreferencesHelper.loadString(this, PreferencesHelper.Key.genderKey));
        messages = PreferencesHelper.loadStringArray(this, PreferencesHelper.Key.presentationMessagesKey, new ArrayList<>());
        selectedMessageIndex = PreferencesHelper.loadInt(this, PreferencesHelper.Key.presentationSelectedMessageIndexKey, -1);
    }

    private void savePreferences() {
        PreferencesHelper.save(this, PreferencesHelper.Key.presentationMessagesKey, messages);
        PreferencesHelper.save(this, PreferencesHelper.Key.presentationSelectedMessageIndexKey, selectedMessageIndex);
    }

    //endregion

    //endregion

    //region COGNITIVE SERVICES

    private void configureTextToSpeech() {
        synthesizer = new Synthesizer(cognitiveServicesApiKey, cognitiveServicesRegion, gender);
    }

    private void synthesizeText() {
        if (selectedMessageIndex < 0 || selectedMessageIndex >= messages.size()) {
            return;
        }

        String text = messages.get(selectedMessageIndex);

        new Thread(() -> {
            synthesizer.speak(text,
                () -> {
                    PresentationActivity.this.runOnUiThread(() -> {
                        setPlayingMessageIndex(selectedMessageIndex);
                    });
                }, () -> {
                    PresentationActivity.this.runOnUiThread(() -> {
                        if (selectedMessageIndex == playingMessageIndex) {
                            setSelectedMessageIndex(selectedMessageIndex + 1);
                        }
                        setPlayingMessageIndex(-1);
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

    public void setPlayingMessageIndex(int playingMessageIndex) {
        this.playingMessageIndex = playingMessageIndex;
        if (presentationAdapter != null) {
            presentationAdapter.setPlayingMessageIndex(playingMessageIndex);
        }
        refreshPlayButton();
    }

    public void setSelectedMessageIndex(int selectedMessageIndex) {
        this.selectedMessageIndex = selectedMessageIndex;
        if (presentationAdapter != null) {
            presentationAdapter.setSelectedMessageIndex(selectedMessageIndex);
        }
        refreshMenuItems();
        refreshPlayButton();
    }

    private void createMessage(String message) {
        String[] newMessages = message.replaceAll("\n", "").split("[.!?]");
        int insertIndex = selectedMessageIndex;
        if (insertIndex < 0 || insertIndex >= messages.size()) {
            insertIndex = messages.size() - 1;
        }
        for (String newMessage : newMessages) {
            insertIndex += 1;
            messages.add(insertIndex, newMessage.trim());
            presentationAdapter.notifyItemInserted(insertIndex);
        }
        presentationAdapter.notifyItemRangeChanged(insertIndex + 1, messages.size() - insertIndex);
        setSelectedMessageIndex(insertIndex);
        Toast.makeText(getApplicationContext(), getString(R.string.presentation_message_toast_create), Toast.LENGTH_SHORT).show();

    }

    private void editSelectedMessage(String message) {
        messages.set(selectedMessageIndex, message);
        Toast.makeText(getApplicationContext(), getString(R.string.presentation_message_toast_edit), Toast.LENGTH_SHORT).show();

        presentationAdapter.notifyItemChanged(selectedMessageIndex);
    }

    private void deleteSelectedMessage() {
        int toDeleteMessageIndex = selectedMessageIndex;
        String text = messages.get(toDeleteMessageIndex);
        String alertText = text.length() < 20 ? text : text.substring(0, 20) + "...";

        new AlertDialog.Builder(PresentationActivity.this)
                .setMessage("Do you want to delete this message?\n\n" + alertText)
                .setPositiveButton("Yes", (dialog, which) -> {
                    messages.remove(toDeleteMessageIndex);
                    if (playingMessageIndex == toDeleteMessageIndex) {
                        setPlayingMessageIndex(-2);
                        setSelectedMessageIndex(-1);
                    }
                    presentationAdapter.notifyDataSetChanged();
                    Toast.makeText(getApplicationContext(), getString(R.string.presentation_message_toast_delete), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .create()
                .show();
    }

    private void deleteAllMessages() {
        new AlertDialog.Builder(PresentationActivity.this)
                .setMessage("Do you want to delete all messages?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    messages.clear();
                    presentationAdapter.notifyDataSetChanged();
                    setSelectedMessageIndex(-1);
                    setPlayingMessageIndex(-2);
                    Toast.makeText(getApplicationContext(), getString(R.string.presentation_message_toast_delete_all), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
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
            startPresentationMessageActivityCreateMessage();
        });
    }

    private void refreshMenuItems() {
        if (editMenuItem == null || deleteMenuItem == null) {
            return;
        }

        boolean visible = selectedMessageIndex >= 0 && selectedMessageIndex < messages.size();
        editMenuItem.setVisible(visible);
        deleteMenuItem.setVisible(visible);
    }

    private void refreshPlayButton() {
        boolean enabled = playingMessageIndex == -1 && selectedMessageIndex >= 0 && selectedMessageIndex < messages.size();
        playFloatingActionButton.setEnabled(enabled);
    }

    //endregion

    //region ACTIVITY INTENT

    private void startPresentationMessageActivityEditMessage() {
        Intent intent = new Intent(this, PresentationMessageActivity.class);
        intent.putExtra(EXTRA_MESSAGE, messages.get(selectedMessageIndex));
        intent.putExtra(EXTRA_REQUEST_CODE, EDIT_REQUEST_CODE);
        startActivityForResult(intent, EDIT_REQUEST_CODE);
    }

    private void startPresentationMessageActivityCreateMessage() {
        Intent intent = new Intent(this, PresentationMessageActivity.class);
        intent.putExtra(EXTRA_REQUEST_CODE, CREATE_REQUEST_CODE);
        startActivityForResult(intent, CREATE_REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            String message = data.getStringExtra(EXTRA_MESSAGE);
            if (message == null) {
                message = "";
            }
            if (requestCode == EDIT_REQUEST_CODE) {
                editSelectedMessage(message);
            } else if (requestCode == CREATE_REQUEST_CODE) {
                createMessage(message);
            }
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.presentation_message_toast_cancel), Toast.LENGTH_SHORT).show();
        }
    }

    //endregion
}
