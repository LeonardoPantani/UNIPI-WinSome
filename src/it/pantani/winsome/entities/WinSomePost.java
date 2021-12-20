/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.entities;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class WinSomePost {
    private final int postID;
    private final String author;
    private final String postTitle;
    private final String postContent;
    private final long dateSent;
    private final ConcurrentHashMap<String, Integer> votes;
    private ArrayList<WinSomeComment> comments;

    public WinSomePost(int postID, String author, String postTitle, String postContent) {
        this.postID = postID;
        this.author = author;
        this.postTitle = postTitle;
        this.postContent = postContent;
        this.dateSent = System.currentTimeMillis();
        this.votes = new ConcurrentHashMap<>();
    }

    public int getPostID() {
        return postID;
    }

    public String getAuthor() {
        return author;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public String getPostContent() {
        return postContent;
    }

    public long getDateSent() {
        return dateSent;
    }

    public ConcurrentHashMap<String, Integer> getVoteList() {
        return votes;
    }

    public int getTotalVotes() {
        return votes.size();
    }

    public int getUpvotes() {
        int up = 0;

        for(Integer value : votes.values()) {
            if(value == 1) up++;
        }

        return up;
    }

    public int getDownvotes() {
        int down = 0;

        for(Integer value : votes.values()) {
            if(value == -1) down++;
        }

        return down;
    }

    public ArrayList<WinSomeComment> getComments() {
        return comments;
    }

    public void addVote(String username, int value) {
        votes.putIfAbsent(username, value);
    }

    public void addComment(String username, String content) {
        if(comments == null) comments = new ArrayList<>();
        comments.add(new WinSomeComment(username, content));
    }

    public Integer findVoteByUser(String username) {
        return votes.get(username);
    }

    public ArrayList<WinSomeComment> findCommentsByUser(String username) {
        ArrayList<WinSomeComment> ret = null;

        for(WinSomeComment c : comments) {
            if(c.getAuthor().equals(username)) {
                if(ret == null) ret = new ArrayList<>();
                ret.add(c);
            }
        }
        return ret;
    }
}
