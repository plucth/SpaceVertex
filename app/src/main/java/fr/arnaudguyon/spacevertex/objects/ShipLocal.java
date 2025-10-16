/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.objects;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import fr.arnaudguyon.spacevertex.hud.BoostButton;
import fr.arnaudguyon.spacevertex.hud.DirectionController;
import fr.arnaudguyon.spacevertex.hud.FireButton;
import fr.arnaudguyon.spacevertex.network.GameDevice;
import fr.arnaudguyon.spacevertex.network.PackMsg;

public class ShipLocal extends Ship {

    private static final long TIME_BETWEEN_BULLETS = 350;
    private static final long BOOST_DURATION = 1250;

    private float mWantedRotation;
    private long mThrustDate;
    private Explosion mExplosion;
    private long  mLastShoot;
    private FireButton mFireButton;
    private BoostButton mBoostButton;
    private DirectionController mDirectionController;
    private long mBoostEndDate;
    private GameDevice otherPlayer;

    public ShipLocal(Context context) {
        super(context);
    }

    public void setOtherPlayer(@NonNull GameDevice otherPlayer) {
        this.otherPlayer = otherPlayer;
    }

    @Override
    public void preDraw(Scene scene, float frameDuration) {

        handlesThrust(scene, frameDuration);
        if (scene.isGameOver()) {
            setReactorPower(Ship.ReactorPower.ON, this, scene.getRemoteShip());
        }
        handlesFlamesAnimation();
        handlesWantedRotation(frameDuration);
        handleSpeed(frameDuration);
        handleShoot(scene, false);
        handleDestroyed(scene);

        mShoot = false;
    }

    @Override
    public void draw(Canvas canvas, float spaceCenterX, float spaceCenterY) {
        float newX = getPosX();
        float newY = getPosY();
        super.draw(canvas, newX, newY);
    }

    public void setWantedDirection(float angleRadians) {
        mWantedRotation = angleRadians;
    }

    public void setButtons(FireButton fireButton, BoostButton boostButton, DirectionController directionController) {
        mFireButton = fireButton;
        mBoostButton = boostButton;
        mDirectionController = directionController;
    }

    @Override
    public void setShipType(ShipType type) {
        super.setShipType(type);
        if (type.canFire()) {
            mFireButton.setVisibility(View.VISIBLE);
            mBoostButton.setVisibility(View.GONE);
        } else {
            mFireButton.setVisibility(View.GONE);
            mBoostButton.setVisibility(View.VISIBLE);
        }
        final int color = type.getShipColor();
        mBoostButton.reinit(color);
        mFireButton.reinit(color);
        mDirectionController.reinit(color);
    }

    public void setThrust(Scene scene, boolean on) {
        if (on) {
            if (mThrustDate == 0) {

                mSounds.startEngine();

                // check if wanted rotation is the same direction as current rotation. yes -> thrust now, else wait for 1/2 turn
                float diffAngle = (float) ((mWantedRotation - getRotation()) % (2 * Math.PI));
                if (diffAngle > Math.PI) {
                    diffAngle -= Math.PI * 2;
                } else if (diffAngle < -Math.PI) {
                    diffAngle += Math.PI * 2;
                }
                //Log.i("ROTATION", "DIFF:" + diffAngle);
                float thrustDelay = mShipType.getThrustDelay();
                long timeToWait = (long) (diffAngle * thrustDelay);
                if (timeToWait < 0) {
                    timeToWait = -timeToWait;
                }
                mThrustDate = SystemClock.uptimeMillis() + timeToWait;
                setReactorPower(boostInProgress() ? ReactorPower.BOOST : ReactorPower.ON, this, scene.getRemoteShip());
            }
        } else {
            if (mThrustDate != 0) {
                mThrustDate = 0;
                mSounds.stopEngine();
            }
            setReactorPower(boostInProgress() ? ReactorPower.BOOST : ReactorPower.OFF, this, scene.getRemoteShip());
        }
    }

    private void handlesThrust(Scene scene, float frameDuration) {

        final boolean thrust;
        if (mThrustDate > 0) {
            thrust = SystemClock.uptimeMillis() > mThrustDate;
        } else {
            thrust = false;
        }
        final boolean boost;
        final long now = SystemClock.uptimeMillis();
        boost = (mBoostEndDate > now);

        if (thrust || boost) {
            float maxSpeed;
            if (scene.isGameOver()) {
                maxSpeed = ShipType.CAT.getMaxSpeed() / 4.f;
            } else {
                maxSpeed = boost ? mShipType.getMaxSpeedBoost() : mShipType.getMaxSpeed();
            }
            float rotation = getRotation();
            float dirX = (float) Math.cos(rotation) * maxSpeed;
            float dirY = (float) Math.sin(rotation) * maxSpeed;

            float smooth = mShipType.getSmoothSpeedFactor();

            float newDirX = mSpeedX*smooth + dirX*(1 - smooth);
            float newDirY = mSpeedY*smooth - dirY*(1 - smooth);

            setSpeed(newDirX, newDirY);
        } else {    // Break
            handleBreak(scene, frameDuration);
        }
    }

    private void handlesWantedRotation(float frameDuration) {
        // Rotates slowly
        float curRot = getRotation();
        float diff = mWantedRotation - curRot;
        while(diff > Math.PI) {
            diff -= Math.PI * 2;
        }
        while(diff < -Math.PI) {
            diff += Math.PI * 2;
        }
        float maxRotSpeed = mShipType.getMaxRotationSpeed();
        float speedLimit = (float) (Math.PI * frameDuration * maxRotSpeed);
        if (diff > speedLimit) {
            diff = speedLimit;
        } else if (diff < -speedLimit) {
            diff = -speedLimit;
        }
        curRot += diff;
        setRotation(curRot);
    }

    @Override
    protected void buildStructure(Scene scene) {
        super.buildStructure(scene);
        setThrust(scene, false);
    }

    @Override
    public PackMsg prepareNetworkMessage(Scene scene, int frameNumber) {
        if (otherPlayer != null) {
            PackMsg.ShipInfo shipInfo = new PackMsg.ShipInfo(scene, this, frameNumber, otherPlayer);
            int reactor = shipInfo.reactor;
            setReactorPower(ReactorPower.find(reactor), this, scene.getRemoteShip());
            return shipInfo;
        }
        return null;
    }

    public int getReactorForNetwork(Scene scene) {
        int thrust = (mThrustDate != 0) ? ReactorPower.ON.mValue : ReactorPower.OFF.mValue;
        int reactor = boostInProgress() ? ReactorPower.BOOST.mValue : thrust;
        if (scene.isGameOver()) {
            reactor = ReactorPower.ON.mValue;
        }
        return reactor;
    }

    private void handleDestroyed(Scene scene) {
        if (mExplosion != null) {
            if (mExplosion.isExplosionFinished()) {
                mExplosion = null;
                setVisible(true);
                scene.invertRoles();
            }
            return;
        }
        if (scene.isGameOver()) {
            return;
        }
        ArrayList<SpaceObject> killers = scene.getKillersCopy();
        if (!killers.isEmpty()) {
            float shipX = getPosX();
            float shipY = getPosY();
            float shipRadius = getOriginalSize();
            for(SpaceObject killer : killers) {
                float diffX = killer.getPosX() - shipX;
                float diffY = killer.getPosY() - shipY;
                float radius2 = (shipRadius + killer.getOriginalSize());
                radius2 *= radius2;
                if ((diffX*diffX) + (diffY*diffY) < radius2) {
                    //Log.d("SHOOT", "BIM!");
                    mSounds.stopEngine();
                    mSounds.playExplosion();
                    scene.setNeutralShipType();
                    mExplosion = new Explosion(this);
                    scene.addObject(mExplosion);
                    sendKilledMessage(scene);
                    setVisible(false);
                    break;
                }
            }
        }
    }

    private void sendKilledMessage(Scene scene) {
        if (otherPlayer != null) {
            PackMsg.Killed killed = new PackMsg.Killed(otherPlayer);
            scene.sendMessage(killed);
        }
    }

    public PackMsg shoot(int frameNumber) {
        if (!mShipType.canFire()) {
            return null;
        }
        if (otherPlayer == null) {
            return null;
        }
        long now = SystemClock.uptimeMillis();
        if (now - mLastShoot < TIME_BETWEEN_BULLETS) {  // too fast, wait
            return null;
        } else {
            mShoot = true;
            mLastShoot = now;

            mSounds.playShoot();

            PackMsg.ShipFire shipFire = new PackMsg.ShipFire(this, frameNumber, otherPlayer);
            return shipFire;
        }
    }

    public void boost() {
        final long now = SystemClock.uptimeMillis();
        if (mBoostEndDate > now) {
            mBoostEndDate += BOOST_DURATION;
        } else {
            mBoostEndDate = now + BOOST_DURATION;
        }
    }
    private boolean boostInProgress() {
        final long now = SystemClock.uptimeMillis();
        return (mBoostEndDate > now);
    }
    public boolean isExploding() {
        return (mExplosion != null);
    }
}