package com.hcmute.edu.vn.focus_life.core.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {

    public static final String TYPE_HEALTH = "health";
    public static final String TYPE_LOCATION = "location";
    public static final String TYPE_NOTIFICATION = "notification";
    public static final String TYPE_EXACT_ALARM = "exact_alarm";

    public static String[] getHealthPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new String[]{Manifest.permission.ACTIVITY_RECOGNITION};
        }
        return new String[0];
    }

    public static String[] getLocationPermissions() {
        return new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
    }

    public static String[] getNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.POST_NOTIFICATIONS};
        }
        return new String[0];
    }

    public static String[] getPermissionsForType(String type) {
        switch (type) {
            case TYPE_HEALTH:
                return getHealthPermissions();
            case TYPE_LOCATION:
                return getLocationPermissions();
            case TYPE_NOTIFICATION:
                return getNotificationPermissions();
            case TYPE_EXACT_ALARM:
                return new String[0];
            default:
                return new String[0];
        }
    }

    public static String[] getOnboardingPermissions() {
        List<String> permissions = new ArrayList<>();
        addAll(permissions, getHealthPermissions());
        addAll(permissions, getLocationPermissions());
        addAll(permissions, getNotificationPermissions());
        return permissions.toArray(new String[0]);
    }

    private static void addAll(List<String> target, String[] items) {
        if (items == null) return;
        for (String item : items) {
            target.add(item);
        }
    }

    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasPermissions(Context context, String[] permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }

        for (String permission : permissions) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasNotificationPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasExactAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        return alarmManager == null || alarmManager.canScheduleExactAlarms();
    }

    public static boolean hasPermissionType(Context context, String type) {
        if (TYPE_EXACT_ALARM.equals(type)) {
            return hasExactAlarmPermission(context);
        }
        if (TYPE_NOTIFICATION.equals(type)) {
            return hasNotificationPermission(context);
        }
        return hasPermissions(context, getPermissionsForType(type));
    }

    public static boolean hasAllOnboardingPermissions(Context context) {
        return hasPermissions(context, getOnboardingPermissions()) && hasExactAlarmPermission(context);
    }

    public static void requestOnboardingPermissions(ActivityResultLauncher<String[]> launcher) {
        if (launcher != null) {
            launcher.launch(getOnboardingPermissions());
        }
    }

    public static void requestPermissionType(ActivityResultLauncher<String[]> launcher, String type) {
        if (launcher == null || TYPE_EXACT_ALARM.equals(type)) return;

        String[] permissions = getPermissionsForType(type);
        if (permissions.length > 0) {
            launcher.launch(permissions);
        }
    }

    public static boolean shouldOpenSettings(Activity activity, String type, boolean wasAskedBefore) {
        if (TYPE_EXACT_ALARM.equals(type)) {
            return !hasExactAlarmPermission(activity);
        }
        if (!wasAskedBefore) return false;

        String[] permissions = getPermissionsForType(type);
        if (permissions.length == 0) return false;

        for (String permission : permissions) {
            boolean granted = ContextCompat.checkSelfPermission(activity, permission)
                    == PackageManager.PERMISSION_GRANTED;

            if (!granted && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    public static void openExactAlarmSettings(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        startSettingsActivity(context, intent);
    }

    public static void openNotificationSettings(Context context) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            intent = appDetailsIntent(context);
        }
        startSettingsActivity(context, intent);
    }

    public static void openAppDetailsSettings(Context context) {
        startSettingsActivity(context, appDetailsIntent(context));
    }

    private static Intent appDetailsIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        return intent;
    }

    private static void startSettingsActivity(Context context, Intent intent) {
        try {
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (Exception ignored) {
            Intent fallback = appDetailsIntent(context);
            if (!(context instanceof Activity)) {
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(fallback);
        }
    }
}
