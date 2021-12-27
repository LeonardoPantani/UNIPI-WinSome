/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.entities;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Classe che rappresenta il portafoglio di un utente. Il bilancio (che può essere anche negativo, sarà
 * la classe che implementa il wallet ad effettuare controlli) viene calcolato dalla lista di transazioni
 * che un utente ha fatto.
 */
public class WinSomeWallet {
    private final String username;
    private final ConcurrentLinkedQueue<WinSomeTransaction> transactions;

    /**
     * Questo costruttore inizializza un wallet.
     * @param username l'username dell'utente che possiede questo portafoglio
     */
    public WinSomeWallet(String username) {
        this.username = username;
        this.transactions = new ConcurrentLinkedQueue<>();
    }

    /**
     * Fornisce l'username dell'utente che possiede questo portafoglio
     * @return username del proprietario di questo portafoglio
     */
    public String getUsername() {
        return username;
    }

    /**
     * Fornisce la lista delle transazioni di questo portafoglio
     * @return lista transazioni
     */
    public ConcurrentLinkedQueue<WinSomeTransaction> getTransactions() {
        return transactions;
    }

    /**
     * Fornisce il bilancio di questo portafoglio. Visto che l'oggetto WinSomeWallet non contiene un bilancio
     * che viene aggiornato, il bilancio è calcolato ogni volta.
     * @return bilancio di questo portafoglio
     */
    public double getBalance() {
        double balance = 0;
        for(WinSomeTransaction t : transactions) {
            balance += t.getEdit();
        }

        return balance;
    }

    /**
     * Permette di inserire una nuova transazione nel portafoglio. Devono essere forniti il valore della transazione,
     * che può essere sia positivo che negativo (la classe che fa uso di questo metodo si occupa dei controlli) e la
     * causale della transazione.
     * @param edit valore della nuova transazione
     * @param reason causale della nuova transazione
     * @return il nuovo bilancio dopo la modifica
     */
    public double changeBalance(double edit, String reason) {
        transactions.add(new WinSomeTransaction(edit, reason));

        return getBalance();
    }
}
