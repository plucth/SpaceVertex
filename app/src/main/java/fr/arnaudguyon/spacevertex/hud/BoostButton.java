/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.hud;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import fr.arnaudguyon.spacevertex.objects.Colors;

public class BoostButton extends AppCompatImageView {

    private static final long RELOAD_DURATION = 15000;
    private static final int NB_BOOST = 3;

    private final @NonNull Paint mPaint = new Paint();
    private final @NonNull RectF mRect = new RectF();
    private BootListener mListener;
    private long mReloadDate;
    private int mColor = Colors.SHIP_MOUSE;

    public BoostButton(Context context) {
        super(context);
        initTouch();
    }

    public BoostButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initTouch();
    }

    public BoostButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initTouch();
    }

    private void initTouch() {

        mPaint.setColor(Colors.HUD_ELEMENTS);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(3);
        mPaint.setAntiAlias(true);

        setOnClickListener(v -> {
            if ((mReloadDate == 0) && (mListener != null)) {
                if (mListener.onBoost()) {
                    mReloadDate = SystemClock.uptimeMillis();
                    invalidate();
                }
            }
        });
    }

    public void setListener(BootListener listener) {
        mListener = listener;
    }

    public void reinit(int color) {
        mColor = color;
        mReloadDate = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = /*canvas.*/getWidth();
        float height = /*canvas.*/getHeight();
        float radius = 0.40f;

        float x = width / 2;
        float y = height / 2;

        long now = SystemClock.uptimeMillis();
        long elapsed = (now - mReloadDate);
        if (elapsed >= RELOAD_DURATION) {
            mPaint.setColor(mColor);
            canvas.drawCircle(x, y, radius * width, mPaint);
            mReloadDate = 0;
        } else {
            int transpColor = (mColor & 0x00FFFFFF) | 0x80000000;
            mPaint.setColor(transpColor);
            float r = radius * width;
            float elapsedPercent = elapsed / (float)RELOAD_DURATION;
            float reloadAngle = 360 * elapsedPercent;
            mRect.set(x - r, y - r, x + r, y + r);
            canvas.drawArc(mRect, -90, reloadAngle, false, mPaint);
            invalidate();
        }

        float spaceX = width/6;
        float sizeY = height/4;

        x = width/2 - (spaceX/2);
        if (NB_BOOST == 3) {
            x -= spaceX;
        } else if (NB_BOOST == 2) {
            x -= spaceX / 2;
        }
        y = height/2;
        for(int i=0; i< NB_BOOST; ++i) {
            canvas.drawLine(x, y - sizeY / 2, x + spaceX, y, mPaint);
            canvas.drawLine(x, y + sizeY / 2, x + spaceX, y, mPaint);
            x += spaceX;
        }
    }

    public interface BootListener {
        boolean onBoost();
    }
}
