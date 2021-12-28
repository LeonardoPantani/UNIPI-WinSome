/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server;

import it.pantani.winsome.server.entities.WinSomeComment;
import it.pantani.winsome.server.entities.WinSomePost;
import it.pantani.winsome.server.entities.WinSomeUser;
import it.pantani.winsome.server.entities.WinSomeWallet;
import it.pantani.winsome.server.exceptions.*;
import it.pantani.winsome.other.ConfigManager;
import it.pantani.winsome.server.utils.PasswordManager;
import it.pantani.winsome.other.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static it.pantani.winsome.other.Utils.getFormattedDate;

// TODO commentare e ottimizzare
public class SocialManager {
    private final ConfigManager config;

    public final AtomicInteger last_post_id;

    private ConcurrentHashMap<String, WinSomeUser> userList;
    private ConcurrentHashMap<String, ArrayList<String>> followersList;
    private ConcurrentHashMap<String, ArrayList<String>> followingList; // ridondanza
    private ConcurrentHashMap<Integer, WinSomePost> postList;
    private ConcurrentHashMap<String, WinSomeWallet> walletList;

    private String currency_singular;
    private String currency_plural;

    private final int decimal_places;

    public SocialManager(ConfigManager config) throws IOException {
        this.config = config;
        userList = new ConcurrentHashMap<>();
        followersList = new ConcurrentHashMap<>();
        followingList = new ConcurrentHashMap<>();
        walletList = new ConcurrentHashMap<>();
        postList = new ConcurrentHashMap<>();

        // elaborazione last_post_id
        String id_toParse = config.getPreference("last_post_id");
        if(id_toParse == null) { // caricamento config fallito
            throw new IOException("id proprieta' nullo");
        }
        int id_parsed;
        try {
            id_parsed = Integer.parseInt(id_toParse);
        } catch(NumberFormatException e) {
            throw new IOException("numero non valido");
        }
        if(id_parsed < 0) throw new IOException("numero negativo");
        last_post_id = new AtomicInteger(id_parsed);

        currency_singular = config.getPreference("currency_name_singular");
        if(currency_singular == null) currency_singular = "wincoin";

        currency_plural = config.getPreference("currency_name_plural");
        if(currency_plural == null) currency_plural = "wincoins";

        decimal_places = Integer.parseInt(config.getPreference("currency_decimal_places"));
    }

    public int createPost(String username, String post_title, String post_content) throws UserNotFoundException, InvalidOperationException {
        if(!userList.containsKey(username)) throw new UserNotFoundException();
        if(post_title.length() > Integer.parseInt(config.getPreference("post_max_title_length"))) throw new InvalidOperationException();
        if(post_content.length() > Integer.parseInt(config.getPreference("post_max_content_length"))) throw new InvalidOperationException();

        int idpost = last_post_id.getAndIncrement();
        postList.putIfAbsent(idpost, new WinSomePost(idpost, username, post_title, post_content));
        return idpost;
    }

    public void ratePost(String username, int post_id, int value) throws UserNotFoundException, InvalidVoteException, PostNotFoundException, InvalidOperationException, SameAuthorException, NotInFeedException {
        if(!userList.containsKey(username)) throw new UserNotFoundException();
        if(value != -1 && value != 1) throw new InvalidVoteException();
        WinSomePost toRate = postList.get(post_id);
        if(toRate == null) throw new PostNotFoundException();
        if(toRate.getAuthor().equals(username)) throw new SameAuthorException();
        if(!isPostInFeed(post_id, username)) throw new NotInFeedException();
        if(toRate.findVoteByUser(username) != null) throw new InvalidOperationException();

        toRate.addVote(username, value);
    }

    public void commentPost(String username, int post_id, String text) throws PostNotFoundException, SameAuthorException, NotInFeedException {
        WinSomePost toComment = postList.get(post_id);
        if(toComment == null) throw new PostNotFoundException();
        if(toComment.getAuthor().equals(username)) throw new SameAuthorException();
        if(!isPostInFeed(post_id, username)) throw new NotInFeedException();

        toComment.addComment(username, text);
    }

    public void deletePost(String username, int post_id) throws PostNotFoundException, InvalidOperationException {
        WinSomePost toDelete = postList.get(post_id);
        if(toDelete == null) throw new PostNotFoundException();
        if(!toDelete.getAuthor().equals(username)) throw new InvalidOperationException();

        postList.remove(post_id);
    }

    public WinSomePost getPost(int post_id) {
        return postList.get(post_id);
    }

    @SuppressWarnings("StringConcatenationInLoop")
    public String getPostFormatted(int post_id, boolean showRewin, boolean showContent, boolean showAuthor, boolean showVotes, boolean showCreationDate, boolean showComments) {
        WinSomePost p = postList.get(post_id);
        String ret;
        if(p == null) return null;
        ArrayList<WinSomeComment> lista_commenti = p.getCommentList();
        ConcurrentLinkedQueue<String> q = p.getRewinUsers();

        ret = "[ Post #" + p.getPostID() + " ]\n";
        if(showRewin && q.size() != 0) { ret += "* post rewinnato da " + q.size() + " "; if(q.size() == 1) { ret += "utente"; } else { ret += "utenti"; } ret += " *\n"; }
        ret += "Titolo: " + p.getPostTitle() + "\n";
        if(showContent) ret += "Contenuto: " + p.getPostContent() + "\n";
        if(showAuthor) ret += "Autore: " + p.getAuthor() + "\n";
        if(showVotes) {
            ret += "Voti (" + p.getTotalVotes() + "): " + p.getNumVotesByValue(1) + " ";
            if(p.getNumVotesByValue(1) == 1) ret += "positivo"; else ret += "positivi";
            ret += ", " + p.getNumVotesByValue(-1) + " ";
            if(p.getNumVotesByValue(-1) == 1) ret += "negativo"; else ret += "negativi";
            ret += "\n";
        }
        if(showCreationDate) ret += "Data: " + getFormattedDate(p.getDateSent()) + "\n";
        if(showComments && lista_commenti != null) {
            ret += "Commenti (" + lista_commenti.size() + "):\n";
            for(WinSomeComment c : lista_commenti) {
                ret += "- " + c.getAuthor() + ": " + c.getContent() + "\n";
            }
        }
        return ret;
    }

    public ArrayList<WinSomeUser> getUsersWithSimilarTags(Set<String> tags_list) {
        ArrayList<WinSomeUser> usersWithTag = new ArrayList<>();
        ArrayList<WinSomeUser> ret;

        for(String t : tags_list) {
            ret = getUsersByTag(t);
            for(WinSomeUser u : ret) {
                if(!usersWithTag.contains(u)) {
                    usersWithTag.add(u);
                }
            }
        }

        return usersWithTag;
    }

    public ArrayList<WinSomeUser> getUsersByTag(String tag) {
        ArrayList<WinSomeUser> ret = new ArrayList<>();
        for(WinSomeUser u : userList.values()) {
            if(u.getTags_list().contains(tag)) {
                ret.add(u);
            }
        }

        return ret;
    }

    public ArrayList<WinSomePost> getUserPosts(String username) {
        ArrayList<WinSomePost> ret = new ArrayList<>();

        for (WinSomePost p : postList.values()) {
            if(p.getAuthor().equals(username)) {
                ret.add(p);
            }
        }

        return ret;
    }

    public ArrayList<WinSomePost> getBlog(String username) {
        ArrayList<WinSomePost> blog = new ArrayList<>();

        for (WinSomePost p : postList.values()) {
            if(p.getAuthor().equals(username) || p.isRewinUserPresent(username)) {
                blog.add(p);
            }
        }

        return blog;
    }

    public ArrayList<WinSomePost> getFeed(String username) {
        ArrayList<String> usersFollowedByUser = getFollowing(username);
        ArrayList<WinSomePost> feed = new ArrayList<>();

        for(String u : usersFollowedByUser) {
            ArrayList<WinSomePost> p = getBlog(u);
            if(p != null) feed.addAll(p);
        }

        return feed;
    }

    boolean isPostInFeed(int post_id, String username) {
        ArrayList<WinSomePost> user_feed = getFeed(username);
        for(WinSomePost p : user_feed) {
            if(p.getPostID() == post_id) {
                return true;
            }
        }
        return false;
    }

    public void rewinPost(int post_id, String username) throws InvalidOperationException, NotInFeedException, PostNotFoundException, UserNotFoundException, SameAuthorException {
        if(!userList.containsKey(username)) throw new UserNotFoundException();
        WinSomePost toRewin = postList.get(post_id);
        if(toRewin == null) throw new PostNotFoundException();
        if(!isPostInFeed(post_id, username)) throw new NotInFeedException();
        if(toRewin.getAuthor().equals(username)) throw new SameAuthorException();
        if(!toRewin.addRewin(username)) throw new InvalidOperationException();
    }

    public void setPostList(ConcurrentHashMap<Integer, WinSomePost> postList) {
        this.postList = postList;
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

    public ConcurrentHashMap<Integer, WinSomePost> getPostList() {
        return postList;
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
        ArrayList<String> ret = followersList.get(username);
        if(ret == null) return new ArrayList<>();
        return ret;
    }

    public ArrayList<String> getFollowing(String username) {
        ArrayList<String> ret = followingList.get(username);
        if(ret == null) return new ArrayList<>();
        return ret;
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

    public WinSomeWallet getWalletByUsername(String username) {
        walletList.putIfAbsent(username, new WinSomeWallet(username));
        return walletList.get(username);
    }

    public ConcurrentHashMap<String, WinSomeWallet> getWalletList() {
        return walletList;
    }

    public void setWalletList(ConcurrentHashMap<String, WinSomeWallet> walletList) {
        this.walletList = walletList;
    }

    public int getFollowerCount(String username) {
        return followersList.get(username).size();
    }

    public int getFollowingCount(String username) {
        return followingList.get(username).size();
    }

    public String getFormattedCurrency(double input) {
        String output = String.format("%." + decimal_places + "f", Utils.roundC(input, decimal_places));

        if(input == 1) {
            return output+" "+currency_singular;
        } else {
            return output+" "+currency_plural;
        }
    }

    public String getFormattedValue(double input) {
        return String.format("%." + decimal_places + "f", Utils.roundC(input, decimal_places));
    }

    public boolean checkUserPassword(WinSomeUser u, String passwordToVerify) {
        if(u == null) return false;

        return PasswordManager.checkPSW(passwordToVerify, u.getSavedPassword());
    }

    /**
     * Permette di salvare il dato l'id dell'ultimo post creato in memoria persistente. Si è preferito tenere
     * questo metodo nella classe SocialManager invece che nel main per cercare di tenere più separati possibili
     * i compiti delle singole classi.
     */
    public void savePersistentData() {
        config.forceSavePreference("last_post_id", String.valueOf(last_post_id.intValue()));
    }
}
