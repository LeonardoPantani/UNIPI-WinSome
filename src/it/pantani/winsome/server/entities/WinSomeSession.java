/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server.entities;

import java.net.Socket;

/**
 * Rappresenta una sessione di un client loggato. Molto utile per risalire all'utente
 * che è attualmente collegato. Contiene anche un timestamp che salva quando l'utente
 * ha effettuato il login.
 */
public class WinSomeSession {
    private final String username;
    private final Socket sessionSocket;
    private final long timestamp;

    /**
     * Inizializza una sessione dato un socket e un nome utente
     * @param sessionSocket il socket di collegamento col client
     * @param username il nome utente relativo al socket
     */
    public WinSomeSession(Socket sessionSocket, String username) {
        this.username = username;
        this.sessionSocket = sessionSocket;
        timestamp = System.currentTimeMillis();
    }

    /**
     * Fornisce il socket della sessione corrente
     * @return il socket della sessione corrente
     */
    public Socket getSessionSocket() {
        return sessionSocket;
    }

    /**
     * Fornisce l'username dell'utente relativo a questa sessione
     * @return l'username dell'utente
     */
    public String getUsername() {
        return username;
    }

    /**
     * Fornisce il tempo UNIX al quale l'utente ha effettuato il login
     * @return il timestamp di login
     */
    public long getTimestamp() {
        return timestamp;
    }
}
