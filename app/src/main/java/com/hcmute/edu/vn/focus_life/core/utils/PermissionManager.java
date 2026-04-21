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
        // ACTIVITY_RECOGNITION chỉ là runtime permission từ Android 10 (API 29)
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

    public static void requestPermissionType(ActivityResultLauncher<String[]> launcher, String type) {
        if (launcher == null) return;

        String[] permissions = getPermissionsForType(type);
        if (permissions.length > 0) {
            launcher.launch(permissions);
        }
    }

    public static boolean shouldShowPermissionRationale(Activity activity, String type) {
        String[] permissions = getPermissionsForType(type);
        if (permissions.length == 0) return false;

        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
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

    public static String[] getOnboardingPermissions() {
        List<String> permissions = new ArrayList<>();

        for (String permission : getHealthPermissions()) {
            permissions.add(permission);
        }
        for (String permission : getLocationPermissions()) {
            permissions.add(permission);
        }
        for (String permission : getNotificationPermissions()) {
            permissions.add(permission);
        }

        return permissions.toArray(new String[0]);
    }

    public static boolean hasAllOnboardingPermissions(Context context) {
        return hasPermissions(context, getOnboardingPermissions());
    }
}