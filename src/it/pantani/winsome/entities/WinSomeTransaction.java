/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.entities;

public class WinSomeTransaction {
    private final float edit;
    private final long date;

    public WinSomeTransaction(float edit) {
        this.edit = edit;
        this.date = System.currentTimeMillis();
    }

    public float getEdit() {
        return edit;
    }

    public long getDate() {
        return date;
    }
}
