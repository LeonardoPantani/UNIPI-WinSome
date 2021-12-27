/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server;

import it.pantani.winsome.server.entities.*;
import it.pantani.winsome.server.exceptions.*;
import it.pantani.winsome.rmi.WinSomeCallback;
import it.pantani.winsome.server.utils.ConfigManager;
import it.pantani.winsome.server.utils.PostComparator;
import it.pantani.winsome.other.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static it.pantani.winsome.other.Utils.getFormattedDate;

public class ConnectionHandler implements Runnable {
    private final Socket clientSocket;
    private WinSomeSession clientSession;
    private final int chCode;

    private PrintWriter out = null;
    private BufferedReader in = null;

    public ConnectionHandler(Socket clientSocket, int chCode) {
        this.clientSocket = clientSocket;
        this.clientSession = null;
        this.chCode = chCode;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch(IOException e) {
            e.printStackTrace();
        }

        if(out == null || in == null) {
            System.err.println("[!] Errore durante instaurazione connessione.");
            return;
        } else {
            System.out.println("[CH #" + chCode + "]> Sto gestendo il socket: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
        }

        String raw_request;
        while(true) {
            try {
                raw_request = in.readLine();
                if(raw_request != null) {
                    System.out.println("[" + clientSocket.getInetAddress() + ":" + clientSocket.getPort() + "]> " + raw_request);

                    String[] temp = raw_request.split(" ");
                    String request = temp[0];
                    String[] arguments = new String[temp.length-1];
                    System.arraycopy(temp, 1, arguments, 0, temp.length - 1);

                    switch (request) {
                        case "login": {
                            if (arguments.length != 2) {
                                out.println("Comando errato, usa: login <username> <password>");
                                break;
                            }
                            login(arguments[0], arguments[1]);
                            break;
                        }
                        case "logout": {
                            if(clientSession == null) {
                                out.println("non hai effettuato il login");
                                break;
                            }
                            logout(clientSession.getUsername());
                            break;
                        }
                        case "listusers": {
                            listusers();
                            break;
                        }
                        case "listfollowing": {
                            listfollowing();
                            break;
                        }
                        case "follow": {
                            if (arguments.length != 1) {
                                out.println("Comando errato, usa: follow <username>");
                                break;
                            }
                            follow(arguments[0]);
                            break;
                        }
                        case "unfollow": {
                            if (arguments.length != 1) {
                                out.println("Comando errato, usa: unfollow <username>");
                                break;
                            }
                            unfollow(arguments[0]);
                            break;
                        }
                        case "post": {
                            String req_body = raw_request.substring(5);
                            String[] text = req_body.split("\\|");
                            if(text.length != 2) {
                                out.println("Comando errato, usa: post <titolo>|<contenuto>");
                                break;
                            }
                            post(text[0], text[1]);
                            break;
                        }
                        case "blog": {
                            blog();
                            break;
                        }
                        case "rewin": {
                            if(arguments.length != 1) {
                                out.println("Comando errato, usa: rewin <id post>");
                                break;
                            }
                            try {
                                rewinPost(Integer.parseInt(arguments[0]));
                            } catch(NumberFormatException e) {
                                out.println("Comando errato, usa: rewin <id post>");
                            }
                            break;
                        }
                        case "rate": {
                            if(arguments.length != 2) {
                                out.println("Comando errato, usa: rate <id post> <+1/-1>");
                                break;
                            }
                            try {
                                rate(Integer.parseInt(arguments[0]), Integer.parseInt(arguments[1]));
                            } catch(NumberFormatException e) {
                                out.println("Comando errato, usa: rate <id post> <+1/-1>");
                            }
                            break;
                        }
                        case "showfeed": {
                            showfeed();
                            break;
                        }
                        case "showpost": {
                            if(arguments.length != 1) {
                                System.err.println("Comando errato, usa: showpost <id post>");
                                break;
                            }
                            try {
                                showpost(Integer.parseInt(arguments[0]));
                            } catch(NumberFormatException e) {
                                out.println("Comando errato, usa: showpost <id post>");
                            }
                            break;
                        }
                        case "comment": {
                            if(arguments.length < 2) {
                                System.err.println("Comando errato, usa: comment <id post> <testo>");
                                break;
                            }
                            // ottenimento commento
                            StringBuilder comment = new StringBuilder();
                            for(int i = 2; i < temp.length; i++) {
                                comment.append(temp[i]).append(" ");
                            }
                            comment.deleteCharAt(comment.length()-1);
                            // aggiunta del commento
                            try {
                                addComment(Integer.parseInt(arguments[0]), comment.toString());
                            } catch(NumberFormatException e) {
                                out.println("Comando errato, usa: comment <id post> <testo>");
                            }
                            break;
                        }
                        case "delete": {
                            if(arguments.length != 1) {
                                System.err.println("Comando errato, usa: delete <id post>");
                                break;
                            }
                            try {
                                deletePost(Integer.parseInt(arguments[0]));
                            } catch(NumberFormatException e) {
                                out.println("Comando errato, usa: delete <id post>");
                            }
                            break;
                        }
                        case "wallet": {
                            getWallet();
                            break;
                        }
                        case "walletbtc": {
                            getWalletInBitcoin();
                            break;
                        }
                        default: {
                            invalidcmd();
                            break;
                        }
                    }
                } else {
                    break;
                }
            } catch (IOException e) {
                break;
            }
        }
        ServerMain.listaSocket.remove(clientSocket);
        if(clientSession != null) ServerMain.listaSessioni.remove(clientSession.getUsername());
        System.out.println("[CH #" + chCode + "]> Collegamento col client " + clientSocket.getInetAddress() + ":" + clientSocket.getPort() + " terminato.");
    }

    private void invalidcmd() {
        out.println("comando non riconosciuto");
    }

    private void login(String username, String password) {
        if(clientSession != null) {
            out.println("sei gia' collegato con l'account '" + clientSession.getUsername() + "'");
            return;
        }
        SocialManager s = ServerMain.social;
        username = username.toLowerCase();
        WinSomeUser u = s.getUser(username);

        if(u != null) {
            if(s.checkUserPassword(u, password)) {
                WinSomeSession wss = getSession(username);
                if(wss != null) {
                    if(wss.getSessionSocket() == clientSocket) {
                        out.println("hai gia' fatto il login in data " + getFormattedDate(wss.getTimestamp()));
                    } else {
                        out.println("questo utente e' collegato e ha fatto il login in data " + getFormattedDate(wss.getTimestamp()));
                    }
                    return;
                }
                wss = new WinSomeSession(clientSocket, username);
                ServerMain.listaSessioni.put(username, wss);
                clientSession = wss;

                out.println(Utils.SOCIAL_LOGIN_SUCCESS);
            } else {
                out.println("password errata");
            }
        } else {
            out.println("utente non trovato");
        }
    }

    private void logout(String username) {
        SocialManager s = ServerMain.social;
        username = username.toLowerCase();
        WinSomeUser u = s.getUser(username);

        if(u != null) {
            if(getSession(username) != null) {
                WinSomeSession wss = ServerMain.listaSessioni.get(username);
                if (wss.getSessionSocket() == clientSocket) { // deve corrispondere il socket della sessione
                    ServerMain.listaSessioni.remove(username);
                    clientSession = null;
                    out.println(Utils.SOCIAL_LOGOUT_SUCCESS);
                } else {
                    out.println("non hai effettuato il login"); // se un altro client prova a fare logout
                }
            } else {
                out.println("non hai effettuato il login");
            }
        } else {
            out.println("utente non trovato");
        }
    }

    private void listusers() {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        Set<String> current_user_tags = s.getUser(current_user).getTags_list();
        if(current_user_tags.size() == 0) {
            out.println("non hai tag impostati, quindi non ci sono utenti con tag in comune con te");
            return;
        }

        // ottengo gli utenti con almeno un tag uguale e rimuovo me stesso
        ArrayList<WinSomeUser> usersWithTag = s.getUsersWithSimilarTags(current_user_tags);
        usersWithTag.removeIf(u -> Objects.equals(u.getUsername(), current_user));
        if(usersWithTag.size() == 0) { // se dopo la rimozione ci sono 0 utenti allora restituisco questo errore
            out.println("nessun utente ha almeno un tag in comune con te :(");
            return;
        }
        StringBuilder output = new StringBuilder();
        output.append("UTENTI CON ALMENO UN TAG IN COMUNE CON TE:\n");
        for(WinSomeUser u : usersWithTag) {
            output.append("* ").append(u.getUsername()).append(" | Tag in comune: ");
            for(String ut : u.getTags_list()) {
                if(current_user_tags.contains(ut)) {
                    output.append(ut).append(" ");
                }
            }
            output.deleteCharAt(output.length()-1);
            output.append("\n");
        }
        Utils.send(out, output.toString());
    }

    private void listfollowing() {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;

        ArrayList<String> following = s.getFollowing(clientSession.getUsername());
        if(following.size() == 0) {
            out.println("non segui alcun utente");
            return;
        }

        StringBuilder output = new StringBuilder();
        output.append("UTENTI CHE SEGUI:\n");
        for(String u : following) {
            output.append("* ").append(u).append("\n");
        }
        Utils.send(out, output.toString());
    }

    private void follow(String username) {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        username = username.toLowerCase();
        String current_user = clientSession.getUsername();

        if(current_user.equalsIgnoreCase(username)) {
            out.println("non puoi seguire te stesso");
            return;
        }
        if(!s.findUser(username)) {
            out.println("quell'utente non esiste");
            return;
        }
        ArrayList<String> user_followers = s.getFollowers(username);
        if(user_followers.contains(current_user)) {
            out.println("segui gia' quell'utente");
            return;
        }

        s.addFollowing(current_user, username);
        s.addFollower(username, current_user);
        out.println("ora segui '" + username + "'");
        try {
            WinSomeCallback.notifyFollowerUpdate(username, "+" + current_user);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void unfollow(String username) {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        username = username.toLowerCase();
        String current_user = clientSession.getUsername();

        if(current_user.equalsIgnoreCase(username)) {
            out.println("non puoi smettere di seguire te stesso");
            return;
        }
        if(!s.findUser(username)) {
            out.println("quell'utente non esiste");
            return;
        }
        ArrayList<String> user_followers = s.getFollowers(username);
        if(!user_followers.contains(current_user)) {
            out.println("non segui quell'utente");
            return;
        }

        s.removeFollowing(current_user, username);
        s.removeFollower(username, current_user);
        out.println("non segui piu' '" + username + "'");
        try {
            WinSomeCallback.notifyFollowerUpdate(username, "-" + current_user);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void post(String post_title, String post_content) {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        try {
            int new_post_id = s.createPost(current_user, post_title, post_content);
            out.println("post pubblicato (#" + new_post_id + ")");
        } catch(InvalidOperationException e) {
            out.println("titolo o contenuto del post troppo lungo");
        } catch(UserNotFoundException e) {
            out.println("utente non trovato");
        }
    }

    private void blog() {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        ArrayList<WinSomePost> user_posts = s.getBlog(current_user);
        if(user_posts.size() == 0) {
            out.println("il tuo blog e' vuoto, pubblica qualcosa!");
            return;
        }
        StringBuilder ret = new StringBuilder("BLOG DI " + current_user + ":\n");
        user_posts.sort(new PostComparator().reversed()); // ordino per data decrescente i post nel blog
        for(WinSomePost p : user_posts) {
            ret.append(s.getPostFormatted(p.getPostID(), true));
        }
        Utils.send(out, ret.toString());
    }

    private void rewinPost(int post_id) {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        try {
            s.rewinPost(post_id, current_user);
            out.println("rewin del post #" + post_id + " effettuato!");
        } catch(InvalidOperationException e) {
            out.println("hai gia' fatto il rewin di questo post");
        } catch(NotInFeedException e) {
            out.println("questo post non e' nel tuo feed");
        } catch(PostNotFoundException e) {
            out.println("impossibile trovare post con id #" + post_id);
        } catch(SameAuthorException e) {
            out.println("non puoi fare il rewin su un tuo post");
        } catch(UserNotFoundException e) {
            e.printStackTrace();
            out.println("errore interno");
        }
    }

    private void rate(int post_id, int vote) {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        try {
            s.ratePost(current_user, post_id, vote);
            if(vote == 1) {
                out.println("hai messo +1 al post #" + post_id);
            } else {
                out.println("hai messo -1 al post #" + post_id);
            }
        } catch(UserNotFoundException e) {
            e.printStackTrace();
            out.println("errore interno");
        } catch(InvalidVoteException e) {
            out.println("voto non valido");
        } catch(PostNotFoundException e) {
            out.println("impossibile trovare post con id #" + post_id);
        } catch(InvalidOperationException e) {
            out.println("hai gia' votato questo post");
        } catch(SameAuthorException e) {
            out.println("non puoi votare un tuo stesso post");
        } catch(NotInFeedException e) {
            out.println("questo post non e' nel tuo feed");
        }
    }

    private void showfeed() {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        ArrayList<WinSomePost> feed = s.getFeed(current_user);
        if(feed.size() == 0) {
            out.println("feed vuoto");
            return;
        }

        StringBuilder output = new StringBuilder();
        output.append("FEED DI ").append(current_user).append(":\n");
        for(WinSomePost p : feed) {
            output.append(s.getPostFormatted(p.getPostID(), false));
        }
        Utils.send(out, output.toString());
    }

    private void showpost(int post_id) {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        WinSomePost p = s.getPost(post_id);

        if(p == null) {
            out.println("impossibile trovare post con id #" + post_id);
            return;
        }
        Utils.send(out, s.getPostFormatted(p.getPostID(), true));
    }

    private void addComment(int post_id, String text) {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        try {
            s.commentPost(current_user, post_id, text);
            out.println("commento pubblicato");
        } catch(PostNotFoundException e) {
            out.println("impossibile trovare post con id #" + post_id);
        } catch(SameAuthorException e) {
            out.println("non puoi commentare sotto un tuo stesso post");
        } catch(NotInFeedException e) {
            out.println("questo post non e' nel tuo feed");
        }
    }

    private void deletePost(int post_id) {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        try {
            s.deletePost(current_user, post_id);
            out.println("post #" + post_id + " eliminato");
        } catch(PostNotFoundException e) {
            out.println("impossibile trovare post con id #" + post_id);
        } catch(InvalidOperationException e) {
            out.println("non puoi eliminare un post che non e' tuo");
        }
    }

    private void getWallet() {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        ConfigManager c = ServerMain.config;
        int precision = Integer.parseInt(c.getPreference("currency_decimal_places"));
        String current_user = clientSession.getUsername();

        WinSomeWallet user_wallet = s.getWalletByUsername(current_user);
        ConcurrentLinkedQueue<WinSomeTransaction> user_transactions = user_wallet.getTransactions();

        StringBuilder output = new StringBuilder("WALLET DI " + current_user + ":\n");
        double money;
        output.append("Bilancio: ").append(s.getFormattedCurrency(user_wallet.getBalance()));
        output.append("\n");
        if(user_transactions.size() != 0) {
            output.append("TRANSAZIONI:\n");
            for(WinSomeTransaction t : user_transactions) {
                output.append("* ");
                money = t.getEdit();
                if(money >= 0) output.append("+").append(s.getFormattedCurrency(money));
                else output.append(s.getFormattedCurrency(money));
                output.append(" | Motivo: ").append(t.getReason());
                output.append(" | Data: ").append(getFormattedDate(t.getDate())).append("\n");
            }
        }

        Utils.send(out, output.toString());
    }

    private void getWalletInBitcoin() {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        ConfigManager c = ServerMain.config;
        int precision = Integer.parseInt(c.getPreference("currency_decimal_places"));
        String current_user = clientSession.getUsername();

        WinSomeWallet user_wallet = s.getWalletByUsername(current_user);
        ConcurrentLinkedQueue<WinSomeTransaction> user_transactions = user_wallet.getTransactions();

        long calcTimeStart = System.currentTimeMillis();
        double conversionRate = Utils.generateRandomValue();
        System.out.println("[CH #" + chCode + "]> Rateo di conversione (" + conversionRate + ") ottenuto in " + (System.currentTimeMillis() - calcTimeStart) + "ms, trasmissione.");
        double money = user_wallet.getBalance();
        double moneyInBitcoin = money * conversionRate;

        String output = "WALLET DI " + current_user + ":\n";
        output += "Bilancio: " + s.getFormattedCurrency(user_wallet.getBalance());
        output += "\n";
        output += "Tasso di conversione: " + s.getFormattedValue(conversionRate) + " (aggiornato al " + getFormattedDate(calcTimeStart) + ")\n";
        output += "Bilancio in Bitcoin: " + s.getFormattedValue(moneyInBitcoin) + "\n";

        Utils.send(out, output);
    }

    public static WinSomeSession getSession(String username) {
        return ServerMain.listaSessioni.get(username);
    }

    public boolean isLogged() {
        return clientSession != null;
    }

    public static WinSomeSession getSessionBySocket(Socket socket) {
        for (WinSomeSession wss : ServerMain.listaSessioni.values()) {
            if(wss.getSessionSocket() == socket) {
                return wss;
            }
        }

        return null;
    }
}
