/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.entities;

public class WinSomeVote {
    private final String author;
    private final int vote;

    public WinSomeVote(String author, int vote) {
        this.author = author;
        this.vote = vote;
    }

    public String getAuthor() {
        return author;
    }

    public int getVote() {
        return vote;
    }
}
