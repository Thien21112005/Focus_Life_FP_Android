package com.hcmute.edu.vn.focus_life.ui.running;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.hcmute.edu.vn.focus_life.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RoutePreviewView extends View {

    private final List<Location> routePoints = new ArrayList<>();

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint startPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint endPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RoutePreviewView(Context context) {
        super(context);
        init();
    }
    public RoutePreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RoutePreviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.white_20));
        gridPaint.setStrokeWidth(dp(1));

        routePaint.setColor(ContextCompat.getColor(getContext(), R.color.primary));
        routePaint.setStrokeWidth(dp(5));
        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setStrokeJoin(Paint.Join.ROUND);

        startPaint.setColor(ContextCompat.getColor(getContext(), R.color.secondary));
        startPaint.setStyle(Paint.Style.FILL);

        endPaint.setColor(ContextCompat.getColor(getContext(), R.color.tertiary));
        endPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.white_70));
        textPaint.setTextSize(dp(14));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setRoutePoints(@Nullable List<Location> points) {
        routePoints.clear();
        if (points != null) {
            for (Location location : points) {
                routePoints.add(new Location(location));
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(ContextCompat.getColor(getContext(), R.color.primary_container));
        drawGrid(canvas);

        if (routePoints.size() < 2) {
            canvas.drawText(
                    "Route preview sẽ hiện khi GPS bắt đầu ghi dữ liệu",
                    getWidth() / 2f,
                    getHeight() / 2f,
                    textPaint
            );
            return;
        }

        float padding = dp(28);
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = -Double.MAX_VALUE;

        for (Location point : routePoints) {
            minLat = Math.min(minLat, point.getLatitude());
            maxLat = Math.max(maxLat, point.getLatitude());
            minLng = Math.min(minLng, point.getLongitude());
            maxLng = Math.max(maxLng, point.getLongitude());
        }

        double latSpan = Math.max(maxLat - minLat, 0.00001d);
        double lngSpan = Math.max(maxLng - minLng, 0.00001d);

        Path path = new Path();
        for (int i = 0; i < routePoints.size(); i++) {
            Location point = routePoints.get(i);
            float x = (float) ((point.getLongitude() - minLng) / lngSpan);
            float y = (float) ((point.getLatitude() - minLat) / latSpan);

            x = padding + (x * (getWidth() - 2f * padding));
            y = getHeight() - padding - (y * (getHeight() - 2f * padding));

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        canvas.drawPath(path, routePaint);

        Location start = routePoints.get(0);
        Location end = routePoints.get(routePoints.size() - 1);

        float startX = normalizeX(start, minLng, lngSpan, padding);
        float startY = normalizeY(start, minLat, latSpan, padding);
        float endX = normalizeX(end, minLng, lngSpan, padding);
        float endY = normalizeY(end, minLat, latSpan, padding);

        canvas.drawCircle(startX, startY, dp(6), startPaint);
        canvas.drawCircle(endX, endY, dp(8), endPaint);

        canvas.drawText(
                String.format(Locale.getDefault(), "%d điểm GPS", routePoints.size()),
                getWidth() / 2f,
                dp(26),
                textPaint
        );
    }

    private void drawGrid(Canvas canvas) {
        int columns = 6;
        int rows = 10;

        for (int i = 1; i < columns; i++) {
            float x = (getWidth() * i) / (float) columns;
            canvas.drawLine(x, 0, x, getHeight(), gridPaint);
        }

        for (int i = 1; i < rows; i++) {
            float y = (getHeight() * i) / (float) rows;
            canvas.drawLine(0, y, getWidth(), y, gridPaint);
        }
    }

    private float normalizeX(Location location, double minLng, double lngSpan, float padding) {
        float x = (float) ((location.getLongitude() - minLng) / lngSpan);
        return padding + (x * (getWidth() - 2f * padding));
    }

    private float normalizeY(Location location, double minLat, double latSpan, float padding) {
        float y = (float) ((location.getLatitude() - minLat) / latSpan);
        return getHeight() - padding - (y * (getHeight() - 2f * padding));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}