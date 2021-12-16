/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.entities;

import java.net.Socket;

public class WinSomeSession {
    private final String username;
    private final Socket sessionSocket;
    private final long timestamp;

    public WinSomeSession(Socket sessionSocket, String username) {
        this.username = username;
        this.sessionSocket = sessionSocket;
        timestamp = System.currentTimeMillis();
    }

    public Socket getSessionSocket() {
        return sessionSocket;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getUsername() {
        return username;
    }
}
