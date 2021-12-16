/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome;

import it.pantani.winsome.entities.WinSomeUser;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class SocialManager {
    private ConcurrentHashMap<String, WinSomeUser> userList;
    private ConcurrentHashMap<String, ArrayList<String>> followersList;
    private ConcurrentHashMap<String, ArrayList<String>> followingList; // ridondanza

    public SocialManager() {
        userList = new ConcurrentHashMap<>();
        followersList = new ConcurrentHashMap<>();
        followingList = new ConcurrentHashMap<>();
    }

    public void setUserList(ConcurrentHashMap<String, WinSomeUser> userList) {
        this.userList = userList;
    }

    public void setFollowersList(ConcurrentHashMap<String, ArrayList<String>> followersList) {
        this.followersList = followersList;
    }

    public void setFollowingList(ConcurrentHashMap<String, ArrayList<String>> followingList) {
        this.followingList = followingList;
    }

    public ConcurrentHashMap<String, ArrayList<String>> getFollowersList() {
        return followersList;
    }

    public ConcurrentHashMap<String, ArrayList<String>> getFollowingList() {
        return followingList;
    }

    public ConcurrentHashMap<String, WinSomeUser> getUserList() {
        return userList;
    }

    public void addUser(WinSomeUser user) {
        userList.put(user.getUsername(), user);
    }

    public void addFollower(String username, String follower) {
        if (followersList.containsKey(username)) {
            followersList.get(username).add(follower);
        } else {
            ArrayList<String> list = new ArrayList<>();
            list.add(follower);
            followersList.put(username, list);
        }
    }

    public void addFollowing(String username, String following) {
        if (followingList.containsKey(username)) {
            followingList.get(username).add(following);
        } else {
            ArrayList<String> list = new ArrayList<>();
            list.add(following);
            followingList.put(username, list);
        }
    }

    public WinSomeUser getUser(String username) {
        return userList.get(username);
    }

    public ArrayList<String> getFollowers(String username) {
        return followersList.get(username);
    }

    public ArrayList<String> getFollowing(String username) {
        return followingList.get(username);
    }

    public void removeUser(String username) {
        userList.remove(username);
        followersList.remove(username);
        followingList.remove(username);
    }

    public void removeFollower(String username, String follower) {
        followersList.get(username).remove(follower);
    }

    public void removeFollowing(String username, String following) {
        followingList.get(username).remove(following);
    }

    public boolean findUser(String username) {
        return userList.containsKey(username);
    }

    public boolean findFollower(String username, String follower) {
        return followersList.get(username).contains(follower);
    }

    public boolean findFollowing(String username, String following) {
        return followingList.get(username).contains(following);
    }

    public int getUserCount() {
        return userList.size();
    }

    public int getFollowerCount(String username) {
        return followersList.get(username).size();
    }

    public int getFollowingCount(String username) {
        return followingList.get(username).size();
    }
}
