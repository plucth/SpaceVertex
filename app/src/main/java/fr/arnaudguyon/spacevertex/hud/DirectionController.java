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
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import fr.arnaudguyon.spacevertex.objects.Colors;

// TODO: handle 1st finger only, or there can be a bug: touch first, then touch elsewhere with second (including out of the controller), remove finger1, control is taken by finger 2.
public class DirectionController extends AppCompatImageView {

    private DirectionListener mListener;
    private final @NonNull Paint mPaint = new Paint();

    public DirectionController(Context context) {
        super(context);
        initTouch();
    }

    public DirectionController(Context context, AttributeSet attrs) {
        super(context, attrs);
        initTouch();
    }

    public DirectionController(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initTouch();
    }

    public void reinit(int color) {
        mPaint.setColor(color);
        invalidate();
    }

    public void setListener(DirectionListener listener) {
        mListener = listener;
    }

    private void initTouch() {

        mPaint.setColor(Colors.HUD_ELEMENTS);
        mPaint.setStrokeWidth(3);
        mPaint.setAntiAlias(true);

        setOnTouchListener((v, event) -> {

            int action = event.getActionMasked();
            if ((action != MotionEvent.ACTION_DOWN) && (action != MotionEvent.ACTION_MOVE)) {
                if (mListener != null) {
                    mListener.setDirectionThrust(false);
                }
                return true;
            }

            //Log.i("TOUCH", "Touch");
            float dx = event.getX() - (getWidth() / 2);
            float dy = (getHeight() / 2) - event.getY();
            float norm2 = (dx * dx) + (dy * dy);
            if (norm2 < 10) {   // centered, don't do anything
                return true;
            }
            float norm = (float) Math.sqrt(norm2);
            dx /= norm;
            dy /= norm;
            float angle;
            if (dx != 0) {
                angle = (float) Math.acos(dx);
                if (dy < 0) {
                    angle *= -1;
                }
            } else {    // dx==0
                if (dy > 0) {
                    angle = (float) Math.PI / 2;
                } else {
                    angle = (float) -Math.PI / 2;
                }
            }

            //Log.i("ANGLE", "ANGLE: " + angle);
            if (mListener != null) {
                mListener.setDirection(angle);
                mListener.setDirectionThrust(true);
            }
            return true;
        });
    }

    public interface DirectionListener {
        void setDirection(float angleRadians);
        void setDirectionThrust(boolean on);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = /*canvas.*/getWidth();
        float height = /*canvas.*/getHeight();
        float radius = 0.40f;

        float previousX = width*radius + (width / 2);
        float previousY = height/2;
        float angle = 0;
        float x, y;
        for(int i=0; i<9; ++i) {
            x = (float) ((width * Math.cos(angle) * radius) + (width / 2));
            y = (float) ((height * Math.sin(angle) * radius) + (height / 2));
            canvas.drawLine(previousX, previousY, x, y, mPaint);
            previousX = x;
            previousY = y;
            angle += (float) (Math.PI * 2 / 8 * 3);
        }

    }


}
