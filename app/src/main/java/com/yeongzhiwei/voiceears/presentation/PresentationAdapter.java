package com.yeongzhiwei.voiceears.presentation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yeongzhiwei.voiceears.R;

import java.util.List;

public class PresentationAdapter extends RecyclerView.Adapter<PresentationAdapter.MessageViewHolder> {

    public interface OnItemClickListener {

        void onItemClick(int index);

    }

    private static final int TYPE_DEFAULT = 0;
    private static final int TYPE_PLAYING = 1;
    private static final int TYPE_SELECTED = 2;

    @NonNull private List<String> messages;
    private int playingMessageIndex;
    private int selectedMessageIndex;
    private OnItemClickListener listener;

    PresentationAdapter(@NonNull List<String> messages, int playingMessageIndex, int selectedMessageIndex, OnItemClickListener listener) {
        this.messages = messages;
        this.playingMessageIndex = playingMessageIndex;
        this.selectedMessageIndex = selectedMessageIndex;
        this.listener = listener;
    }

    void setPlayingMessageIndex(int playingMessageIndex) {
        int oldPlayingMessageIndex = this.playingMessageIndex;
        this.playingMessageIndex = playingMessageIndex;
        notifyItemChanged(oldPlayingMessageIndex);
        notifyItemChanged(playingMessageIndex);
    }

    void setSelectedMessageIndex(int selectedMessageIndex) {
        int oldSelectedMessageIndex = this.selectedMessageIndex;
        this.selectedMessageIndex = selectedMessageIndex;
        notifyItemChanged(oldSelectedMessageIndex);
        notifyItemChanged(selectedMessageIndex);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View itemView;

        switch (viewType) {
            case TYPE_PLAYING:
                itemView = layoutInflater.inflate(R.layout.item_presentation_message_playing, parent, false);
                break;
            case TYPE_SELECTED:
                itemView = layoutInflater.inflate(R.layout.item_presentation_message_selected, parent, false);
                break;
            case TYPE_DEFAULT:
            default:
                itemView = layoutInflater.inflate(R.layout.item_presentation_message_default, parent, false);
        }

        return new MessageViewHolder(itemView, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(position, messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == playingMessageIndex) {
            return TYPE_PLAYING;
        } else if (position == selectedMessageIndex) {
            return TYPE_SELECTED;
        }
        return TYPE_DEFAULT;
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView messageTextView;

        MessageViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.textView_message);
            messageTextView.setOnClickListener(v -> {
                if (listener != null) {
                    int index = (int) v.getTag();
                    listener.onItemClick(index);
                }
            });
        }

        void bind(int index, String message) {
            messageTextView.setTag(index);
            messageTextView.setText(message);
        }
    }

}
