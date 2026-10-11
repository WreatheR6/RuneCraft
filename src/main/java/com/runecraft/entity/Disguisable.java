package com.runecraft.entity;

/**
 * A creature that can lie disguised (OSRS rock crabs look like plain rocks until a player comes
 * close). Read by the client model to draw only the disguise while {@link #isDisguised()}.
 */
public interface Disguisable {
    boolean isDisguised();
}
