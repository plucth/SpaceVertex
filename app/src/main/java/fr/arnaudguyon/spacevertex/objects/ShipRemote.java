/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.objects;

import android.content.Context;
import android.graphics.Canvas;

import fr.arnaudguyon.spacevertex.network.PackMsg;

public class ShipRemote extends Ship {

    private Explosion mExplosion;

    public ShipRemote(Context context) {
        super(context);
    }

    @Override
    public void preDraw(Scene scene, float frameDuration) {
        // normal move with last registered speed
        handleSpeed(frameDuration);
        if (mReactorPower == ReactorPower.OFF) {
            handleBreak(scene, frameDuration);
        }
        if (scene.isGameOver()) {
            setReactorPower(Ship.ReactorPower.ON, scene.getLocalShip(), this);
        }
        handlesFlamesAnimation();
        handleShoot(scene, true);
        handleExplosion(scene);
        mShoot = false;
    }

    @Override
    public void draw(Canvas canvas, float spaceCenterX, float spaceCenterY) {
        super.draw(canvas, spaceCenterX, spaceCenterY);
    }

    @Override
    public void onNetworkMessageReceived(Scene scene, PackMsg packMsg) {
        if (packMsg instanceof PackMsg.ShipInfo) {
            PackMsg.ShipInfo shipInfo = (PackMsg.ShipInfo) packMsg;
            float posx = shipInfo.x;
            float posy = shipInfo.y;
            // Smooth position
            if (!scene.isGameOver()) {
                posx = (getPosX() * 0.95f) + (posx * 0.05f);
                posy = (getPosY() * 0.95f) + (posy * 0.05f);
            }
            setPos(posx, posy);
            mSpeedX = shipInfo.speedX;
            mSpeedY = shipInfo.speedY;
            float rot = shipInfo.rotation;
            setRotation(rot);
            int reactor = shipInfo.reactor;
            setReactorPower(ReactorPower.find(reactor), scene.getLocalShip(), this);
        }
    }

    public void setDestroyed(Scene scene) {
        mSounds.playRemoteExplosion(scene.getLocalShip(), this);
        scene.setNeutralShipType();
        mExplosion = new Explosion(this);
        scene.addObject(mExplosion);
        setVisible(false);
    }

    private void handleExplosion(Scene scene) {
        if (mExplosion != null) {
            if (mExplosion.isExplosionFinished()) {
                mExplosion = null;
                scene.invertRoles();
                setVisible(true);
            }
        }
    }

    public void fire(Scene scene, PackMsg.ShipFire shipFire, ShipLocal shipLocal) {
        float posX = shipFire.x;
        float posY = shipFire.y;
        float rotation = shipFire.rotation;

        boolean canKill = mShipType.canFire();

        mSounds.playRemoteShoot(shipLocal, this);

        FireBall ball = new FireBall(getOriginalSize());
        float[] firePos = new float[2];
        getFirePos(18, posX, posY, rotation, firePos);
        ball.setPos(firePos[0], firePos[1]);
        float[] fireSpeed = new float[2];
        getFireSpeed(rotation, fireSpeed);
        ball.setSpeed(fireSpeed[0], fireSpeed[1]);
        ball.setCanKill(canKill);
        scene.addObject(ball);

        FireBall ball2 = new FireBall(getOriginalSize());
        getFirePos(22, posX, posY, rotation, firePos);
        ball2.setPos(firePos[0], firePos[1]);
        ball2.setSpeed(fireSpeed[0], fireSpeed[1]);
        ball2.setCanKill(canKill);
        scene.addObject(ball2);
    }

    public boolean isExploding() {
        return (mExplosion != null);
    }

}
