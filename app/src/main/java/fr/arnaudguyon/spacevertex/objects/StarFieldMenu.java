/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.objects;

public class StarFieldMenu extends StarField {

    private boolean mScrollStars = true;
    private float mSpeed = 0.3f;

    public StarFieldMenu(int nbStars, boolean bigStar) {
        super(nbStars, bigStar);
    }

    public void scrollStars(boolean scrollStars, float speed) {
        mScrollStars = scrollStars;
        mSpeed = speed;
    }

    @Override
    public void preDraw(Scene scene, float frameDuration) {

        if (mScrollStars) {
            float screenScale = getScreenScale() * mSpeed;
            float speed = mBigStar ? 2 : 1;

            for (StarData data : mStars) {
                data.mX -= screenScale * speed;
            }
        }
        super.preDraw(scene, frameDuration);
    }
}
