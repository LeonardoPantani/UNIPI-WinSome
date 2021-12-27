/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server.entities;

/**
 * Classe che rappresenta un voto (positivo/negativo) del social WinSome. Ogni voto è caratterizzato da
 * ovviamente l'autore, il valore del voto stesso (sarà poi la classe che implementerà questo oggetto che
 * sceglierà i valori che un voto può assumere) e la data di invio in tempo UNIX. E' un oggetto immutabile.
 */
public class WinSomeVote {
    private final String author;
    private final int vote;
    private final long dateSent;

    /**
     * Questo costruttore inizializza un oggetto di tipo WinSomeVote. La data viene impostata in automatico.
     * @param author l'autore del voto
     * @param vote il valore del voto
     */
    public WinSomeVote(String author, int vote) {
        this.author = author;
        this.vote = vote;
        this.dateSent = System.currentTimeMillis();
    }

    /**
     * Fornisce l'autore del voto
     * @return l'autore del voto
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Fornisce il valore del voto (sarà la classe che implementa questo oggetto a scegliere il range
     * di valori accettati)
     * @return il valore del voto
     */
    public int getVote() {
        return vote;
    }

    /**
     * Fornisce la data di invio del voto
     * @return la data di invio del voto in tempo UNIX
     */
    public long getDateSent() {
        return dateSent;
    }
}
