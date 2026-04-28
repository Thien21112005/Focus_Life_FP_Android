package com.hcmute.edu.vn.focus_life.ui.ai;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import com.hcmute.edu.vn.focus_life.FocusLifeApp;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.utils.DateUtils;
import com.hcmute.edu.vn.focus_life.data.local.db.AppDatabase;
import com.hcmute.edu.vn.focus_life.data.local.entity.DailySummaryEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.ProfileEntity;

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

    private static final String TAG = "AICoachChat";
    private RecyclerView rvChat;
    private AICoachAdapter adapter;
    private List<ChatMessage> messageList;
    private EditText etMessage;
    private View btnSend;
    private ProgressBar chatProgressBar;

    private static final String GEMINI_API_KEY = "AIzaSyBi_ChxpeGNM6wCQQPuoACYflQWxvRBtpc";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY;

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
        messageList.add(new ChatMessage("Chào bạn! Tôi là FocusLife AI. Tôi đã sẵn sàng tư vấn dựa trên thể trạng và mục tiêu của bạn.", ChatMessage.TYPE_AI));

        adapter = new AICoachAdapter(messageList);
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChat.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
        setupChips(view);
    }

    private void setupChips(View view) {
        if(view.findViewById(R.id.chip1) != null)
            view.findViewById(R.id.chip1).setOnClickListener(v -> { etMessage.setText("Dựa trên BMI của tôi, tôi nên tập gì?"); sendMessage(); });
        if(view.findViewById(R.id.chip2) != null)
            view.findViewById(R.id.chip2).setOnClickListener(v -> { etMessage.setText("Tôi đã gần đạt mục tiêu hôm nay chưa?"); sendMessage(); });
        if(view.findViewById(R.id.chip3) != null)
            view.findViewById(R.id.chip3).setOnClickListener(v -> { etMessage.setText("Lời khuyên để hoàn thành mục tiêu?"); sendMessage(); });
    }

    private void sendMessage() {
        String query = etMessage.getText().toString().trim();
        if (query.isEmpty()) return;

        addMessage(new ChatMessage(query, ChatMessage.TYPE_USER));
        etMessage.setText("");

        fetchContextAndCallAI(query);
    }

    private void fetchContextAndCallAI(String userText) {
        chatProgressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            StringBuilder contextBuilder = new StringBuilder();
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                String uid = FocusLifeApp.getInstance().getSessionManager().requireUid();

                // 1. Lấy thông tin Profile (BMI, Mục tiêu)
                ProfileEntity profile = db.profileDao().getByUid(uid);
                if (profile != null) {
                    float heightM = profile.heightCm / 100;
                    float bmi = (heightM > 0) ? profile.weightKg / (heightM * heightM) : 0;
                    contextBuilder.append(String.format("Người dùng tên: %s. Thể trạng: Cao %.0fcm, Nặng %.1fkg, BMI: %.1f. Mục tiêu chính: %s. ",
                            profile.displayName, profile.heightCm, profile.weightKg, bmi, profile.primaryGoal));
                }

                // 2. Lấy dữ liệu vận động hôm nay
                String today = DateUtils.todayKey();
                DailySummaryEntity summary = db.dailySummaryDao().getByDate(today);
                if (summary != null) {
                    contextBuilder.append(String.format("Hôm nay đã đi: %d bước, Nạp: %d kcal, Tiêu thụ: %.1f kcal, Nước: %d ml.",
                            summary.steps, summary.nutritionCalories, summary.calories, summary.waterMl));
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi lấy dữ liệu ngữ cảnh", e);
            }

            callGeminiAPI(userText, contextBuilder.toString());
        });
    }

    private void callGeminiAPI(String userText, String healthContext) {
        String responseText = "";
        try {
            URL url = new URL(GEMINI_URL);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);

            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject partObj = new JSONObject();

            String prompt = "Bạn là chuyên gia sức khỏe FocusLife AI. Ngữ cảnh người dùng: " + healthContext +
                    "\nHãy trả lời câu hỏi sau bằng tiếng Việt, gọi tên người dùng nếu có, tư vấn bám sát thể trạng và mục tiêu của họ: " + userText;

            partObj.put("text", prompt);
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
                // Đọc lỗi chi tiết từ Server
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                Log.e(TAG, "API Error (" + responseCode + "): " + sb.toString());
                responseText = "AI Coach đang bận phân tích dữ liệu (Lỗi " + responseCode + "), bạn thử lại sau nhé.";
            }
        } catch (Exception e) {
            Log.e(TAG, "Connection Error", e);
            responseText = "Lỗi kết nối: " + e.getMessage();
        }

        final String finalResponse = responseText;
        mainHandler.post(() -> {
            if (isAdded()) {
                chatProgressBar.setVisibility(View.GONE);
                addMessage(new ChatMessage(finalResponse, ChatMessage.TYPE_AI));
            }
        });
    }

    private void addMessage(ChatMessage message) {
        messageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvChat.scrollToPosition(messageList.size() - 1);
    }
}
