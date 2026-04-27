package com.hcmute.edu.vn.focus_life.data.remote.cloudinary;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.hcmute.edu.vn.focus_life.BuildConfig;
import com.hcmute.edu.vn.focus_life.core.exception.UserFacingException;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class CloudinaryImageUploader {

    public static class UploadResult {
        public final String secureUrl;
        public final String publicId;

        public UploadResult(@NonNull String secureUrl, @NonNull String publicId) {
            this.secureUrl = secureUrl;
            this.publicId = publicId;
        }
    }

    public boolean isConfigured() {
        return !TextUtils.isEmpty(BuildConfig.CLOUDINARY_CLOUD_NAME)
                && !TextUtils.isEmpty(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
                && !"YOUR_UNSIGNED_UPLOAD_PRESET".equals(BuildConfig.CLOUDINARY_UPLOAD_PRESET);
    }

    @NonNull
    public UploadResult uploadNutritionImage(@NonNull Context context,
                                             @NonNull Uri uri,
                                             @NonNull String uid,
                                             @NonNull String dateKey,
                                             @NonNull String entryUuid) throws UserFacingException {
        if (!isConfigured()) {
            throw new UserFacingException("Cloudinary chưa được cấu hình upload_preset hợp lệ cho Android.");
        }

        String boundary = "----FocusLifeBoundary" + System.currentTimeMillis();
        String folder = buildFolder(uid, dateKey);
        String publicId = sanitize(entryUuid);
        HttpURLConnection connection = null;

        try {
            URL url = new URL(String.format(Locale.US,
                    "https://api.cloudinary.com/v1_1/%s/image/upload",
                    BuildConfig.CLOUDINARY_CLOUD_NAME));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream())) {
                writeTextPart(outputStream, boundary, "upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET);
                writeTextPart(outputStream, boundary, "folder", folder);
                writeTextPart(outputStream, boundary, "public_id", publicId);
                writeTextPart(outputStream, boundary, "tags", "focuslife,nutrition,mlkit");
                writeTextPart(outputStream, boundary, "context", "app=FocusLife|module=nutrition_diary|uid=" + sanitize(uid));
                writeFilePart(context, outputStream, boundary, uri);
                outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }

            int responseCode = connection.getResponseCode();
            String responseBody = readFully(responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream());

            if (responseCode < 200 || responseCode >= 300) {
                String errorMessage = extractCloudinaryError(responseBody);
                throw new UserFacingException("Upload ảnh Cloudinary thất bại: " + errorMessage);
            }

            JSONObject jsonObject = new JSONObject(responseBody);
            String secureUrl = jsonObject.optString("secure_url", "");
            String uploadedPublicId = jsonObject.optString("public_id", publicId);
            if (TextUtils.isEmpty(secureUrl)) {
                throw new UserFacingException("Cloudinary không trả về secure_url sau khi upload ảnh.");
            }
            return new UploadResult(secureUrl, uploadedPublicId);
        } catch (UserFacingException e) {
            throw e;
        } catch (Exception e) {
            throw new UserFacingException("Không thể upload ảnh món ăn lên Cloudinary.", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NonNull
    public UploadResult uploadAvatarImage(@NonNull Context context,
                                          @NonNull Uri uri,
                                          @NonNull String uid) throws UserFacingException {
        if (!isConfigured()) {
            throw new UserFacingException("Cloudinary chưa được cấu hình upload_preset hợp lệ cho Android.");
        }

        String boundary = "----FocusLifeBoundary" + System.currentTimeMillis();
        String folder = "focuslife/avatars/" + sanitize(uid);
        String publicId = "avatar_" + System.currentTimeMillis();
        HttpURLConnection connection = null;

        try {
            URL url = new URL(String.format(Locale.US,
                    "https://api.cloudinary.com/v1_1/%s/image/upload",
                    BuildConfig.CLOUDINARY_CLOUD_NAME));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream())) {
                writeTextPart(outputStream, boundary, "upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET);
                writeTextPart(outputStream, boundary, "folder", folder);
                writeTextPart(outputStream, boundary, "public_id", publicId);
                writeTextPart(outputStream, boundary, "tags", "focuslife,profile,avatar");
                writeTextPart(outputStream, boundary, "context", "app=FocusLife|module=profile_avatar|uid=" + sanitize(uid));
                writeFilePart(context, outputStream, boundary, uri);
                outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }

            int responseCode = connection.getResponseCode();
            String responseBody = readFully(responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream());

            if (responseCode < 200 || responseCode >= 300) {
                String errorMessage = extractCloudinaryError(responseBody);
                throw new UserFacingException("Upload ảnh đại diện thất bại: " + errorMessage);
            }

            JSONObject jsonObject = new JSONObject(responseBody);
            String secureUrl = jsonObject.optString("secure_url", "");
            String uploadedPublicId = jsonObject.optString("public_id", publicId);
            if (TextUtils.isEmpty(secureUrl)) {
                throw new UserFacingException("Cloudinary không trả về secure_url sau khi upload ảnh đại diện.");
            }
            return new UploadResult(secureUrl, uploadedPublicId);
        } catch (UserFacingException e) {
            throw e;
        } catch (Exception e) {
            throw new UserFacingException("Không thể upload ảnh đại diện lên Cloudinary.", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void writeTextPart(@NonNull OutputStream outputStream,
                               @NonNull String boundary,
                               @NonNull String name,
                               @NonNull String value) throws IOException {
        outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(value.getBytes(StandardCharsets.UTF_8));
        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeFilePart(@NonNull Context context,
                               @NonNull OutputStream outputStream,
                               @NonNull String boundary,
                               @NonNull Uri uri) throws IOException, UserFacingException {
        String mimeType = context.getContentResolver().getType(uri);
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "image/jpeg";
        }
        String fileName = "nutrition_scan.jpg";
        String lastSegment = uri.getLastPathSegment();
        if (!TextUtils.isEmpty(lastSegment)) {
            fileName = lastSegment;
        }

        outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));

        try (InputStream inputStream = new BufferedInputStream(context.getContentResolver().openInputStream(uri))) {
            if (inputStream == null) {
                throw new UserFacingException("Không thể đọc file ảnh để upload Cloudinary.");
            }
            byte[] buffer = new byte[8192];
            int count;
            while ((count = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
            }
        }
        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    @NonNull
    private String readFully(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        try (InputStream in = inputStream; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            return bos.toString(StandardCharsets.UTF_8.name());
        }
    }

    @NonNull
    private String extractCloudinaryError(@NonNull String body) {
        try {
            JSONObject root = new JSONObject(body);
            if (root.has("error")) {
                JSONObject error = root.optJSONObject("error");
                if (error != null) {
                    String message = error.optString("message", "Cloudinary trả về lỗi không xác định.");
                    return TextUtils.isEmpty(message) ? "Cloudinary trả về lỗi không xác định." : message;
                }
            }
        } catch (Exception ignored) {
        }
        return TextUtils.isEmpty(body) ? "Cloudinary trả về lỗi không xác định." : body;
    }

    @NonNull
    private String buildFolder(@NonNull String uid, @NonNull String dateKey) {
        String baseFolder = TextUtils.isEmpty(BuildConfig.CLOUDINARY_BASE_FOLDER)
                ? "focuslife/nutrition"
                : BuildConfig.CLOUDINARY_BASE_FOLDER;
        return baseFolder + "/" + sanitize(uid) + "/" + sanitize(dateKey);
    }

    @NonNull
    private String sanitize(@NonNull String value) {
        return value.replaceAll("[^a-zA-Z0-9_\\-/]", "_");
    }
}
