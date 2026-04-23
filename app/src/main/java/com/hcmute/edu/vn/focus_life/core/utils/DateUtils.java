package com.hcmute.edu.vn.focus_life.core.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final Locale LOCALE_VI = new Locale("vi", "VN");

    private static SimpleDateFormat dateKeyFormat() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    private static SimpleDateFormat dayDocFormat() {
        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    }

    public static String todayKey() {
        return dateKeyFormat().format(new Date());
    }

    public static String dayDocumentKey(long timeMillis) {
        return dayDocFormat().format(new Date(timeMillis));
    }

    public static Date parseDateKey(String dateKey) {
        try {
            return dateKeyFormat().parse(dateKey);
        } catch (ParseException e) {
            return new Date();
        }
    }

    public static String shiftDate(String dateKey, int dayOffset) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(parseDateKey(dateKey));
        calendar.add(Calendar.DAY_OF_YEAR, dayOffset);
        return dateKeyFormat().format(calendar.getTime());
    }

    public static String formatDiaryDate(String dateKey) {
        Date selectedDate = parseDateKey(dateKey);
        Calendar today = Calendar.getInstance();
        zeroTime(today);

        Calendar compare = Calendar.getInstance();
        compare.setTime(selectedDate);
        zeroTime(compare);

        long diffDays = (compare.getTimeInMillis() - today.getTimeInMillis()) / (24L * 60L * 60L * 1000L);
        String suffix = new SimpleDateFormat("dd/MM", LOCALE_VI).format(selectedDate);
        if (diffDays == 0) return "Hôm nay, " + suffix;
        if (diffDays == -1) return "Hôm qua, " + suffix;
        if (diffDays == 1) return "Ngày mai, " + suffix;
        return new SimpleDateFormat("EEE, dd/MM", LOCALE_VI).format(selectedDate);
    }

    public static String formatShortDate(String dateKey) {
        return new SimpleDateFormat("dd/MM", LOCALE_VI).format(parseDateKey(dateKey));
    }

    private static void zeroTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
