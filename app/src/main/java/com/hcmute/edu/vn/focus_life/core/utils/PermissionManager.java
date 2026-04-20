package com.hcmute.edu.vn.focus_life.core.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {

    public static String[] getOnboardingPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACTIVITY_RECOGNITION);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return permissions.toArray(new String[0]);
    }

    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasAllOnboardingPermissions(Context context) {
        for (String permission : getOnboardingPermissions()) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    public static void requestOnboardingPermissions(ActivityResultLauncher<String[]> launcher) {
        launcher.launch(getOnboardingPermissions());
    }
}
