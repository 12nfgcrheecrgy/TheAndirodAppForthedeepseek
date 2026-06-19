package com.deepseek.chat.ui.search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.deepseek.chat.R;
import com.deepseek.chat.data.repository.ChatRepository;
import com.deepseek.chat.domain.model.ChatMessage;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private TextInputEditText etSearch;
    private RecyclerView rvResults;
    private TextView tvEmpty;
    private SearchResultAdapter adapter;
    private ChatRepository chatRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        etSearch = findViewById(R.id.et_search);
        rvResults = findViewById(R.id.rv_results);
        tvEmpty = findViewById(R.id.tv_empty);

        toolbar.setNavigationOnClickListener(v -> finish());

        chatRepository = new ChatRepository(this);
        adapter = new SearchResultAdapter();
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearch.getText() != null ? etSearch.getText().toString() : "");
                hideKeyboard();
                return true;
            }
            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    performSearch(query);
                } else {
                    adapter.submitList(new ArrayList<>());
                    tvEmpty.setVisibility(View.GONE);
                }
            }
        });

        etSearch.requestFocus();
        showKeyboard();
    }

    private void performSearch(String query) {
        chatRepository.searchMessages(query).observe(this, messages -> {
            adapter.submitList(messages);
            tvEmpty.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }

    private static class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

        private List<ChatMessage> items = new ArrayList<>();

        public void submitList(List<ChatMessage> items) {
            this.items = items != null ? items : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatMessage msg = items.get(position);
            holder.tvRole.setText(msg.isUser() ? "用户" : "助手");
            holder.tvContent.setText(msg.getContent());
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), com.deepseek.chat.ui.chat.ChatActivity.class);
                intent.putExtra("session_id", msg.getSessionId());
                v.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvRole;
            final TextView tvContent;

            ViewHolder(View itemView) {
                super(itemView);
                tvRole = itemView.findViewById(R.id.tv_role);
                tvContent = itemView.findViewById(R.id.tv_content);
            }
        }
    }
}
