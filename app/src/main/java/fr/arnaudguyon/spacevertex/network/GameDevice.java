/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.network;

import androidx.annotation.NonNull;

public abstract class GameDevice {

    public abstract boolean equalsTo(@NonNull GameDevice other);

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
