package com.hcmute.edu.vn.focus_life.core.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final SimpleDateFormat DATE_KEY = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DAY_DOC_KEY = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

    public static String todayKey() {
        return DATE_KEY.format(new Date());
    }

    public static String dayDocumentKey(long timeMillis) {
        return DAY_DOC_KEY.format(new Date(timeMillis));
    }
}
