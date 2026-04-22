package com.hcmute.edu.vn.focus_life.core.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {

    public static final String TYPE_HEALTH = "health";
    public static final String TYPE_LOCATION = "location";
    public static final String TYPE_NOTIFICATION = "notification";

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

    public static boolean hasPermissionType(Context context, String type) {
        return hasPermissions(context, getPermissionsForType(type));
    }

    public static boolean hasAllOnboardingPermissions(Context context) {
        return hasPermissions(context, getOnboardingPermissions());
    }

    public static void requestOnboardingPermissions(ActivityResultLauncher<String[]> launcher) {
        if (launcher != null) {
            launcher.launch(getOnboardingPermissions());
        }
    }

    public static void requestPermissionType(ActivityResultLauncher<String[]> launcher, String type) {
        if (launcher == null) return;

        String[] permissions = getPermissionsForType(type);
        if (permissions.length > 0) {
            launcher.launch(permissions);
        }
    }

    public static boolean shouldOpenSettings(Activity activity, String type, boolean wasAskedBefore) {
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
}
