package com.hcmute.edu.vn.focus_life.core.exception;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.StorageException;

public final class FirebaseExceptionMapper {

    private FirebaseExceptionMapper() {
    }

    public static String toUserMessage(Exception e) {
        if (e == null) {
            return "Có lỗi xảy ra. Vui lòng thử lại.";
        }

        if (e instanceof UserFacingException) {
            return ((UserFacingException) e).getUserMessage();
        }

        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            String code = ((FirebaseAuthInvalidCredentialsException) e).getErrorCode();

            if ("ERROR_WRONG_PASSWORD".equals(code)
                    || "ERROR_INVALID_CREDENTIAL".equals(code)
                    || "ERROR_INVALID_LOGIN_CREDENTIALS".equals(code)) {
                return "Sai mật khẩu. Vui lòng kiểm tra lại.";
            }

            if ("ERROR_INVALID_EMAIL".equals(code)) {
                return "Email không đúng định dạng.";
            }

            return "Thông tin đăng nhập không hợp lệ.";
        }

        if (e instanceof FirebaseAuthInvalidUserException) {
            return "Tài khoản không tồn tại hoặc đã bị xóa.";
        }

        if (e instanceof FirebaseAuthUserCollisionException) {
            return "Email này đã được dùng cho tài khoản khác.";
        }

        if (e instanceof FirebaseTooManyRequestsException) {
            return "Bạn thao tác quá nhiều lần. Vui lòng thử lại sau ít phút.";
        }

        if (e instanceof FirebaseNetworkException) {
            return "Kết nối mạng đang không ổn định. Vui lòng thử lại.";
        }

        if (e instanceof StorageException) {
            StorageException se = (StorageException) e;
            int code = se.getErrorCode();

            if (code == StorageException.ERROR_OBJECT_NOT_FOUND) {
                return "Không tìm thấy file trên bộ nhớ cloud.";
            }
            if (code == StorageException.ERROR_BUCKET_NOT_FOUND) {
                return "Bucket Firebase Storage chưa tồn tại hoặc đang sai cấu hình.";
            }
            if (code == StorageException.ERROR_PROJECT_NOT_FOUND) {
                return "Project Firebase Storage không tồn tại.";
            }
            if (code == StorageException.ERROR_NOT_AUTHENTICATED) {
                return "Bạn cần đăng nhập lại để tiếp tục.";
            }
            if (code == StorageException.ERROR_NOT_AUTHORIZED) {
                return "Bạn chưa có quyền truy cập file này.";
            }
            if (code == StorageException.ERROR_RETRY_LIMIT_EXCEEDED) {
                return "Kết nối tới bộ nhớ cloud bị gián đoạn. Vui lòng thử lại.";
            }
            return "Không thể xử lý file lúc này.";
        }

        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
            switch (fe.getCode()) {
                case PERMISSION_DENIED:
                    return "Bạn không có quyền truy cập dữ liệu này.";
                case UNAVAILABLE:
                    return "Dữ liệu đang tạm thời không khả dụng. Vui lòng thử lại.";
                case NOT_FOUND:
                    return "Không tìm thấy dữ liệu.";
                default:
                    return "Không thể đồng bộ dữ liệu lúc này.";
            }
        }

        if (e instanceof FirebaseAuthException) {
            return "Không thể xác thực tài khoản lúc này.";
        }

        String raw = e.getMessage();
        if (raw != null) {
            if (raw.contains("No AppCheckProvider installed")) {
                return "App Check chưa được cấu hình, nhưng bạn vẫn có thể tiếp tục dùng app.";
            }
            if (raw.contains("The supplied auth credential is incorrect")
                    || raw.contains("malformed or has expired")
                    || raw.contains("INVALID_LOGIN_CREDENTIALS")) {
                return "Sai mật khẩu. Vui lòng kiểm tra lại.";
            }
            if (raw.contains("network error") || raw.contains("Network error")) {
                return "Kết nối mạng đang không ổn định. Vui lòng thử lại.";
            }
        }

        return "Có lỗi xảy ra. Vui lòng thử lại.";
    }
}
