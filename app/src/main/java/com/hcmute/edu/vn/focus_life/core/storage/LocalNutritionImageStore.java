package com.hcmute.edu.vn.focus_life.core.storage;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hcmute.edu.vn.focus_life.core.exception.UserFacingException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LocalNutritionImageStore {

    private static final String DIR_NAME = "nutrition_scans";

    @Nullable
    public String persistIfNeeded(@NonNull Context context,
                                  @Nullable String imageUriString,
                                  @NonNull String entryUuid) throws UserFacingException {
        if (TextUtils.isEmpty(imageUriString)) {
            return imageUriString;
        }

        Uri sourceUri = Uri.parse(imageUriString);
        if (sourceUri == null) {
            return imageUriString;
        }

        if (isAlreadyInAppStorage(context, sourceUri)) {
            return sourceUri.toString();
        }

        File dir = new File(context.getFilesDir(), DIR_NAME);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new UserFacingException("Không thể tạo vùng lưu ảnh tạm cho nhật ký dinh dưỡng.");
        }

        String extension = guessExtension(context, sourceUri);
        File outFile = new File(dir, entryUuid + "_" + System.currentTimeMillis() + extension);

        try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
             OutputStream outputStream = new FileOutputStream(outFile)) {
            if (inputStream == null) {
                throw new UserFacingException("Không đọc được ảnh bạn đã chọn.");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            return Uri.fromFile(outFile).toString();
        } catch (IOException e) {
            throw new UserFacingException("Không thể sao chép ảnh scan vào bộ nhớ ứng dụng.", e);
        }
    }

    private boolean isAlreadyInAppStorage(@NonNull Context context, @NonNull Uri uri) {
        if (!"file".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        String path = uri.getPath();
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        return path.startsWith(context.getFilesDir().getAbsolutePath());
    }

    @NonNull
    private String guessExtension(@NonNull Context context, @NonNull Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        if (!TextUtils.isEmpty(mimeType)) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (!TextUtils.isEmpty(ext)) {
                return "." + ext;
            }
        }
        String path = uri.getPath();
        if (!TextUtils.isEmpty(path)) {
            int index = path.lastIndexOf('.');
            if (index >= 0 && index < path.length() - 1) {
                String ext = path.substring(index);
                if (ext.length() <= 6) {
                    return ext;
                }
            }
        }
        return ".jpg";
    }
}
