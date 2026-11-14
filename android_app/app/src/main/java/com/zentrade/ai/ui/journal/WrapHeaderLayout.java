package com.zentrade.ai.ui.journal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Baris header log: anak pertama di kiri, anak kedua di kanan (seperti "justify-between").
 * Kalau lebar tidak cukup, anak kedua turun ke baris bawah (seperti "flex-wrap"), jadi tidak ada yang terpotong atau menumpuk.
 */
public class WrapHeaderLayout extends ViewGroup {

    private final int rowGap;
    private final int minGap;
    private boolean stacked = false;

    public WrapHeaderLayout(Context c) { this(c, null); }

    public WrapHeaderLayout(Context c, AttributeSet a) {
        super(c, a);
        float d = c.getResources().getDisplayMetrics().density;
        rowGap = Math.round(4 * d);
        minGap = Math.round(4 * d);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int avail = MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight();
        int count = getChildCount();
        int[] w = new int[count];
        int[] h = new int[count];
        for (int i = 0; i < count; i++) {
            View ch = getChildAt(i);
            ch.measure(MeasureSpec.makeMeasureSpec(Math.max(avail, 0), MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            w[i] = ch.getMeasuredWidth();
            h[i] = ch.getMeasuredHeight();
        }
        int height;
        if (count >= 2 && w[0] + minGap + w[1] > avail) {
            stacked = true;
            height = h[0] + rowGap + h[1];
        } else {
            stacked = false;
            height = 0;
            for (int i = 0; i < count; i++) height = Math.max(height, h[i]);
        }
        int width = MeasureSpec.getMode(widthSpec) == MeasureSpec.EXACTLY
                ? MeasureSpec.getSize(widthSpec)
                : Math.max(w[0], count > 1 ? w[1] : 0) + getPaddingLeft() + getPaddingRight();
        setMeasuredDimension(width, height + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        if (count == 0) return;
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = r - l - getPaddingRight();
        View first = getChildAt(0);
        if (count == 1) {
            first.layout(left, top, left + first.getMeasuredWidth(), top + first.getMeasuredHeight());
            return;
        }
        View second = getChildAt(1);
        if (stacked) {
            first.layout(left, top, left + first.getMeasuredWidth(), top + first.getMeasuredHeight());
            int y = top + first.getMeasuredHeight() + rowGap;
            second.layout(left, y, left + second.getMeasuredWidth(), y + second.getMeasuredHeight());
        } else {
            int rowH = Math.max(first.getMeasuredHeight(), second.getMeasuredHeight());
            int y1 = top + (rowH - first.getMeasuredHeight()) / 2;
            int y2 = top + (rowH - second.getMeasuredHeight()) / 2;
            first.layout(left, y1, left + first.getMeasuredWidth(), y1 + first.getMeasuredHeight());
            second.layout(right - second.getMeasuredWidth(), y2, right, y2 + second.getMeasuredHeight());
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }
}
