/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.hud;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import fr.arnaudguyon.spacevertex.objects.Colors;

public class FireButton extends AppCompatImageView {

    private FireListener mListener;
    private final @NonNull Paint mPaint = new Paint();

    public FireButton(Context context) {
        super(context);
        initTouch();
    }

    public FireButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initTouch();
    }

    public FireButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initTouch();
    }

    private void initTouch() {

        mPaint.setColor(Colors.HUD_ELEMENTS);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(3);
        mPaint.setAntiAlias(true);

        setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onFirePressed();
            }
        });
    }

    public void reinit(int color) {
        mPaint.setColor(color);
        invalidate();
    }

    public void setListener(FireListener listener) {
        mListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = /*canvas.*/getWidth();
        float height = /*canvas.*/getHeight();
        float radius = 0.40f;

        float x = width / 2;
        float y = height / 2;

        canvas.drawCircle(x, y, radius * width, mPaint);

        canvas.drawCircle(x, y, radius * width * 0.25f, mPaint);

        canvas.drawLine(x, y + radius * width * 0.35f, x, y + radius * width * 0.90f, mPaint);
        canvas.drawLine(x - width * 0.05f , y + radius * width * 0.35f, x - width * 0.05f, y + radius * width * 0.90f, mPaint);
        canvas.drawLine(x + width * 0.05f , y + radius * width * 0.35f, x + width * 0.05f, y + radius * width * 0.90f, mPaint);
    }

    public interface FireListener {
        void onFirePressed();
    }
}
