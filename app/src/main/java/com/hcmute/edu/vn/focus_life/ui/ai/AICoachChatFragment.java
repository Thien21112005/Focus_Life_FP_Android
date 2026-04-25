package com.hcmute.edu.vn.focus_life.ui.ai;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.BuildConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class AICoachChatFragment extends Fragment {

    private RecyclerView rvChat;
    private AICoachAdapter adapter;
    private List<ChatMessage> messageList;
    private EditText etMessage;
    private View btnSend;
    private ProgressBar chatProgressBar;

    // URL CHUẨN: v1beta + gemini-1.5-flash
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + BuildConfig.GEMINI_API_KEY;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_ai_coach_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChat = view.findViewById(R.id.rvChat);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        chatProgressBar = view.findViewById(R.id.chatProgressBar);

        messageList = new ArrayList<>();
        messageList.add(new ChatMessage("Chào bạn! Tôi là FocusLife AI. Bạn cần tư vấn gì về sức khỏe hôm nay?", ChatMessage.TYPE_AI));

        adapter = new AICoachAdapter(messageList);
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChat.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
        setupChips(view);
    }

    private void setupChips(View view) {
        if(view.findViewById(R.id.chip1) != null)
            view.findViewById(R.id.chip1).setOnClickListener(v -> { etMessage.setText("Gợi ý bài tập sức khỏe."); sendMessage(); });
        if(view.findViewById(R.id.chip2) != null)
            view.findViewById(R.id.chip2).setOnClickListener(v -> { etMessage.setText("Thực đơn ăn uống lành mạnh."); sendMessage(); });
        if(view.findViewById(R.id.chip3) != null)
            view.findViewById(R.id.chip3).setOnClickListener(v -> { etMessage.setText("Mẹo để tập trung làm việc."); sendMessage(); });
    }

    private void sendMessage() {
        String query = etMessage.getText().toString().trim();
        if (query.isEmpty()) return;

        addMessage(new ChatMessage(query, ChatMessage.TYPE_USER));
        etMessage.setText("");
        callGeminiAPI(query);
    }

    private void addMessage(ChatMessage message) {
        messageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvChat.scrollToPosition(messageList.size() - 1);
    }

    private void callGeminiAPI(String userText) {
        chatProgressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            String responseText = "";
            try {
                URL url = new URL(GEMINI_URL);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Body JSON
                JSONObject jsonBody = new JSONObject();
                JSONArray contentsArray = new JSONArray();
                JSONObject contentObj = new JSONObject();
                JSONArray partsArray = new JSONArray();
                JSONObject partObj = new JSONObject();
                partObj.put("text", "Bạn là huấn luyện viên sức khỏe. Trả lời ngắn gọn: " + userText);
                partsArray.put(partObj);
                contentObj.put("parts", partsArray);
                contentsArray.put(contentObj);
                jsonBody.put("contents", contentsArray);

                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject responseJson = new JSONObject(sb.toString());
                    responseText = responseJson.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");
                } else {
                    // ĐỌC LỖI CHI TIẾT TỪ SERVER
                    InputStreamReader isr = new InputStreamReader(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    responseText = "Google báo lỗi: " + sb.toString();
                }
            } catch (Exception e) {
                responseText = "Lỗi kết nối App: " + e.getMessage();
            }

            final String finalResponse = responseText;
            mainHandler.post(() -> {
                if (isAdded()) {
                    chatProgressBar.setVisibility(View.GONE);
                    addMessage(new ChatMessage(finalResponse, ChatMessage.TYPE_AI));
                }
            });
        });
    }
}