package com.example.accidentreportandmonitoringsystem.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RotatingGradientDrawable extends Drawable {
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float rotationAngle = 0f;
    private final RectF borderRect = new RectF();
    private final RectF glowRect = new RectF();
    private final RectF fillRect = new RectF();

    public RotatingGradientDrawable() {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(10f);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(14f);
        glowPaint.setShadowLayer(28f, 0f, 0f, Color.parseColor("#2563A6"));

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.parseColor("#FFFFFF"));
    }

    public void setRotationAngle(float angle) {
        this.rotationAngle = angle;
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(@NonNull android.graphics.Rect bounds) {
        super.onBoundsChange(bounds);
        fillRect.set(bounds.left + 6f, bounds.top + 6f, bounds.right - 6f, bounds.bottom - 6f);
        glowRect.set(bounds.left + 10f, bounds.top + 10f, bounds.right - 10f, bounds.bottom - 10f);
        borderRect.set(bounds.left + 10f, bounds.top + 10f, bounds.right - 10f, bounds.bottom - 10f);

        int cx = bounds.centerX();
        int cy = bounds.centerY();
        Shader shader = new SweepGradient(cx, cy, new int[]{
            Color.parseColor("#0B172A"),
            Color.parseColor("#142B4A"),
            Color.parseColor("#2563A6"),
            Color.parseColor("#142B4A"),
            Color.parseColor("#0B172A")
        }, new float[]{0.0f, 0.25f, 0.5f, 0.75f, 1.0f});
        borderPaint.setShader(shader);
        glowPaint.setShader(shader);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        // 1. Draw solid clean center card background
        canvas.drawRoundRect(fillRect, 16f, 16f, fillPaint);

        // 2. Draw rotating vibrant glowing shadow/halo layer
        canvas.save();
        canvas.rotate(rotationAngle, getBounds().centerX(), getBounds().centerY());
        canvas.drawRoundRect(glowRect, 14f, 14f, glowPaint);

        // 3. Draw rotating conic gradient border stroke on top
        canvas.drawRoundRect(borderRect, 14f, 14f, borderPaint);
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        borderPaint.setAlpha(alpha);
        glowPaint.setAlpha(alpha);
        fillPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        borderPaint.setColorFilter(colorFilter);
        glowPaint.setColorFilter(colorFilter);
        fillPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
