/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.objects;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import fr.arnaudguyon.spacevertex.sound.Sounds;

public abstract class Ship extends SpaceObject {

    private final String TAG = super.getClass().getName();
    private static final float FIRE_SPEED = 1f;

    protected float mSpeedX, mSpeedY;
    private LineObject mLeftFlame;
    private LineObject mRightFlame;
    private LineObject mLeftCanon;
    private LineObject mRightCanon;
    private LineObject mShipLine;
    protected boolean mShoot = false;
    protected ShipType mShipType = ShipType.SOLO;
    protected ReactorPower mReactorPower = ReactorPower.OFF;

    protected Sounds mSounds;

    private static final float MAXSPEED_FAST = 0.65f;
    private static final float MAXSPEED_SLOW = 0.4f;
    private static final float MAXSPEED_BOOST = 1.5f;
    private static final float MAX_ROTATION_SPEED_SLOW = 1/1000.f;
    private static final float MAX_ROTATION_SPEED_FAST = 1/100.f;
    private static final float THRUST_DELAY_LONG = 250;
    private static final float THRUST_DELAY_SHORT = 80;
    private static final float SMOOTH_SPEED = 0.97f;
    private static final float INSTANT_SPEED = 0.5f;

    public enum ShipType {
        SOLO(true, true, Colors.SHIP_CAT, MAXSPEED_FAST, MAX_ROTATION_SPEED_FAST, THRUST_DELAY_SHORT, INSTANT_SPEED),
        CAT(true, false, Colors.SHIP_CAT, MAXSPEED_FAST, MAX_ROTATION_SPEED_SLOW, THRUST_DELAY_LONG, SMOOTH_SPEED),
        MOUSE(false, true, Colors.SHIP_MOUSE, MAXSPEED_SLOW, MAX_ROTATION_SPEED_FAST, THRUST_DELAY_SHORT, INSTANT_SPEED),
        CAT_NEUTRAL(false, false, Colors.SHIP_NEUTRAL, MAXSPEED_FAST, MAX_ROTATION_SPEED_SLOW, THRUST_DELAY_LONG, SMOOTH_SPEED),
        MOUSE_NEUTRAL(false, false, Colors.SHIP_NEUTRAL, MAXSPEED_SLOW, MAX_ROTATION_SPEED_FAST, THRUST_DELAY_SHORT, INSTANT_SPEED);

        private final boolean mCanFire;
        private final boolean mHasBoost;
        private final int mShipColor;
        private final float mMaxSpeed;
        private final float mMaxRotationSpeed;
        private final float mThrustDelay;
        private final float mSmoothSpeed;

        ShipType(boolean canFire, boolean hasBoost, int shipColor, float maxSpeed, float maxRotationSpeed, float thrustDelay, float smoothSpeed) {
            mCanFire = canFire;
            mHasBoost = hasBoost;
            mShipColor = shipColor;
            mMaxSpeed = maxSpeed;
            mMaxRotationSpeed = maxRotationSpeed;
            mThrustDelay = thrustDelay;
            mSmoothSpeed = smoothSpeed;
        }

        protected boolean canFire() {
            return mCanFire;
        }
        protected boolean hasBoost() {
            return mHasBoost;
        }
        public int getShipColor() {
            return mShipColor;
        }
        protected float getMaxSpeed() {
            return mMaxSpeed;
        }
        protected float getMaxSpeedBoost() {
            return MAXSPEED_BOOST;
        }
        protected float getMaxRotationSpeed() {
            return mMaxRotationSpeed;
        }
        protected float getThrustDelay() {
            return mThrustDelay;
        }
        protected float getSmoothSpeedFactor() { return mSmoothSpeed; }
    }

    public enum ReactorPower {
        OFF(0),
        ON(1),
        BOOST(2);
        public final int mValue;
        ReactorPower(int value) {
            mValue = value;
        }
        static ReactorPower find(int value) {
            for(ReactorPower power : ReactorPower.values()) {
                if (power.mValue == value) {
                    return power;
                }
            }
            return OFF;
        }
    }

    protected static final float[] POINTS = {
            // Ship 0-2
            -1.5f,2, 2.5f,0, -1.5f,-2,
            // Cockpit 3-5
            -0.5f,0.75f, 0,-1, -0.5f,-0.75f,// middle point unused
            // Left Reactor 6-9
            -1.5f,1.5f, -1.5f,0.5f, -1.7f,0.5f, -1.7f,1.5f,
            // Right Reactor 10-13
            -1.5f,-0.5f, -1.5f,-1.5f, -1.7f,-1.5f, -1.7f,-0.5f,
            // Left/Right Flames 14-15
            -3.5f,1, -3.5f,-1,
            // Left Canon // 16-19
            0.5f,1, 1,1, 1,1.25f, 0,1.25f,
            // Right Canon // 20-23
            0.5f,-1, 1,-1, 1,-1.25f, 0,-1.25f
    };

    private static final float SIZE = 15f;

    private static final int[] SHIP_LINES = {0, 1, 2, 0};
    private static final int[] COCKPIT_LINES = {3, 5}; //{3, 4, 5, 3};
    private static final int[] LEFT_REACTOR_LINES = {7, 8, 9, 6};
    private static final int[] RIGHT_REACTOR_LINES = {11, 12, 13 , 10};
    protected static final int[] LEFT_FLAME_LINES = {8, 14, 9};
    protected static final int[] RIGHT_FLAME_LINES = {12, 15, 13};
    private static final int[] LEFT_CANON = {16, 17, 18, 19};
    private static final int[] RIGHT_CANON = {20, 21, 22, 23};

    public Ship(Context context) {
        mSounds = Sounds.getInstance(context);
    }

    @Override
    protected float[] getPointDefinitions() {
        return POINTS;
    }

    @Override
    protected float getOriginalSize() {
        return SIZE;
    }

    @Override
    protected void buildStructure(Scene scene) {
        mShipLine = addLine(Colors.SHIP_CAT, SHIP_LINES);
        addLine(Colors.SHIP_COCKPIT, COCKPIT_LINES);
        addLine(Colors.SHIP_REACTOR, LEFT_REACTOR_LINES);
        addLine(Colors.SHIP_REACTOR, RIGHT_REACTOR_LINES);
        mLeftFlame = addLine(Colors.SHIP_FLAME, LEFT_FLAME_LINES);
        mRightFlame = addLine(Colors.SHIP_FLAME, RIGHT_FLAME_LINES);
        mLeftCanon = addLine(Colors.SHIP_CAT, LEFT_CANON);
        mRightCanon = addLine(Colors.SHIP_CAT, RIGHT_CANON);
        Arc arc = new Arc(-0.5f, 0, 0.75f, -90, 180, Colors.SHIP_COCKPIT);
        addArc(arc);
    }

    protected void setSpeed(float speedX, float speedY) {  // not framerate dependant
        mSpeedX = speedX;
        mSpeedY = speedY;
    }

    public float getSpeedX() {
        return mSpeedX;
    }

    public float getSpeedY() {
        return mSpeedY;
    }

    public boolean hasBoost() {
        return mShipType.hasBoost();
    }

    protected void getFirePos(int index, float[] pos) {
        getFirePos(index, getPosX(), getPosY(), getRotation(), pos);
    }
    protected void getFirePos(int index, float shipX, float shipY, float shipRot, float[] pos) {
        float[] points = getPointDefinitions();
        float posX = points[index*2] * getOriginalSize();
        float posY = points[index*2 + 1] * getOriginalSize();

        final float cos = (float) Math.cos(shipRot);
        final float sin = (float) Math.sin(shipRot);
        // x2= x*cos(a) + y*sin(a);
        pos[0] = shipX + (posX*cos + posY*sin);
        // y2 = y*cos(a) - x*sin(a);
        pos[1] = shipY + (posY*cos - posX*sin);
    }

    protected void getFireSpeed(float[] speed) {
        float rotation = getRotation();
        getFireSpeed(rotation, speed);
    }
    protected void getFireSpeed(float rotation, float[] speed) {
        final float cos = (float) Math.cos(-rotation);
        final float sin = (float) Math.sin(-rotation);
        speed[0] = cos* FIRE_SPEED;
        speed[1] = sin* FIRE_SPEED;
    }

    protected void handleSpeed(float frameDuration) {
        if ((mSpeedX == 0) && (mSpeedY == 0)) {
            return;
        }
        // Move
        float x = getPosX() + (mSpeedX * frameDuration);
        float y = getPosY() + (mSpeedY * frameDuration);
        setPos(x, y);
    }

    public void setNeutralShipType() {
        if (mShipType == ShipType.CAT) {
            setShipType(ShipType.CAT_NEUTRAL);
        } else if (mShipType == ShipType.MOUSE) {
            setShipType(ShipType.MOUSE_NEUTRAL);
        }
    }

    public void setShipType(ShipType type) {
        mShipType = type;
        setCanon(type.canFire());
        mShipLine.setColor(type.getShipColor());
    }
    public ShipType getShipType() {
        return mShipType;
    }

    private void setCanon(boolean canonOn) {
        mLeftCanon.setVisible(canonOn);
        mRightCanon.setVisible(canonOn);
    }

    protected void handleBreak(Scene scene, float frameDuration) {
//        if (scene.isGameOver()) {
//            return; // Don't break in Game Over
//        }

        float norm = (float) Math.sqrt(mSpeedX*mSpeedX + mSpeedY*mSpeedY);
        if (norm > 0) {
            float breakX = -mSpeedX / norm * frameDuration * 0.0004f;
            float breakY = -mSpeedY / norm * frameDuration * 0.0004f;
            float speedX = mSpeedX + breakX;
            float speedY = mSpeedY + breakY;
            if ((speedX * mSpeedX < 0) || (speedY * mSpeedY < 0)) { // broke to fast, negative way -> stop
                setSpeed(0, 0);
            } else {
                setSpeed(speedX, speedY);
            }
        }
    }

    protected void handlesFlamesAnimation() {
        if ((mLeftFlame != null) && mLeftFlame.isVisible()) {

            float length = (mReactorPower == ReactorPower.BOOST) ? -6f : -3f;   // Boost vs ON
            float powa = (float) (length - (Math.random() * 1.0f));  // ]-length ; -length+1]

            int leftIndex = LEFT_FLAME_LINES[1]*2;
            POINTS[leftIndex + 0] = powa;
            updatePrepare(leftIndex);

            int rightIndex = RIGHT_FLAME_LINES[1]*2;
            POINTS[rightIndex + 0] = powa;
            updatePrepare(rightIndex);
        }
    }

    public void setReactorPower(ReactorPower power, ShipLocal shipLocal, ShipRemote shipRemote) {
        //Log.d(TAG, "SetReactorPower " + power);
        if (power != mReactorPower) {
            mReactorPower = power;
            boolean visible = (power != ReactorPower.OFF);
            mLeftFlame.setVisible(visible);
            mRightFlame.setVisible(visible);
            if (power == ReactorPower.BOOST) {
                if (shipLocal == this) {
                    mSounds.playTurbo();
                } else {
                    mSounds.playRemoteTurbo(shipLocal, shipRemote);
                }
            }
        }
    }

    protected boolean drawInRadar(Canvas canvas, float spaceCenterX, float spaceCenterY) {
        float screenWidth = getScreenWidth();
        float screenHeight = getScreenHeight();
        final float screenScale = getScreenScale();
        float screenPosX = (getPosX() - spaceCenterX)*screenScale + (screenWidth/2);
        float screenPosY = (getPosY() - spaceCenterY)*screenScale + (screenHeight/2);

        // Don't simply Clamp position, but keep the correct direction
        float vDirX = (screenPosX - (screenWidth/2));
        float vDirY = (screenPosY - (screenHeight/2));
        float vNorm = (float) Math.sqrt(vDirX * vDirX + vDirY * vDirY);
        if (vNorm != 0) {
            vDirX /= vNorm;
            vDirY /= vNorm;
        }

        float size = screenHeight / 30.f;
        boolean drawRadar = false;
        if (screenPosX < 0) {
            float diffX = -screenPosX;
            float diffY = diffX * (vDirY/vDirX);
            screenPosX += diffX;
            screenPosY += diffY;
            drawRadar = true;
        } else if (screenPosX > screenWidth) {
            float diffX = screenPosX - screenWidth;
            float diffY = diffX * (vDirY/vDirX);
            screenPosX -= diffX;
            screenPosY -= diffY;
            drawRadar = true;
        }
        if (screenPosY < 0) {
            float diffY = -screenPosY;
            float diffX = diffY * (vDirX/vDirY);
            screenPosY += diffY;
            screenPosX += diffX;
            drawRadar = true;
        } else if (screenPosY > screenHeight) {
            float diffY = screenPosY - screenHeight;
            float diffX = diffY * (vDirX/vDirY);
            screenPosX -= diffX;
            screenPosY -= diffY;
            drawRadar = true;
        }

        if (drawRadar) {
            Paint paint = getPaint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(mShipType.getShipColor());
            canvas.drawCircle(screenPosX, screenPosY, size, paint);
        }
        return drawRadar;
    }

    protected void handleShoot(Scene scene, boolean canKill) {
        if (mShoot) {
            FireBall ball = new FireBall(getOriginalSize());
            //ball.prepare((int) getScreenWidth(), (int) getScreenHeight());
            float[] firePos = new float[2];
            getFirePos(18, firePos);
            ball.setPos(firePos[0], firePos[1]);
            float[] fireSpeed = new float[2];
            getFireSpeed(fireSpeed);
            ball.setSpeed(fireSpeed[0], fireSpeed[1]);
            ball.setCanKill(canKill);
            scene.addObject(ball);

            FireBall ball2 = new FireBall(getOriginalSize());
            //ball2.prepare((int) getScreenWidth(), (int) getScreenHeight());
            getFirePos(22, firePos);
            ball2.setPos(firePos[0], firePos[1]);
            ball2.setSpeed(fireSpeed[0], fireSpeed[1]);
            ball2.setCanKill(canKill);
            scene.addObject(ball2);
        }
    }
}
