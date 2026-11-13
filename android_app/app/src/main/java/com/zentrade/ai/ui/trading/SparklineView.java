package com.zentrade.ai.ui.trading;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/** Grafik mini (sparkline) floating profit, dengan titik akhir berdenyut. */
public class SparklineView extends View {

    // Titik contoh dari desain (viewBox 144 x 36, y makin kecil = makin tinggi).
    private float[] ys = {28, 25, 29, 18, 21, 12, 15, 6, 3};
    private static final float BOX_H = 36f;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path line = new Path();
    private final Path area = new Path();
    private ValueAnimator pulse;
    private float pulseAlpha = 1f;

    public SparklineView(Context c) { super(c); init(); }
    public SparklineView(Context c, AttributeSet a) { super(c, a); init(); }
    public SparklineView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        float d = getResources().getDisplayMetrics().density;
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f * d);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(Color.parseColor("#58E7AA"));
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.parseColor("#58E7AA"));
        fillPaint.setStyle(Paint.Style.FILL);
        setLayerType(LAYER_TYPE_NONE, null);
    }

    /** Ganti data grafik (nilai y dalam skala 0..36, kecil = tinggi). */
    public void setPoints(float[] points) {
        if (points == null || points.length < 2) return;
        ys = points;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        fillPaint.setShader(new LinearGradient(0, 0, 0, h,
                Color.parseColor("#5958E7AA"), Color.parseColor("#0058E7AA"), Shader.TileMode.CLAMP));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        pulse = ValueAnimator.ofFloat(1f, 0.45f);
        pulse.setDuration(1000);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.addUpdateListener(a -> {
            pulseAlpha = (float) a.getAnimatedValue();
            invalidate();
        });
        pulse.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (pulse != null) pulse.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float d = getResources().getDisplayMetrics().density;
        float w = getWidth();
        float h = getHeight();
        float padR = 3f * d;
        float usableW = w - padR;
        int n = ys.length;

        line.reset();
        area.reset();
        float lastX = 0, lastY = 0;
        for (int i = 0; i < n; i++) {
            float x = usableW * i / (n - 1);
            float y = ys[i] / BOX_H * h;
            if (i == 0) {
                line.moveTo(x, y);
                area.moveTo(x, y);
            } else {
                line.lineTo(x, y);
                area.lineTo(x, y);
            }
            lastX = x;
            lastY = y;
        }
        area.lineTo(lastX, h);
        area.lineTo(0, h);
        area.close();

        c.drawPath(area, fillPaint);
        c.drawPath(line, linePaint);
        dotPaint.setAlpha((int) (255 * pulseAlpha));
        c.drawCircle(lastX, lastY, 3f * d, dotPaint);
    }
}
