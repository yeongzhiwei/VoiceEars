package com.yeongzhiwei.voiceears.message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yeongzhiwei.voiceears.R;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int TYPE_INCOMING = 1;
    private static final int TYPE_OUTGOING = 2;
    private static final int TYPE_OUTGOING_ACTIVE = 3;
    private static final int TYPE_SYSTEM = 4;

    @NonNull private List<Message> messages;
    private int messageTextSize;

    public MessageAdapter(@NonNull List<Message> messages, int messageTextSize) {
        this.messages = messages;
        this.messageTextSize = messageTextSize;
    }

    public void setMessages(@NonNull List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void setMessageTextSize(int messageTextSize) {
        this.messageTextSize = messageTextSize;
    }


    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View itemView;

        switch (viewType) {
            case TYPE_INCOMING:
                itemView = layoutInflater.inflate(R.layout.item_message_incoming, parent, false);
                break;
            case TYPE_OUTGOING:
                itemView = layoutInflater.inflate(R.layout.item_message_outgoing, parent, false);
                break;
            case TYPE_OUTGOING_ACTIVE:
                itemView = layoutInflater.inflate(R.layout.item_message_outgoing_active, parent, false);
                break;
            case TYPE_SYSTEM:
            default:
                itemView = layoutInflater.inflate(R.layout.item_message_system, parent, false);
        }

        return new MessageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(messages.get(position).getMessage());
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        switch (messages.get(position).getType()) {
            case Incoming:
                return TYPE_INCOMING;
            case Outgoing:
                return TYPE_OUTGOING;
            case OutgoingActive:
                return TYPE_OUTGOING_ACTIVE;
            case System:
            default:
                return TYPE_SYSTEM;
        }
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageTextView;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.textView_message);
            messageTextView.setTextSize(messageTextSize);
        }

        void bind(String message) {
            messageTextView.setText(message);
        }
    }

}
