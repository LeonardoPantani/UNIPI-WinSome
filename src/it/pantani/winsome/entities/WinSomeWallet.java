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

    public WinSomeWallet(String username) {
        this.username = username;
        this.transactions = new ConcurrentLinkedQueue<>();
    }

    public String getUsername() {
        return username;
    }

    public double getBalance() {
        double balance = 0;
        for(WinSomeTransaction t : transactions) {
            balance += t.getEdit();
        }

        return balance;
    }

    public ConcurrentLinkedQueue<WinSomeTransaction> getTransactions() {
        return transactions;
    }

    public double changeBalance(double edit, String reason) {
        transactions.add(new WinSomeTransaction(edit, reason));

        return getBalance();
    }
}
