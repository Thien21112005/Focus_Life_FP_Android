package com.hcmute.edu.vn.focus_life.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.motivation.MotivationPreferences;
import com.hcmute.edu.vn.focus_life.core.motivation.MotivationQuote;
import com.hcmute.edu.vn.focus_life.core.motivation.MotivationReminderScheduler;
import com.hcmute.edu.vn.focus_life.core.motivation.MotivationRepository;
import com.hcmute.edu.vn.focus_life.core.session.OnboardingPreferences;
import com.hcmute.edu.vn.focus_life.data.repository.ProfileRepository;
import com.hcmute.edu.vn.focus_life.domain.model.UserProfile;
import com.hcmute.edu.vn.focus_life.ui.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private TextView tvGreeting;
    private TextView tvSubtitle;
    private TextView tvMotivationQuote;
    private TextView tvMotivationTime;
    private TextView tvDailyTip;
    private MotivationPreferences motivationPreferences;
    private OnboardingPreferences onboardingPreferences;
    private String displayName = "bạn";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_panel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        onboardingPreferences = new OnboardingPreferences(requireContext());
        motivationPreferences = new MotivationPreferences(requireContext());
        displayName = resolveLocalName();

        TextView tvBrand = view.findViewById(R.id.tvHomeBrand);
        tvGreeting = view.findViewById(R.id.tvHomeGreeting);
        tvSubtitle = view.findViewById(R.id.tvHomeSubtitle);
        TextView tvTodayLabel = view.findViewById(R.id.tvTodayLabel);
        tvMotivationQuote = view.findViewById(R.id.tvMotivationQuote);
        tvMotivationTime = view.findViewById(R.id.tvMotivationTime);
        tvDailyTip = view.findViewById(R.id.tvDailyTip);
        View btnNextMotivation = view.findViewById(R.id.btnNextMotivation);
        View cardFocusStart = view.findViewById(R.id.cardFocusStart);
        View btnStartFocus = view.findViewById(R.id.btnStartFocus);
        View cardHealthShortcut = view.findViewById(R.id.cardHealthShortcut);
        View cardDiaryShortcut = view.findViewById(R.id.cardDiaryShortcut);

        if (tvBrand != null) tvBrand.setText("FocusLife");
        if (tvTodayLabel != null) tvTodayLabel.setText(formatToday());
        bindGreeting();
        bindMotivation(MotivationRepository.getQuoteForNow());

        if (btnNextMotivation != null) {
            btnNextMotivation.setOnClickListener(v -> {
                int index = motivationPreferences.nextQuoteCursor(MotivationRepository.getQuoteCount());
                bindMotivation(MotivationRepository.getQuoteByIndex(index));
            });
        }

        View.OnClickListener openFocus = v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openTab(MainActivity.TAB_FOCUS);
            }
        };
        if (cardFocusStart != null) cardFocusStart.setOnClickListener(openFocus);
        if (btnStartFocus != null) btnStartFocus.setOnClickListener(openFocus);

        if (cardHealthShortcut != null) {
            cardHealthShortcut.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).openTab(MainActivity.TAB_HEALTH);
                }
            });
        }
        if (cardDiaryShortcut != null) {
            cardDiaryShortcut.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).openTab(MainActivity.TAB_DIARY);
                }
            });
        }

        loadProfileName();
        MotivationReminderScheduler.scheduleConfiguredDailyReminder(requireContext());
    }

    private void loadProfileName() {
        new ProfileRepository(requireActivity()).getCurrentProfile(profile -> {
            if (!isAdded()) return;
            String name = null;
            if (profile != null) {
                name = profile.displayName;
            }
            displayName = safeName(name, resolveLocalName());
            bindGreeting();
        });
    }

    private void bindGreeting() {
        if (tvGreeting != null) tvGreeting.setText(buildGreeting(displayName));
        if (tvSubtitle != null) tvSubtitle.setText(buildSubtitle());
    }

    private void bindMotivation(MotivationQuote quote) {
        if (tvMotivationQuote != null) tvMotivationQuote.setText(quote.getText());
        if (tvMotivationTime != null) {
            String state = motivationPreferences.isEnabled()
                    ? "Nhắc động lực lúc " + motivationPreferences.getReminderTimeText() + " và khi mở khóa điện thoại"
                    : "Thông báo động lực đang tắt";
            tvMotivationTime.setText(state);
        }
        if (tvDailyTip != null) tvDailyTip.setText(MotivationRepository.getDailyTip());
    }

    private String resolveLocalName() {
        String name = onboardingPreferences == null ? "" : onboardingPreferences.getDisplayName();
        if (name == null || name.trim().isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                name = user.getDisplayName();
            }
        }
        return safeName(name, "bạn");
    }

    private String safeName(String name, String fallback) {
        if (name == null || name.trim().isEmpty()) return fallback;
        return name.trim();
    }

    private String buildGreeting(String displayName) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 11) return "Chào buổi sáng, " + displayName;
        if (hour < 14) return "Chào buổi trưa, " + displayName;
        if (hour < 18) return "Chào buổi chiều, " + displayName;
        return "Chào buổi tối, " + displayName;
    }

    private String buildSubtitle() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 11) return "Bắt đầu ngày mới bằng một nhịp sống khỏe hơn và một việc quan trọng.";
        if (hour < 14) return "Giữ năng lượng ổn định: ăn vừa đủ, uống nước và nghỉ ngắn đúng lúc.";
        if (hour < 18) return "Đi thêm một chút, uống thêm một ly nước và hoàn thành phần việc quan trọng.";
        return "Khép ngày nhẹ nhàng, nhìn lại điều tốt bạn đã làm cho sức khỏe hôm nay.";
    }

    private String formatToday() {
        try {
            return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date());
        } catch (Exception ignored) {
            return "Hôm nay";
        }
    }
}
