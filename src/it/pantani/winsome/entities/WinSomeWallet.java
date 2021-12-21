/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.entities;

import java.util.concurrent.ConcurrentLinkedQueue;

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

    public float getBalance() {
        float balance = 0;
        for(WinSomeTransaction t : transactions) {
            balance += t.getEdit();
        }

        return balance;
    }

    public ConcurrentLinkedQueue<WinSomeTransaction> getTransactions() {
        return transactions;
    }

    public float changeBalance(float edit) {
        transactions.add(new WinSomeTransaction(edit));

        return getBalance();
    }
}
