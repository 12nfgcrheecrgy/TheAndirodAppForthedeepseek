package com.deepseek.chat.ui.sessions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.deepseek.chat.R;
import com.deepseek.chat.domain.model.ChatSession;

public class SessionsAdapter extends ListAdapter<ChatSession, SessionsAdapter.ViewHolder> {

    private OnSessionClickListener listener;

    public SessionsAdapter() {
        super(new DiffUtil.ItemCallback<ChatSession>() {
            @Override
            public boolean areItemsTheSame(@NonNull ChatSession oldItem, @NonNull ChatSession newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull ChatSession oldItem, @NonNull ChatSession newItem) {
                return oldItem.getTitle().equals(newItem.getTitle())
                        && oldItem.getUpdatedAt() == newItem.getUpdatedAt();
            }
        });
    }

    public void setListener(OnSessionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSession session = getItem(position);
        holder.tvTitle.setText(session.getTitle());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSessionClick(session);
        });
        holder.btnOptions.setOnClickListener(v -> {
            if (listener != null) listener.onSessionOptionsClick(session, v);
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final ImageButton btnOptions;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_session_title);
            btnOptions = itemView.findViewById(R.id.btn_session_options);
        }
    }

    public interface OnSessionClickListener {
        void onSessionClick(ChatSession session);
        void onSessionOptionsClick(ChatSession session, View anchor);
    }
}
