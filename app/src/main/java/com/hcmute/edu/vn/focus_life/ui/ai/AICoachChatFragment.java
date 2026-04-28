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

import com.hcmute.edu.vn.focus_life.BuildConfig;
import com.hcmute.edu.vn.focus_life.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class AICoachChatFragment extends Fragment {

    private static final String TAG = "AICoach";

    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private static final int MAX_HISTORY_MESSAGES = 8;

    private RecyclerView rvChat;
    private AICoachAdapter adapter;
    private List<ChatMessage> messageList;

    private EditText etMessage;
    private View btnSend;
    private ProgressBar chatProgressBar;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.activity_ai_coach_chat, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupRecyclerView();
        setupActions(view);
    }

    private void bindViews(@NonNull View view) {
        rvChat = view.findViewById(R.id.rvChat);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        chatProgressBar = view.findViewById(R.id.chatProgressBar);

        if (rvChat == null || etMessage == null || btnSend == null || chatProgressBar == null) {
            throw new IllegalStateException(
                    "activity_ai_coach_chat.xml is missing required views: rvChat, etMessage, btnSend or chatProgressBar"
            );
        }
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();

        messageList.add(new ChatMessage(
                "Chào bạn! Tôi là FocusLife AI. Bạn có thể hỏi tôi về bài tập, ăn uống lành mạnh hoặc mẹo tập trung nhé.",
                ChatMessage.TYPE_AI
        ));

        adapter = new AICoachAdapter(messageList);

        rvChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChat.setAdapter(adapter);
        rvChat.scrollToPosition(messageList.size() - 1);
    }

    private void setupActions(@NonNull View view) {
        btnSend.setOnClickListener(v -> sendMessage());
        setupChips(view);
    }

    private void setupChips(@NonNull View view) {
        View chip1 = view.findViewById(R.id.chip1);
        View chip2 = view.findViewById(R.id.chip2);
        View chip3 = view.findViewById(R.id.chip3);

        if (chip1 != null) {
            chip1.setOnClickListener(v -> {
                etMessage.setText("Hãy gợi ý cho tôi một bài tập tại nhà 15 phút, không cần dụng cụ, phù hợp cho sinh viên ít vận động.");
                sendMessage();
            });
        }

        if (chip2 != null) {
            chip2.setOnClickListener(v -> {
                etMessage.setText("Hãy gợi ý thực đơn ăn uống lành mạnh trong một ngày cho sinh viên, dễ nấu và tiết kiệm chi phí.");
                sendMessage();
            });
        }

        if (chip3 != null) {
            chip3.setOnClickListener(v -> {
                etMessage.setText("Hãy gợi ý 5 mẹo giúp tôi tập trung học tập và làm việc hiệu quả hơn.");
                sendMessage();
            });
        }
    }

    private void sendMessage() {
        if (etMessage == null) return;

        String query = etMessage.getText() == null
                ? ""
                : etMessage.getText().toString().trim();

        if (query.isEmpty()) {
            return;
        }

        addMessage(new ChatMessage(query, ChatMessage.TYPE_USER));
        etMessage.setText("");

        callGeminiAPI();
    }

    private void addMessage(ChatMessage message) {
        if (messageList == null || adapter == null || rvChat == null) {
            return;
        }

        messageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvChat.scrollToPosition(messageList.size() - 1);
    }

    private void callGeminiAPI() {
        if (chatProgressBar != null) {
            chatProgressBar.setVisibility(View.VISIBLE);
        }

        if (btnSend != null) {
            btnSend.setEnabled(false);
        }

        List<ChatMessage> historySnapshot = createHistorySnapshot();

        executorService.execute(() -> {
            String responseText;
            HttpsURLConnection conn = null;

            try {
                String apiKey = BuildConfig.GEMINI_API_KEY;

                if (apiKey == null || apiKey.trim().isEmpty()) {
                    responseText = "AI Coach chưa được cấu hình API key Gemini. Bạn kiểm tra lại local.properties nha.";
                } else {
                    URL url = new URL(GEMINI_BASE_URL + "?key=" + apiKey.trim());

                    conn = (HttpsURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.setDoOutput(true);

                    JSONObject jsonBody = buildGeminiRequestBody(historySnapshot);

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                        os.write(input);
                        os.flush();
                    }

                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        String successBody = readStream(conn.getInputStream());
                        responseText = parseGeminiResponse(successBody);
                    } else {
                        InputStream errorStream = conn.getErrorStream() != null
                                ? conn.getErrorStream()
                                : conn.getInputStream();

                        String errorBody = readStream(errorStream);
                        Log.e(TAG, "Gemini API error " + responseCode + ": " + errorBody);

                        responseText = buildErrorMessage(responseCode, errorBody);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "callGeminiAPI failed", e);
                responseText = "AI Coach đang gặp lỗi kết nối. Bạn thử lại sau nhé.";
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            final String finalResponseText = responseText;

            mainHandler.post(() -> {
                if (!isAdded() || chatProgressBar == null || btnSend == null) {
                    return;
                }

                chatProgressBar.setVisibility(View.GONE);
                btnSend.setEnabled(true);

                addMessage(new ChatMessage(finalResponseText, ChatMessage.TYPE_AI));
            });
        });
    }

    private List<ChatMessage> createHistorySnapshot() {
        List<ChatMessage> snapshot = new ArrayList<>();

        if (messageList == null || messageList.isEmpty()) {
            return snapshot;
        }

        int startIndex = Math.max(0, messageList.size() - MAX_HISTORY_MESSAGES);

        for (int i = startIndex; i < messageList.size(); i++) {
            ChatMessage message = messageList.get(i);
            if (message != null && message.getContent() != null && !message.getContent().trim().isEmpty()) {
                snapshot.add(message);
            }
        }

        return snapshot;
    }

    private JSONObject buildGeminiRequestBody(List<ChatMessage> historySnapshot) throws Exception {
        JSONObject jsonBody = new JSONObject();

        JSONArray contentsArray = new JSONArray();
        JSONObject contentObj = new JSONObject();
        JSONArray partsArray = new JSONArray();
        JSONObject partObj = new JSONObject();

        String prompt = buildPrompt(historySnapshot);

        partObj.put("text", prompt);
        partsArray.put(partObj);
        contentObj.put("parts", partsArray);
        contentsArray.put(contentObj);

        jsonBody.put("contents", contentsArray);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.45);
        generationConfig.put("topP", 0.9);
        generationConfig.put("maxOutputTokens", 700);
        jsonBody.put("generationConfig", generationConfig);

        return jsonBody;
    }

    private String buildPrompt(List<ChatMessage> historySnapshot) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Bạn là FocusLife AI, trợ lý sức khỏe, dinh dưỡng và tập trung cá nhân trong ứng dụng Android FocusLife.\n\n");

        prompt.append("QUY TẮC TRẢ LỜI:\n");
        prompt.append("- Luôn trả lời bằng tiếng Việt.\n");
        prompt.append("- Không tự giới thiệu lại trong mỗi câu trả lời.\n");
        prompt.append("- Không lặp lại câu 'Tôi là FocusLife AI' nếu không cần thiết.\n");
        prompt.append("- Nếu người dùng chỉ chào hỏi, hãy chào ngắn gọn rồi hỏi họ muốn hỗ trợ gì.\n");
        prompt.append("- Trả lời cụ thể, thực tế, dễ làm theo.\n");
        prompt.append("- Ưu tiên gạch đầu dòng ngắn gọn.\n");
        prompt.append("- Không trả lời chung chung kiểu 'bạn nên tập luyện thường xuyên'.\n");
        prompt.append("- Không chẩn đoán bệnh, không kê đơn thuốc và không thay thế tư vấn bác sĩ.\n");
        prompt.append("- Nếu người dùng hỏi về bài tập, hãy đưa lịch tập cụ thể gồm thời lượng, động tác, số hiệp hoặc số lần.\n");
        prompt.append("- Nếu người dùng hỏi về ăn uống, hãy đưa thực đơn cụ thể theo bữa sáng, trưa, tối và bữa phụ.\n");
        prompt.append("- Nếu người dùng hỏi về tập trung, hãy đưa mẹo cụ thể có thể áp dụng ngay.\n");
        prompt.append("- Nếu câu hỏi liên quan sức khỏe nghiêm trọng, hãy khuyên người dùng gặp bác sĩ/chuyên gia.\n\n");

        prompt.append("NGỮ CẢNH HỘI THOẠI GẦN ĐÂY:\n");

        if (historySnapshot == null || historySnapshot.isEmpty()) {
            prompt.append("Chưa có ngữ cảnh.\n");
        } else {
            for (ChatMessage message : historySnapshot) {
                String role = message.getType() == ChatMessage.TYPE_USER
                        ? "Người dùng"
                        : "FocusLife AI";

                prompt.append(role)
                        .append(": ")
                        .append(message.getContent().trim())
                        .append("\n");
            }
        }

        prompt.append("\nHãy trả lời tin nhắn cuối cùng của Người dùng dựa trên ngữ cảnh trên.");
        prompt.append("\nCâu trả lời nên tự nhiên, không quá dài, phù hợp hiển thị trong bong bóng chat mobile.");

        return prompt.toString();
    }

    private String parseGeminiResponse(String responseBody) {
        try {
            JSONObject responseJson = new JSONObject(responseBody);

            JSONArray candidates = responseJson.optJSONArray("candidates");

            if (candidates == null || candidates.length() == 0) {
                return "AI Coach chưa có phản hồi phù hợp. Bạn thử hỏi lại cụ thể hơn nhé.";
            }

            JSONObject firstCandidate = candidates.getJSONObject(0);
            JSONObject content = firstCandidate.optJSONObject("content");

            if (content == null) {
                return "AI Coach chưa có nội dung phản hồi. Bạn thử lại sau nhé.";
            }

            JSONArray parts = content.optJSONArray("parts");

            if (parts == null || parts.length() == 0) {
                return "AI Coach chưa tạo được câu trả lời. Bạn thử lại sau nhé.";
            }

            StringBuilder result = new StringBuilder();

            for (int i = 0; i < parts.length(); i++) {
                JSONObject part = parts.getJSONObject(i);
                String text = part.optString("text", "").trim();

                if (!text.isEmpty()) {
                    if (result.length() > 0) {
                        result.append("\n");
                    }
                    result.append(text);
                }
            }

            if (result.length() == 0) {
                return "AI Coach chưa có phản hồi rõ ràng. Bạn thử lại nhé.";
            }

            return result.toString().trim();
        } catch (Exception e) {
            Log.e(TAG, "parseGeminiResponse failed: " + responseBody, e);
            return "AI Coach đã nhận phản hồi nhưng không đọc được nội dung. Bạn thử lại sau nhé.";
        }
    }

    private String buildErrorMessage(int responseCode, String errorBody) {
        String safeErrorBody = errorBody == null ? "" : errorBody;

        if (responseCode == 400) {
            return "Yêu cầu gửi đến AI chưa hợp lệ. Bạn thử nhập câu hỏi ngắn gọn và rõ hơn nhé.";
        }

        if (responseCode == 401 || responseCode == 403
                || safeErrorBody.contains("PERMISSION_DENIED")
                || safeErrorBody.contains("API_KEY_INVALID")
                || safeErrorBody.contains("API key")) {
            return "AI Coach hiện chưa thể phản hồi. Bạn kiểm tra lại API key Gemini trong local.properties nha.";
        }

        if (responseCode == 429
                || safeErrorBody.toLowerCase().contains("quota")
                || safeErrorBody.toLowerCase().contains("rate limit")) {
            return "AI Coach đang bị giới hạn lượt gọi. Bạn chờ một lát rồi thử lại nhé.";
        }

        if (responseCode >= 500) {
            return "Máy chủ AI đang bận. Bạn thử lại sau ít phút nhé.";
        }

        return "AI Coach đang gặp lỗi kết nối. Bạn thử lại sau nhé.";
    }

    private String readStream(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }

        return sb.toString();
    }

    @Override
    public void onDestroyView() {
        mainHandler.removeCallbacksAndMessages(null);

        rvChat = null;
        adapter = null;
        etMessage = null;
        btnSend = null;
        chatProgressBar = null;

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}