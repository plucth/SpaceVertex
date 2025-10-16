/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.network;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

import fr.arnaudguyon.spacevertex.objects.Scene;
import fr.arnaudguyon.spacevertex.objects.ShipLocal;

public abstract class PackMsg {

    private static final int HEADER_TYPE_IDX = 0;
    private static final int HEADER_SIZE = HEADER_TYPE_IDX + 1;

    private static final int SHIPINFO_X_IDX = HEADER_SIZE;
    private static final int SHIPINFO_Y_IDX = SHIPINFO_X_IDX + 4;
    private static final int SHIPINFO_ROTATION_IDX = SHIPINFO_Y_IDX + 2; //4;
    private static final int SHIPINFO_SPEED_X_IDX = SHIPINFO_ROTATION_IDX + 4;
    private static final int SHIPINFO_SPEED_Y_IDX = SHIPINFO_SPEED_X_IDX + 4;
    private static final int SHIPINFO_REACTOR_IDX = SHIPINFO_SPEED_Y_IDX + 4;
    private static final int SHIPINFO_SIZE = SHIPINFO_REACTOR_IDX + 1;

    private static final int SHIPFIRE_X_IDX = HEADER_SIZE;
    private static final int SHIPFIRE_Y_IDX = SHIPFIRE_X_IDX + 4;
    private static final int SHIPFIRE_ROTATION_IDX = SHIPFIRE_Y_IDX + 4;
    private static final int SHIPFIRE_SIZE = SHIPFIRE_ROTATION_IDX + 4;

    private static final int GAMECHRONO_VALUE_IDX = HEADER_SIZE;
    private static final int GAMECHRONO_SIZE = GAMECHRONO_VALUE_IDX + 4;

    private static final int SCORE_VALUE_IDX = HEADER_SIZE;
    private static final int SCORE_SCORER_IDX = SCORE_VALUE_IDX + 4;
    private static final int SCORE_SIZE = SCORE_SCORER_IDX + 1;

    private static final int NETWORKVERSION_VALUE_IDX = HEADER_SIZE;
    private static final int NETWORKVERSION_SIZE = NETWORKVERSION_VALUE_IDX + 4;

    public enum MsgType {
        UNKNOWN((byte) 0),
        NETWORK_VERSION((byte) 1),  // TODO: rename COMPATIBILITY_VERSION ?
        WRONG_NETWORK_VERSION((byte) 2),
        SESSION_FULL((byte) 3),
        QUITTING((byte) 4),
        START_GAME((byte) 5),
        GAME_RESTART((byte) 6),
        GAME_CHRONO((byte) 7),
        SHIP_INFO((byte) 8),
        SHIP_FIRE((byte) 9),
        SHIP_SCORE((byte) 10),
        KILLED((byte) 11);

        private final byte rawValue;

        MsgType(byte rawValue) {
            this.rawValue = rawValue;
        }

        static @NonNull
        MsgType get(byte value) {
            for (MsgType type : MsgType.values()) {
                if (type.rawValue == value) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }

    public enum SendPolicy {
        STACK_WHEN_BUSY,
        SKIP_WHEN_BUSY
    }

    protected final byte[] buffer;
    private SendPolicy sendPolicy = SendPolicy.STACK_WHEN_BUSY;
    private final @NonNull GameDevice gameDevice;

    public @NonNull
    byte[] getBuffer() {
        return buffer;
    }

    public SendPolicy getSendPolicy() {
        return sendPolicy;
    }

    public MsgType getType() {
        return MsgType.get(buffer[HEADER_TYPE_IDX]);
    }

    public @NonNull GameDevice getDevice() {
        return gameDevice;  // can be target device when message is sent, or source device if message is received
    }

    private PackMsg(MsgType type, int bufferSize, int frame, SendPolicy sendPolicy, @NonNull GameDevice gameDevice) {
        buffer = new byte[bufferSize];
        buffer[HEADER_TYPE_IDX] = type.rawValue;
        this.sendPolicy = sendPolicy;
        this.gameDevice = gameDevice;
    }

    private PackMsg(@NonNull byte[] buffer, @NonNull GameDevice gameDevice) {
        this.buffer = buffer;
        this.gameDevice = gameDevice;
    }

    public static PackMsg create(@NonNull byte[] buffer, @NonNull GameDevice gameDevice) {
        if (buffer.length >= HEADER_SIZE) {
            byte type = buffer[HEADER_TYPE_IDX];
            MsgType msgType = MsgType.get(type);
            switch (msgType) {
                case SHIP_INFO:
                    return new ShipInfo(buffer, gameDevice);
                case SHIP_FIRE:
                    return new ShipFire(buffer, gameDevice);
                case SHIP_SCORE:
                    return new ShipScore(buffer, gameDevice);
                case KILLED:
                    return new Killed(buffer, gameDevice);
                case GAME_CHRONO:
                    return new GameChrono(buffer, gameDevice);
                case GAME_RESTART:
                    return new GameRestart(buffer, gameDevice);
                case NETWORK_VERSION:
                    return new NetworkVersion(buffer, gameDevice);
                case QUITTING:
                    return new QuitGame(buffer, gameDevice);
                case SESSION_FULL:
                    return new SessionFull(buffer, gameDevice);
                case WRONG_NETWORK_VERSION:
                    return new WrongNetworkVersion(buffer, gameDevice);
                case START_GAME:
                    return new StartGame(buffer, gameDevice);
            }
        }
        return null;
    }

    protected short floatToShort(float value) {
        return (short) (value * 10000);
    }

    protected float shortToFloat(short value) {
        return (value / 10000f);
    }

    public static class ShipInfo extends PackMsg {

        public final float x;
        public final float y;
        public final float rotation;
        public final float speedX;
        public final float speedY;
        public final byte reactor;

        public ShipInfo(Scene scene, ShipLocal ship, int frame, @NonNull GameDevice targetDevice) {
            super(MsgType.SHIP_INFO, SHIPINFO_SIZE, frame, SendPolicy.SKIP_WHEN_BUSY, targetDevice);
            x = ship.getPosX();
            y = ship.getPosY();
            rotation = ship.getRotation();
            speedX = ship.getSpeedX();
            speedY = ship.getSpeedY();
            reactor = (byte) ship.getReactorForNetwork(scene);
            ByteBuffer.wrap(buffer).putFloat(SHIPINFO_X_IDX, x);
            ByteBuffer.wrap(buffer).putFloat(SHIPINFO_Y_IDX, y);
//            ByteBuffer.wrap(buffer).putFloat(SHIPINFO_ROTATION_IDX, rotation);
            ByteBuffer.wrap(buffer).putShort(SHIPINFO_ROTATION_IDX, floatToShort(rotation));
            ByteBuffer.wrap(buffer).putFloat(SHIPINFO_SPEED_X_IDX, speedX);
            ByteBuffer.wrap(buffer).putFloat(SHIPINFO_SPEED_Y_IDX, speedY);
            buffer[SHIPINFO_REACTOR_IDX] = reactor;
        }

        public ShipInfo(@NonNull byte[] buffer, @NonNull GameDevice gameDevice) {
            super(buffer, gameDevice);
            x = ByteBuffer.wrap(buffer).getFloat(SHIPINFO_X_IDX);
            y = ByteBuffer.wrap(buffer).getFloat(SHIPINFO_Y_IDX);
//            rotation = ByteBuffer.wrap(buffer).getFloat(SHIPINFO_ROTATION_IDX);
            rotation = shortToFloat(ByteBuffer.wrap(buffer).getShort(SHIPINFO_ROTATION_IDX));
            speedX = ByteBuffer.wrap(buffer).getFloat(SHIPINFO_SPEED_X_IDX);
            speedY = ByteBuffer.wrap(buffer).getFloat(SHIPINFO_SPEED_Y_IDX);
            reactor = buffer[SHIPINFO_REACTOR_IDX];
        }
    }

    public static class ShipFire extends PackMsg {

        public final float x;
        public final float y;
        public final float rotation;

        public ShipFire(ShipLocal ship, int frame, @NonNull GameDevice targetDevice) {
            super(MsgType.SHIP_FIRE, SHIPFIRE_SIZE, frame, SendPolicy.STACK_WHEN_BUSY, targetDevice);
            x = ship.getPosX();
            y = ship.getPosY();
            rotation = ship.getRotation();
            ByteBuffer.wrap(buffer).putFloat(SHIPFIRE_X_IDX, x);
            ByteBuffer.wrap(buffer).putFloat(SHIPFIRE_Y_IDX, y);
            ByteBuffer.wrap(buffer).putFloat(SHIPFIRE_ROTATION_IDX, rotation);
        }

        public ShipFire(@NonNull byte[] buffer, @NonNull GameDevice gameDevice) {
            super(buffer, gameDevice);
            x = ByteBuffer.wrap(buffer).getFloat(SHIPFIRE_X_IDX);
            y = ByteBuffer.wrap(buffer).getFloat(SHIPFIRE_Y_IDX);
            rotation = ByteBuffer.wrap(buffer).getFloat(SHIPFIRE_ROTATION_IDX);
        }
    }

    public static class GameRestart extends PackMsg {

        public GameRestart(@NonNull GameDevice targetDevice) {
            super(MsgType.GAME_RESTART, HEADER_SIZE + 1, 0, SendPolicy.STACK_WHEN_BUSY, targetDevice);
        }

        public GameRestart(@NonNull byte[] buffer, @NonNull GameDevice gameDevice) {
            super(buffer, gameDevice);
        }
    }

    public static class GameChrono extends PackMsg {

        public final int value;

        public GameChrono(int value, @NonNull GameDevice targetDevice) {
            super(MsgType.GAME_CHRONO, GAMECHRONO_SIZE, 0, SendPolicy.STACK_WHEN_BUSY, targetDevice);
            this.value = value;
            ByteBuffer.wrap(buffer).putInt(GAMECHRONO_VALUE_IDX, value);
        }

        public GameChrono(@NonNull byte[] buffer, @NonNull GameDevice gameDevice) {
            super(buffer, gameDevice);
            value = ByteBuffer.wrap(buffer).getInt(GAMECHRONO_VALUE_IDX);
        }

    }

    public static class ShipScore extends PackMsg {

        public enum Scorer {
            CAT(0), MOUSE(1);
            final int scorer;

            Scorer(int raw) {
                this.scorer = raw;
            }

            static Scorer findScorer(int raw, Scorer defaultValue) {
                for (Scorer scorer : Scorer.values()) {
                    if (scorer.scorer == raw) {
                        return scorer;
                    }
                }
                return defaultValue;
            }
        }

        public final Scorer scorer;
        public final int score;

        public ShipScore(Scorer scorer, int score, @NonNull GameDevice targetDevice) {
            super(MsgType.SHIP_SCORE, SCORE_SIZE, 0, SendPolicy.STACK_WHEN_BUSY, targetDevice);
            this.scorer = scorer;
            this.score = score;
            buffer[SCORE_SCORER_IDX] = (byte) scorer.scorer;
            ByteBuffer.wrap(buffer).putInt(SCORE_VALUE_IDX, score);
        }

        public ShipScore(@NonNull byte[] buffer, @NonNull GameDevice gameDevice) {
            super(buffer, gameDevice);
            scorer = Scorer.findScorer(buffer[SCORE_SCORER_IDX], Scorer.CAT);
            score = ByteBuffer.wrap(buffer).getInt(SCORE_VALUE_IDX);
        }

    }

    public static class NetworkVersion extends PackMsg {

        public final int version;

        public NetworkVersion(int version, @NonNull GameDevice targetDevice) {
            super(MsgType.NETWORK_VERSION, NETWORKVERSION_SIZE, 0, SendPolicy.STACK_WHEN_BUSY, targetDevice);
            this.version = version;
            ByteBuffer.wrap(buffer).putInt(NETWORKVERSION_VALUE_IDX, version);
        }

        public NetworkVersion(@NonNull byte[] buffer, @NonNull GameDevice gameDevice) {
            super(buffer, gameDevice);
            version = ByteBuffer.wrap(buffer).getInt(NETWORKVERSION_VALUE_IDX);
        }

    }

    public static abstract class SimpleMsg extends PackMsg {

        protected SimpleMsg(MsgType type, @NonNull GameDevice targetDevice) {
            super(type, HEADER_SIZE, 0, SendPolicy.STACK_WHEN_BUSY, targetDevice);
        }

        protected SimpleMsg(@NonNull byte[] buffer, @NonNull GameDevice gameDevice) {
            super(buffer, gameDevice);
        }
    }

    public static class Killed extends SimpleMsg {
        public Killed(@NonNull GameDevice targetDevice) {
            super(MsgType.KILLED, targetDevice);
        }

        protected Killed(@NonNull byte[] buffer, @NonNull GameDevice gameDevice) {
            super(buffer, gameDevice);
        }
    }

    public static class SessionFull extends SimpleMsg {
        public SessionFull(@NonNull GameDevice targetDevice) {
            super(MsgType.SESSION_FULL, targetDevice);
        }

        protected SessionFull(@NonNull byte[] buffer, @NonNull GameDevice gameDevice) {
            super(buffer, gameDevice);
        }
    }

    public static class WrongNetworkVersion extends SimpleMsg {
        public WrongNetworkVersion(@NonNull GameDevice targetDevice) {
            super(MsgType.WRONG_NETWORK_VERSION, targetDevice);
        }

        protected WrongNetworkVersion(@NonNull byte[] buffer, @NonNull GameDevice gameDevice) {
            super(buffer, gameDevice);
        }
    }

    public static class StartGame extends SimpleMsg {
        public StartGame(@NonNull GameDevice targetDevice) {
            super(MsgType.START_GAME, targetDevice);
        }

        protected StartGame(@NonNull byte[] buffer, @NonNull GameDevice gameDevice) {
            super(buffer, gameDevice);
        }
    }

    public static class QuitGame extends SimpleMsg {
        public QuitGame(@NonNull GameDevice targetDevice) {
            super(MsgType.QUITTING, targetDevice);
        }

        protected QuitGame(@NonNull byte[] buffer, @NonNull GameDevice gameDevice) {
            super(buffer, gameDevice);
        }
    }

}
