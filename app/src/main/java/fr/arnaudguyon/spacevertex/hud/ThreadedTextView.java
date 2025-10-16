/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.hud;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * TextView that can be modified from outside the Main Thread
 */
public class ThreadedTextView extends AppCompatTextView {

    private boolean mTextChanged = false;
    private boolean mColorChanged = false;
    private String mText;
    private int mColor;

    public ThreadedTextView(Context context) {
        super(context);
    }

    public ThreadedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThreadedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setText(String text) {
        synchronized (this) {
            mText = text;
            mTextChanged = true;
        }
        postInvalidate();
    }
    public void setTextColor(int color) {
        synchronized (this) {
            mColor = color;
            mColorChanged = true;
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mTextChanged) {
            super.setText(mText);
            mTextChanged = false;
        }   // else keep Manifest / previous value
        if (mColorChanged) {
            super.setTextColor(mColor);
            mColorChanged = false;
        }   // else keep Manifest / previous value
        super.onDraw(canvas);
    }
}
