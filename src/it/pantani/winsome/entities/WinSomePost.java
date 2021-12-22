/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.entities;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WinSomePost {
    private final int postID;
    private final String author;
    private final String postTitle;
    private final String postContent;
    private final long dateSent;
    private final ConcurrentHashMap<String, WinSomeVote> votes;
    private final ArrayList<WinSomeComment> comments;
    private final ConcurrentLinkedQueue<String> rewinUsers;
    private int numIterations;

    public WinSomePost(int postID, String author, String postTitle, String postContent) {
        this.postID = postID;
        this.author = author;
        this.postTitle = postTitle;
        this.postContent = postContent;
        this.dateSent = System.currentTimeMillis();
        this.votes = new ConcurrentHashMap<>();
        this.comments = new ArrayList<>();
        this.rewinUsers = new ConcurrentLinkedQueue<>();

        this.numIterations = 0;
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

    public ConcurrentHashMap<String, WinSomeVote> getVoteList() {
        return votes;
    }

    public ConcurrentHashMap<String, WinSomeVote> getVoteList(long after_this_date) {
        ConcurrentHashMap<String, WinSomeVote> ret = new ConcurrentHashMap<>();

        for(WinSomeVote v : votes.values()) {
            if(v.getDateSent() >= after_this_date) {
                ret.put(v.getAuthor(), v);
            }
        }

        return ret;
    }

    public int getTotalVotes() {
        return votes.size();
    }

    public int getUpvotes() {
        int up = 0;

        for(WinSomeVote value : votes.values()) {
            if(value.getVote() == 1) up++;
        }

        return up;
    }

    public int getDownvotes() {
        int down = 0;

        for(WinSomeVote value : votes.values()) {
            if(value.getVote() == -1) down++;
        }

        return down;
    }

    public ArrayList<WinSomeComment> getCommentList() {
        return comments;
    }

    public ArrayList<WinSomeComment> getCommentList(long after_this_date) {
        ArrayList<WinSomeComment> ret = null;
        for(WinSomeComment c : comments) {
            if(c.getDateSent() >= after_this_date) {
                if(ret == null) ret = new ArrayList<>();
                ret.add(c);
            }
        }

        return ret;
    }

    public void addVote(String username, int value) {
        votes.putIfAbsent(username, new WinSomeVote(username, value));
    }

    public void addComment(String username, String content) {
        comments.add(new WinSomeComment(username, content));
    }

    public Integer findVoteByUser(String username) {
        WinSomeVote v = votes.get(username);
        if(v == null) return null;
        return v.getVote();
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

    public ArrayList<WinSomeComment> findCommentsByUser(String username, long after_this_date) {
        ArrayList<WinSomeComment> ret = null;

        for(WinSomeComment c : comments) {
            if(c.getAuthor().equals(username) && c.getDateSent() >= after_this_date) {
                if(ret == null) ret = new ArrayList<>();
                ret.add(c);
            }
        }
        return ret;
    }

    public ArrayList<String> getUsersCommenting() {
        ArrayList<String> ret = null;

        for(WinSomeComment c : comments) {
            if(ret == null) ret = new ArrayList<>();
            if(!ret.contains(c.getAuthor())) {
                ret.add(c.getAuthor());
            }
        }

        return ret;
    }

    public ArrayList<String> getUsersCommenting(long after_this_date) {
        ArrayList<String> ret = new ArrayList<>();

        for(WinSomeComment c : comments) {
            if(!ret.contains(c.getAuthor()) && c.getDateSent() >= after_this_date) {
                ret.add(c.getAuthor());
            }
        }

        return ret;
    }

    public ConcurrentLinkedQueue<String> getRewinUsers() {
        return rewinUsers;
    }

    public boolean addRewin(String username) {
        if(rewinUsers.contains(username)) {
            return false;
        } else {
            rewinUsers.add(username);
            return true;
        }
    }

    public boolean isRewinUserPresent(String username) {
        return rewinUsers.contains(username);
    }

    public int getNumIterations() {
        return numIterations;
    }

    public int addIteration() {
        return ++this.numIterations;
    }
}
