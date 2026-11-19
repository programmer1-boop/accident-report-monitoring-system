package com.example.accidentreportandmonitoringsystem.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RotatingGradientDrawable extends Drawable {
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix shaderMatrix = new Matrix();
    private SweepGradient sweepGradient;
    private float rotationAngle = 0f;
    private final RectF borderRect = new RectF();
    private final RectF fillRect = new RectF();

    public RotatingGradientDrawable() {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6f);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.parseColor("#FFFFFF"));
    }

    public void setRotationAngle(float angle) {
        this.rotationAngle = angle;
        if (sweepGradient != null && !getBounds().isEmpty()) {
            shaderMatrix.setRotate(rotationAngle, getBounds().centerX(), getBounds().centerY());
            sweepGradient.setLocalMatrix(shaderMatrix);
        }
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(@NonNull android.graphics.Rect bounds) {
        super.onBoundsChange(bounds);
        fillRect.set(bounds.left + 3f, bounds.top + 3f, bounds.right - 3f, bounds.bottom - 3f);
        borderRect.set(bounds.left + 3f, bounds.top + 3f, bounds.right - 3f, bounds.bottom - 3f);

        int cx = bounds.centerX();
        int cy = bounds.centerY();
        sweepGradient = new SweepGradient(cx, cy, new int[]{
            Color.parseColor("#142B4A"),
            Color.parseColor("#2563EB"),
            Color.parseColor("#D92D35"),
            Color.parseColor("#2563EB"),
            Color.parseColor("#142B4A")
        }, new float[]{0.0f, 0.25f, 0.5f, 0.75f, 1.0f});
        borderPaint.setShader(sweepGradient);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        // Draw stationary rounded white card background and animated gradient border
        canvas.drawRoundRect(fillRect, 24f, 24f, fillPaint);
        canvas.drawRoundRect(borderRect, 24f, 24f, borderPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        borderPaint.setAlpha(alpha);
        fillPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        borderPaint.setColorFilter(colorFilter);
        fillPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
