package com.hcmute.edu.vn.focus_life.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.hcmute.edu.vn.focus_life.R;
import com.hcmute.edu.vn.focus_life.core.water.WaterReminderScheduler;
import com.hcmute.edu.vn.focus_life.data.repository.HealthMetricsRepository;

import java.text.DecimalFormat;
import java.util.Locale;

public class DashboardScreenFragment extends Fragment {

    private static final String ARG_LAYOUT_RES = "arg_layout_res";
    private static final int DEFAULT_WATER_GLASSES = 8;
    private static final int DEFAULT_WATER_START = 8;
    private static final int DEFAULT_WATER_END = 22;

    private HealthMetricsRepository healthRepository;
    private int todayWaterGlassesCache = 0;

    public static DashboardScreenFragment newInstance(int layoutRes) {
        DashboardScreenFragment fragment = new DashboardScreenFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RES, layoutRes);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        int layoutRes = R.layout.activity_widget_preview;
        if (getArguments() != null) {
            layoutRes = getArguments().getInt(ARG_LAYOUT_RES, R.layout.activity_widget_preview);
        }
        return inflater.inflate(layoutRes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        healthRepository = new HealthMetricsRepository();

        View bottomNav = view.findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);

        View btnChatAI = view.findViewById(R.id.btnChatAI);
        if (btnChatAI != null) {
            btnChatAI.setOnClickListener(v -> openTab(MainActivity.TAB_AI_COACH));
        }

        int layoutRes = R.layout.activity_widget_preview;
        if (getArguments() != null) layoutRes = getArguments().getInt(ARG_LAYOUT_RES, R.layout.activity_widget_preview);

        if (layoutRes == R.layout.activity_health_tracker) {
            bindHealthTracker(view);
        } else if (layoutRes == R.layout.activity_set_month_goal) {
            bindMonthlyGoal(view);
        } else if (layoutRes == R.layout.activity_monthly_report) {
            bindMonthlyReport(view);
        }
    }

    private void bindHealthTracker(View view) {
        View btnTarget = view.findViewById(R.id.btnOpenMonthlyGoal);
        if (btnTarget != null) btnTarget.setOnClickListener(v -> openTab(MainActivity.TAB_TARGET));
        View btnReport = view.findViewById(R.id.btnOpenMonthlyReport);
        if (btnReport != null) btnReport.setOnClickListener(v -> openTab(MainActivity.TAB_REPORT));

        View btnWater = view.findViewById(R.id.btnLogWater);
        if (btnWater != null) {
            btnWater.setOnClickListener(v -> healthRepository.logWaterGlass((success, message) -> {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    loadHealthReport(view);
                }
            }));
        }

        bindWaterReminderSettings(view);
        updateReminderButton(view);
        updateWaterScheduleText(view);
        loadHealthReport(view);
    }

    private void bindMonthlyGoal(View view) {
        EditText etSteps = view.findViewById(R.id.etGoalSteps);
        EditText etCalories = view.findViewById(R.id.etGoalCalories);
        EditText etFocus = view.findViewById(R.id.etGoalFocusMinutes);

        for (EditText editText : new EditText[]{etSteps, etCalories, etFocus}) {
            if (editText != null) editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        }

        loadGoalIntoForm(view);
        View reload = view.findViewById(R.id.btnReloadMonthlyGoal);
        if (reload != null) reload.setOnClickListener(v -> loadGoalIntoForm(view));

        View save = view.findViewById(R.id.btnSaveMonthlyGoal);
        if (save != null) {
            save.setOnClickListener(v -> {
                HealthMetricsRepository.MonthlyGoal goal = HealthMetricsRepository.MonthlyGoal.defaultForCurrentMonth();
                goal.stepTarget = 0;
                goal.runningDistanceKmTarget = readInt(etSteps, 0);
                goal.netCalorieTarget = readInt(etCalories, 0);
                goal.focusMinuteTarget = readInt(etFocus, 0);
                goal.waterGlassTargetPerDay = DEFAULT_WATER_GLASSES;
                healthRepository.saveMonthlyGoal(goal, (success, message) -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    setText(view, R.id.tvGoalStatus, success ? "Đã lưu mục tiêu tháng" : message);
                });
            });
        }

        View delete = view.findViewById(R.id.btnDeleteMonthlyGoal);
        if (delete != null) {
            delete.setOnClickListener(v -> healthRepository.deleteCurrentMonthlyGoal((success, message) -> {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                loadGoalIntoForm(view);
            }));
        }
    }

    private void bindMonthlyReport(View view) {
        View btnRefresh = view.findViewById(R.id.btnRefreshReport);
        if (btnRefresh != null) btnRefresh.setOnClickListener(v -> loadMonthlyReport(view));
        View btnGoal = view.findViewById(R.id.btnOpenGoalFromReport);
        if (btnGoal != null) btnGoal.setOnClickListener(v -> openTab(MainActivity.TAB_TARGET));
        loadMonthlyReport(view);
    }

    private void loadHealthReport(View view) {
        setLoading(view, true);
        healthRepository.loadMonthlyReport((report, error) -> {
            if (!isAdded()) return;
            setLoading(view, false);
            setText(view, R.id.tvHealthMonth, formatMonth(report.monthKey));
            setText(view, R.id.tvHealthSteps, runningGoalMainText(report));
            setText(view, R.id.tvHealthStepsTarget, runningGoalProgressText(report));
            setProgress(view, R.id.progressHealthSteps, report.runningDistancePercent());

            setText(view, R.id.tvHealthNetCalories, formatNumber(report.netCalories) + " kcal");
            String calorieText = caloriesGoalProgressText(report);
            setText(view, R.id.tvHealthNetCaloriesTarget, calorieText);
            setProgress(view, R.id.progressHealthCalories, report.netCaloriePercent());

            setText(view, R.id.tvHealthFocusMinutes, formatFocus(report.focusMinutes));
            setText(view, R.id.tvHealthAppUsage, focusGoalProgressText(report) + " · Hoạt động app: " + formatMillis(report.appActiveMillis));
            setProgress(view, R.id.progressHealthFocus, report.focusPercent());

            int waterTarget = effectiveWaterTarget(report.goal);
            todayWaterGlassesCache = Math.max(0, report.todayWaterGlasses);
            setText(view, R.id.tvHealthWater, report.todayWaterGlasses + "/" + waterTarget + " ly");
            setText(view, R.id.tvHealthWaterTarget, report.todayWaterMl + " ml hôm nay");
            setProgress(view, R.id.progressHealthWater, HealthMetricsRepository.percent(report.todayWaterGlasses, waterTarget));

            setText(view, R.id.tvHealthInsight, buildHealthInsight(report));
            updateWaterScheduleCards(view, currentWaterStartHour(), currentWaterEndHour(), waterTarget, report.todayWaterGlasses);
            renderWaterReminderLockState(view, todayWaterGlassesCache);
            setText(view, R.id.tvWaterSchedule, waterScheduleStatusText(todayWaterGlassesCache));
        });
    }

    private void loadMonthlyReport(View view) {
        setLoading(view, true);
        healthRepository.loadMonthlyReport((report, error) -> {
            if (!isAdded()) return;
            setLoading(view, false);
            setText(view, R.id.tvReportMonth, "Báo cáo " + formatMonth(report.monthKey));
            setText(view, R.id.tvReportSteps, runningGoalMainText(report));
            setText(view, R.id.tvReportStepsSub, runningGoalProgressText(report));
            setText(view, R.id.tvReportCalories, formatNumber(report.netCalories) + " kcal");
            setText(view, R.id.tvReportCaloriesSub, caloriesGoalProgressText(report));
            setText(view, R.id.tvReportFocus, formatFocus(report.focusMinutes));
            setText(view, R.id.tvReportFocusSub, focusGoalProgressText(report));
            setText(view, R.id.tvReportUsage, formatMillis(report.appActiveMillis));
            int monthlyWaterTarget = report.monthlyWaterTarget();
            setText(view, R.id.tvReportWater, report.monthlyWaterGlasses + " / " + monthlyWaterTarget + " ly");
            setText(view, R.id.tvReportWaterSub, monthlyWaterSubText(report, monthlyWaterTarget));
            setProgress(view, R.id.progressReportWater, HealthMetricsRepository.percent(report.monthlyWaterGlasses, monthlyWaterTarget));
            setText(view, R.id.tvReportInsight, buildReportInsight(report));
        });
    }

    private void loadGoalIntoForm(View view) {
        healthRepository.loadMonthlyGoal((goal, error) -> {
            if (!isAdded()) return;
            setText(view, R.id.tvGoalMonth, "Thiết lập cho " + formatMonth(goal.monthKey));
            setEditTextOptional(view, R.id.etGoalSteps, goal.runningDistanceKmTarget);
            setEditTextOptional(view, R.id.etGoalCalories, goal.netCalorieTarget);
            setEditTextOptional(view, R.id.etGoalFocusMinutes, goal.focusMinuteTarget);
            setText(view, R.id.tvGoalStatus, goal.hasSavedGoal ? "Mục tiêu tháng đã được tải." : "Chưa có mục tiêu tháng. Hãy nhập những mục bạn muốn theo dõi.");
        });
    }

    private void bindWaterReminderSettings(View view) {
        EditText etStart = view.findViewById(R.id.etWaterReminderStartHour);
        EditText etEnd = view.findViewById(R.id.etWaterReminderEndHour);
        if (etStart != null) etStart.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        if (etEnd != null) etEnd.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});

        int savedStart = WaterReminderScheduler.isEnabled(requireContext()) ? WaterReminderScheduler.startHour(requireContext()) : DEFAULT_WATER_START;
        int savedEnd = WaterReminderScheduler.isEnabled(requireContext()) ? WaterReminderScheduler.endHour(requireContext()) : DEFAULT_WATER_END;
        if (etStart != null) etStart.setText(String.valueOf(savedStart));
        if (etEnd != null) etEnd.setText(String.valueOf(savedEnd));

        View btnSave = view.findViewById(R.id.btnSaveWaterReminderSettings);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveWaterReminderSettingsIfAllowed(view, etStart, etEnd));
        }

        View btnReminder = view.findViewById(R.id.btnWaterReminder);
        if (btnReminder != null) {
            btnReminder.setOnClickListener(v -> toggleWaterReminderIfAllowed(view, etStart, etEnd));
        }
    }

    private void saveWaterReminderSettingsIfAllowed(View view, EditText etStart, EditText etEnd) {
        healthRepository.loadTodayWaterGlasses((glasses, error) -> {
            if (!isAdded()) return;
            todayWaterGlassesCache = Math.max(0, glasses);
            if (todayWaterGlassesCache > 0) {
                blockWaterReminderChange(view, todayWaterGlassesCache);
                return;
            }
            int start = clamp(readInt(etStart, DEFAULT_WATER_START), 0, 23);
            int end = clamp(readInt(etEnd, DEFAULT_WATER_END), 0, 23);
            requestNotificationPermissionIfNeeded();
            WaterReminderScheduler.scheduleDailyCups(requireContext(), start, end, DEFAULT_WATER_GLASSES);
            healthRepository.saveWaterReminderSettings(true, start, end, DEFAULT_WATER_GLASSES);
            Toast.makeText(requireContext(), "Đã lưu lịch nhắc uống nước", Toast.LENGTH_SHORT).show();
            if (etStart != null) etStart.setText(String.valueOf(start));
            if (etEnd != null) etEnd.setText(String.valueOf(end));
            updateReminderButton(view);
            updateWaterScheduleText(view);
        });
    }

    private void toggleWaterReminderIfAllowed(View view, EditText etStart, EditText etEnd) {
        healthRepository.loadTodayWaterGlasses((glasses, error) -> {
            if (!isAdded()) return;
            todayWaterGlassesCache = Math.max(0, glasses);
            if (todayWaterGlassesCache > 0) {
                blockWaterReminderChange(view, todayWaterGlassesCache);
                return;
            }
            int start = clamp(readInt(etStart, DEFAULT_WATER_START), 0, 23);
            int end = clamp(readInt(etEnd, DEFAULT_WATER_END), 0, 23);
            if (WaterReminderScheduler.isEnabled(requireContext())) {
                WaterReminderScheduler.cancel(requireContext());
                healthRepository.saveWaterReminderSettings(false, start, end, DEFAULT_WATER_GLASSES);
                Toast.makeText(requireContext(), "Đã tắt nhắc uống nước", Toast.LENGTH_SHORT).show();
            } else {
                requestNotificationPermissionIfNeeded();
                WaterReminderScheduler.scheduleDailyCups(requireContext(), start, end, DEFAULT_WATER_GLASSES);
                healthRepository.saveWaterReminderSettings(true, start, end, DEFAULT_WATER_GLASSES);
                Toast.makeText(requireContext(), "Đã bật lịch nhắc uống nước", Toast.LENGTH_SHORT).show();
                if (etStart != null) etStart.setText(String.valueOf(start));
                if (etEnd != null) etEnd.setText(String.valueOf(end));
            }
            updateReminderButton(view);
            updateWaterScheduleText(view);
        });
    }

    private void blockWaterReminderChange(View view, int glasses) {
        String message = "Hôm nay bạn đã uống " + glasses + "/8 ly nên giờ nhắc đã được khóa. Bạn có thể đổi lịch vào ngày mai.";
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        setText(view, R.id.tvWaterSchedule, message);
        renderWaterReminderLockState(view, glasses);
    }


    private void renderWaterReminderLockState(View view, int glasses) {
        boolean locked = glasses > 0;
        EditText etStart = view.findViewById(R.id.etWaterReminderStartHour);
        EditText etEnd = view.findViewById(R.id.etWaterReminderEndHour);
        TextView btnSave = view.findViewById(R.id.btnSaveWaterReminderSettings);
        TextView btnReminder = view.findViewById(R.id.btnWaterReminder);
        if (etStart != null) {
            etStart.setEnabled(!locked);
            etStart.setAlpha(locked ? 0.55f : 1f);
        }
        if (etEnd != null) {
            etEnd.setEnabled(!locked);
            etEnd.setAlpha(locked ? 0.55f : 1f);
        }
        if (btnSave != null) {
            btnSave.setText(locked ? "Đổi giờ vào ngày mai" : "Lưu giờ nhắc");
            btnSave.setAlpha(locked ? 0.65f : 1f);
        }
        if (btnReminder != null) {
            btnReminder.setAlpha(locked ? 0.65f : 1f);
        }
    }

    private String waterScheduleStatusText(int glasses) {
        if (glasses > 0) {
            return "Hôm nay đã uống " + glasses + "/8 ly. Giờ nhắc đã khóa đến ngày mai.";
        }
        return WaterReminderScheduler.isEnabled(requireContext())
                ? "Đang bật nhắc uống nước. Bạn có thể đổi giờ khi hôm nay chưa uống ly nào."
                : "Chưa bật nhắc uống nước. Mặc định 8 ly/ngày.";
    }

    private void updateReminderButton(View view) {
        TextView button = view.findViewById(R.id.btnWaterReminder);
        if (button == null || !isAdded()) return;
        button.setText(WaterReminderScheduler.isEnabled(requireContext()) ? "Tắt nhắc uống nước" : "Bật lịch nhắc uống nước");
    }

    private void updateWaterScheduleText(View view) {
        if (!isAdded()) return;
        int start = WaterReminderScheduler.isEnabled(requireContext()) ? WaterReminderScheduler.startHour(requireContext()) : DEFAULT_WATER_START;
        int end = WaterReminderScheduler.isEnabled(requireContext()) ? WaterReminderScheduler.endHour(requireContext()) : DEFAULT_WATER_END;
        int cups = DEFAULT_WATER_GLASSES;
        updateWaterScheduleCards(view, start, end, cups, todayWaterGlassesCache);
        renderWaterReminderLockState(view, todayWaterGlassesCache);
        setText(view, R.id.tvWaterSchedule, waterScheduleStatusText(todayWaterGlassesCache));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (!isAdded()) return;
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7208);
        }
    }

    private void openTab(String tab) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openTab(tab);
        }
    }

    private String buildHealthInsight(HealthMetricsRepository.MonthlyReport report) {
        if (report.runningDistanceKm <= 0f && report.focusMinutes <= 0 && report.nutritionCalories <= 0) {
            return "Khi bạn bắt đầu ghi nhận chạy bộ, dinh dưỡng và focus, báo cáo tháng sẽ tự cập nhật tại đây.";
        }
        if (report.goal.runningDistanceKmTarget > 0 && report.runningDistancePercent() >= 100) {
            return "Bạn đã đạt mục tiêu chạy bộ tháng này. Tiếp tục giữ nhịp vận động đều nhé.";
        }
        if (report.goal.focusMinuteTarget > 0 && report.focusPercent() >= 100) {
            return "Thời gian focus đang rất ổn. Hãy duy trì thói quen làm việc sâu mỗi ngày.";
        }
        if (report.netCalories > 0 && report.goal.netCalorieTarget > 0 && report.netCalories > report.goal.netCalorieTarget) {
            return "Calo cuối đang cao hơn mục tiêu. Bạn có thể cân bằng lại bữa ăn và vận động nhẹ.";
        }
        if (report.todayWaterGlasses < DEFAULT_WATER_GLASSES) {
            return "Hôm nay bạn đã uống " + report.todayWaterGlasses + "/8 ly. Sang ngày mới chỉ số nước sẽ tự về 0/8 để bắt đầu lại.";
        }
        return "Các chỉ số của bạn đang được cập nhật. Hãy duy trì vận động, focus và uống nước đều mỗi ngày.";
    }

    private String monthlyWaterSubText(HealthMetricsRepository.MonthlyReport report, int monthlyWaterTarget) {
        if (monthlyWaterTarget <= 0) return "Tính riêng theo mục tiêu 8 ly mỗi ngày.";
        int missing = Math.max(0, monthlyWaterTarget - report.monthlyWaterGlasses);
        if (missing == 0) return "Bạn đã đạt đủ mục tiêu nước uống của tháng này.";
        return "Còn thiếu " + missing + " ly so với mục tiêu 8 ly mỗi ngày.";
    }

    private String buildReportInsight(HealthMetricsRepository.MonthlyReport report) {
        int monthlyWaterTarget = report.monthlyWaterTarget();
        if (monthlyWaterTarget > 0 && report.monthlyWaterGlasses < monthlyWaterTarget) {
            int missing = monthlyWaterTarget - report.monthlyWaterGlasses;
            return "Tháng này bạn đã uống " + report.monthlyWaterGlasses + "/" + monthlyWaterTarget + " ly nước, còn thiếu " + missing + " ly so với chỉ tiêu. Hãy chia đều nước uống trong ngày để dễ đạt 8 ly hơn.";
        }
        if (report.goal.runningDistanceKmTarget > 0 && report.runningDistancePercent() < 50) return "Quãng đường chạy tháng này còn thấp. Bạn có thể bắt đầu bằng các buổi chạy ngắn để tăng dần.";
        if (report.goal.netCalorieTarget > 0 && report.netCaloriePercent() > 100) return "Calo cuối đã vượt mục tiêu. Nên cân bằng bữa ăn và vận động thêm.";
        if (report.goal.focusMinuteTarget > 0 && report.focusPercent() < 60) return "Thời gian focus còn thấp. Hãy thử các phiên ngắn 25 phút để dễ duy trì hơn.";
        return "Bạn đang duy trì khá ổn. Tiếp tục giữ nhịp vận động, focus và uống nước đều mỗi ngày.";
    }

    private String runningGoalMainText(HealthMetricsRepository.MonthlyReport report) {
        int target = report.goal == null ? 0 : report.goal.runningDistanceKmTarget;
        if (target <= 0) return formatKm(report.runningDistanceKm);
        return formatKmValue(report.runningDistanceKm) + " / " + target + " km";
    }

    private String runningGoalProgressText(HealthMetricsRepository.MonthlyReport report) {
        int target = report.goal == null ? 0 : report.goal.runningDistanceKmTarget;
        if (target <= 0) return "Chưa đặt mục tiêu";
        return "Đã đạt " + formatKmValue(report.runningDistanceKm) + " / " + target + " km · " + report.runningDistancePercent() + "% mục tiêu";
    }

    private String caloriesGoalProgressText(HealthMetricsRepository.MonthlyReport report) {
        String detail = "Nạp " + formatNumber(report.nutritionCalories) + " - tiêu thụ " + formatNumber(report.burnedCalories) + " kcal";
        int target = report.goal == null ? 0 : report.goal.netCalorieTarget;
        if (target <= 0) return detail + " · Chưa đặt mục tiêu";
        return formatNumber(report.netCalories) + " / " + formatNumber(target) + " kcal · " + report.netCaloriePercent() + "% mục tiêu";
    }

    private String focusGoalProgressText(HealthMetricsRepository.MonthlyReport report) {
        int target = report.goal == null ? 0 : report.goal.focusMinuteTarget;
        if (target <= 0) return "Chưa đặt mục tiêu focus";
        return formatFocus(report.focusMinutes) + " / " + formatFocus(target) + " · " + report.focusPercent() + "% mục tiêu";
    }

    private String targetText(int target, int percent, String targetText) {
        if (target <= 0) return "Chưa đặt mục tiêu";
        return percent + "% mục tiêu · " + targetText;
    }

    private int effectiveWaterTarget(HealthMetricsRepository.MonthlyGoal goal) {
        return DEFAULT_WATER_GLASSES;
    }

    private int currentWaterStartHour() {
        if (isAdded() && WaterReminderScheduler.isEnabled(requireContext())) {
            return WaterReminderScheduler.startHour(requireContext());
        }
        return DEFAULT_WATER_START;
    }

    private int currentWaterEndHour() {
        if (isAdded() && WaterReminderScheduler.isEnabled(requireContext())) {
            return WaterReminderScheduler.endHour(requireContext());
        }
        return DEFAULT_WATER_END;
    }

    private void updateWaterScheduleCards(View root, int startHour, int endHour, int cups, int completedCups) {
        int[] ids = new int[]{R.id.tvWaterCup1, R.id.tvWaterCup2, R.id.tvWaterCup3, R.id.tvWaterCup4, R.id.tvWaterCup5, R.id.tvWaterCup6, R.id.tvWaterCup7, R.id.tvWaterCup8};
        updateScheduleCards(root, ids, startHour, endHour, cups, completedCups);
    }

    private void updateScheduleCards(View root, int[] ids, int startHour, int endHour, int cups, int completedCups) {
        cups = clamp(cups <= 0 ? DEFAULT_WATER_GLASSES : cups, 1, 8);
        startHour = clamp(startHour, 0, 23);
        endHour = clamp(endHour, 0, 23);
        int startMinutes = startHour * 60;
        int endMinutes = endHour * 60;
        if (endMinutes <= startMinutes) endMinutes = startMinutes + 14 * 60;
        int range = Math.max(0, endMinutes - startMinutes);
        for (int i = 0; i < ids.length; i++) {
            TextView tv = root.findViewById(ids[i]);
            if (tv == null) continue;
            if (i >= cups) {
                tv.setVisibility(View.GONE);
                continue;
            }
            int minuteOfDay = cups == 1 ? startMinutes : startMinutes + Math.round(range * (i / (float) (cups - 1)));
            minuteOfDay = ((minuteOfDay % (24 * 60)) + (24 * 60)) % (24 * 60);
            tv.setVisibility(View.VISIBLE);
            tv.setText("Ly " + (i + 1) + "\n" + String.format(Locale.getDefault(), "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60));
            if (i < completedCups) {
                tv.setBackgroundResource(R.drawable.bg_gradient_primary);
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_primary));
            } else {
                tv.setBackgroundResource(R.drawable.bg_card_surface_low);
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface));
            }
        }
    }

    private void setLoading(View root, boolean loading) {
        View progress = root.findViewById(R.id.healthProgressBar);
        if (progress != null) progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void setText(View root, int id, String text) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private void setEditTextOptional(View root, int id, int value) {
        EditText editText = root.findViewById(id);
        if (editText != null) editText.setText(value > 0 ? String.valueOf(value) : "");
    }

    private void setProgress(View root, int id, int percent) {
        ProgressBar progressBar = root.findViewById(id);
        if (progressBar != null) progressBar.setProgress(Math.min(100, Math.max(0, percent)));
    }

    private int readInt(EditText editText, int fallback) {
        if (editText == null || editText.getText() == null) return fallback;
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) return fallback;
        try {
            int value = Integer.parseInt(text);
            return value > 0 ? value : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatKm(float value) {
        return formatKmValue(value) + " km";
    }

    private String formatKmValue(float value) {
        if (value <= 0f) return "0";
        return new DecimalFormat("#,##0.###").format(value);
    }

    private String formatNumber(int value) {
        return new DecimalFormat("#,###").format(value);
    }

    private String formatMonth(String monthKey) {
        if (monthKey == null || monthKey.length() != 7) return "Tháng này";
        return "Tháng " + monthKey.substring(5, 7) + "/" + monthKey.substring(0, 4);
    }

    private String formatFocus(int minutes) {
        int hours = minutes / 60;
        int remain = minutes % 60;
        if (hours <= 0) return remain + " phút";
        if (remain == 0) return hours + " giờ";
        return String.format(Locale.getDefault(), "%d giờ %d phút", hours, remain);
    }

    private String formatMillis(long millis) {
        long minutes = Math.max(0L, millis / 60000L);
        long hours = minutes / 60L;
        long remain = minutes % 60L;
        if (hours <= 0) return remain + " phút";
        if (remain == 0) return hours + " giờ";
        return hours + " giờ " + remain + " phút";
    }
}
