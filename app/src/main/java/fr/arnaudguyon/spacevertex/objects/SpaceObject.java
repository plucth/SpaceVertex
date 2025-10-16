/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.objects;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;

import fr.arnaudguyon.spacevertex.network.GameConnection;
import fr.arnaudguyon.spacevertex.network.PackMsg;

public abstract class SpaceObject {

    private float[] mScaledPoints;
    private float[] mTransformedPoints;
    private float mPosX, mPosY, mRotation;
    private boolean mCanKill = false;
    private int mScreenWidth, mScreenHeight;
    private ArrayList<LineObject> mLines;
    private ArrayList<Circle> mCircles;
    private ArrayList<Arc> mArcs;
    private Paint mPaint;
    private boolean mToBeDestroyed;
    protected GameConnection gameConnection;
    private boolean mVisible = true;

    static class LineObject {
        int mColor;
        int[] mIndices;
        private boolean mVisible;
        LineObject(int color, int[] indices) {
            mColor = color;
            mIndices = indices;
            mVisible = true;
        }
        public void setVisible(boolean visible) {
            mVisible = visible;
        }
        public boolean isVisible() {
            return mVisible;
        }
        public void setColor(int color) {
            mColor = color;
        }
    }

    public float getStrokeWidth() {
        return Math.max(mScreenHeight / 500f, 2);
    }

    public void prepare(Scene scene, int screenWidth, int screenHeight) {

        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        float screenScale = getScreenScale();   // call after ini mScreenWidth & mScreenHeight

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(getStrokeWidth());


        // Scale original points of object
        final float[] points = getPointDefinitions();
        if (points != null) {
            final float scale = screenScale * getOriginalSize();
            final int nbPoints = points.length / 2;
            mScaledPoints = new float[nbPoints * 2];
            for (int i = 0; i < nbPoints * 2; i += 2) {
                mScaledPoints[i] = points[i] * scale;
                mScaledPoints[i + 1] = -points[i + 1] * scale;  // invert Y
            }
            mTransformedPoints = new float[nbPoints * 2];
        }

        // ask objects to add their lines/colors
        buildStructure(scene);
    }

    public float getScreenScale() {
        return Math.min(mScreenWidth, mScreenHeight) * 0.001f;
    }

    public void updatePrepare(int index) {  // when points are modified in real time
        float screenScale = getScreenScale();
        final float scale = screenScale * getOriginalSize();
        final float[] points = getPointDefinitions();
        mScaledPoints[index] = points[index] * scale;
        mScaledPoints[index+1] = -points[index+1] * scale;  // invert Y
    }

    public void setPos(float x, float y) {
        mPosX = x;
        mPosY = y;
    }
    public float getPosX() {
        return mPosX;
    }
    public float getPosY() {
        return mPosY;
    }

    public void setRotation(float rotation) {
        mRotation = (float) (rotation % (2 * Math.PI));
    }
    public float getRotation() {
        return mRotation;
    }

    public void computeTransformation(float spaceCenterX, float spaceCenterY) {
        final float screenScale = getScreenScale();
        float translationX = (mPosX - spaceCenterX)*screenScale + (mScreenWidth/2);
        float translationY = (mPosY - spaceCenterY)*screenScale + (mScreenHeight/2);

        if (mRotation == 0) {
            final int nbPoints = mScaledPoints.length / 2;
            for(int i=0; i<nbPoints*2; i+=2) {
                mTransformedPoints[i+0] = mScaledPoints[i+0] + translationX;
                mTransformedPoints[i+1] = mScaledPoints[i+1] + translationY;
            }
        } else {
            final int nbPoints = mScaledPoints.length / 2;
            final float cos = (float) Math.cos(mRotation);
            final float sin = (float) Math.sin(mRotation);
            for(int i=0; i<nbPoints*2; i+=2) {
                // x2= x*cos(a) + y*sin(a);
                mTransformedPoints[i + 0] = translationX + (mScaledPoints[i + 0]*cos + mScaledPoints[i+1]*sin);
                // y2 = y*cos(a) - x*sin(a);
                mTransformedPoints[i + 1] = translationY + (mScaledPoints[i + 1]*cos - mScaledPoints[i+0]*sin);
            }
        }
    }

    protected LineObject addLine(int color, int[] indices) {
        if (mLines == null) {
            mLines = new ArrayList<>();
        }
        LineObject line = new LineObject(color, indices);
        mLines.add(line);
        return line;
    }

    protected void addCircle(Circle circle) {
        if (mCircles == null) {
            mCircles = new ArrayList<>();
        }
        mCircles.add(circle);
    }
    protected void addArc(Arc arc) {
        if (mArcs == null) {
            mArcs = new ArrayList<>();
        }
        mArcs.add(arc);
    }

    public abstract void preDraw(Scene scene, float frameDuration);

    public void draw(Canvas canvas, float spaceCenterX, float spaceCenterY) {

        if (!mVisible) {
            return;
        }

        if (drawInRadar(canvas, spaceCenterX, spaceCenterY)) {
            return;
        }

        if (mLines != null) {
            computeTransformation(spaceCenterX, spaceCenterY);
            for (LineObject line : mLines) {
                if (!line.mVisible) {
                    continue;
                }
                mPaint.setColor(line.mColor);
                int[] indices = line.mIndices;
                for (int i = 0; i < indices.length - 1; ++i) {
                    int indiceStart = indices[i];
                    float x1 = mTransformedPoints[indiceStart * 2];
                    float y1 = mTransformedPoints[indiceStart * 2 + 1];
                    int indiceStop = indices[i + 1];
                    float x2 = mTransformedPoints[indiceStop * 2];
                    float y2 = mTransformedPoints[indiceStop * 2 + 1];
                    canvas.drawLine(x1, y1, x2, y2, mPaint);
                }
            }
        }

        //final float scale = getScreenScale();

        if (mCircles != null) {
            for(Circle circle : mCircles) {
                circle.draw(canvas, this, spaceCenterX, spaceCenterY);
            }
        }
        if (mArcs != null) {
            for(Arc arc : mArcs) {
                //arc.draw(canvas, this, (mScreenWidth/2) - spaceCenterX*scale, (mScreenHeight/2) - spaceCenterY*scale);
                arc.draw(canvas, this, spaceCenterX, spaceCenterY);
            }
        }
    }

    protected boolean drawInRadar(Canvas canvas, float spaceCenterX, float spaceCenterY) {
        final float translationX = mPosX - spaceCenterX + (mScreenWidth/2);
        final float translationY = mPosY - spaceCenterY + (mScreenHeight/2);
        // Don't draw by default, just pretend to do so
        return ((translationX < 0) || (translationX > mScreenWidth) || (translationY < 0) || (translationY > mScreenHeight));
    }

    protected void destroy() {
        mToBeDestroyed = true;
    }
    public boolean isToBeDestroyed() {
        return mToBeDestroyed;
    }

    public boolean isVisible() {
        return mVisible;
    }

    public void setVisible(boolean visible) {
        mVisible = visible;
    }

    public void setWebSockets(GameConnection gameConnection) {
        this.gameConnection = gameConnection;
    }

    protected abstract float[] getPointDefinitions();
    protected abstract float getOriginalSize();
    protected abstract void buildStructure(Scene scene);
    public PackMsg prepareNetworkMessage(Scene scene, int frameNumber) { return null; }
    public void onNetworkMessageReceived(Scene scene, PackMsg packMsg) {}
    public void setCanKill(boolean canKill) {   // can Kill Local Ship
        mCanKill = canKill;
    }
    public boolean canKill() { return mCanKill; }
    protected float getScreenWidth() {
        return mScreenWidth;
    }
    protected float getScreenHeight() {
        return mScreenHeight;
    }
    protected Paint getPaint() {
        return mPaint;
    }

}
