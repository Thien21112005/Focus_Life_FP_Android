package com.hcmute.edu.vn.focus_life.ui.ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hcmute.edu.vn.focus_life.R;
import java.util.List;

public class AICoachAdapter extends RecyclerView.Adapter<AICoachAdapter.ChatViewHolder> {

    private final List<ChatMessage> messages;

    public AICoachAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Layout item_chat_message phải chứa cả layout cho User và AI
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (message.getType() == ChatMessage.TYPE_USER) {
            holder.layoutUser.setVisibility(View.VISIBLE);
            holder.layoutAI.setVisibility(View.GONE);
            holder.tvMessageUser.setText(message.getContent());
        } else {
            holder.layoutAI.setVisibility(View.VISIBLE);
            holder.layoutUser.setVisibility(View.GONE);
            holder.tvMessageAI.setText(message.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        View layoutAI, layoutUser;
        TextView tvMessageAI, tvMessageUser;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutAI = itemView.findViewById(R.id.layoutAI);
            layoutUser = itemView.findViewById(R.id.layoutUser);
            tvMessageAI = itemView.findViewById(R.id.tvMessageAI);
            tvMessageUser = itemView.findViewById(R.id.tvMessageUser);
        }
    }
}