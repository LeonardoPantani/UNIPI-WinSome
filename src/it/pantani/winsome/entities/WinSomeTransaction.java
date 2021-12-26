/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.entities;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Classe che rappresenta una singola transazione del social WinSome. Contiene ovviamente il
 * cambiamento (change) al wallet dell'utente, la data della transazione e la causale.
 * E' un oggetto immutabile.
 */
public class WinSomeTransaction {
    // rappresenta la modifica in termini di valuta winsome
    private final double edit;
    private final String reason;
    private final long date;

    /**
     * Questo costruttore inizializza un oggetto di tipo WinSomeTransaction. La data viene impostata in automatico.
     * @param edit il valore della transazione (può essere positivo, negativo o anche 0)
     * @param reason la causale
     */
    public WinSomeTransaction(double edit, String reason) {
        this.edit = edit;
        this.reason = reason;
        this.date = System.currentTimeMillis();
    }

    /**
     * Fornisce la modifica in termini di valuta winsome
     * @return il valore della transazione
     */
    public double getEdit() {
        return edit;
    }

    /**
     * Fornisce la causale della transazione
     * @return motivo della transazione (può essere null)
     */
    public String getReason() {
        return reason;
    }

    /**
     * Fornisce la data di creazione della transazione
     * @return data della transazione in tempo UNIX
     */
    public long getDate() {
        return date;
    }
}
