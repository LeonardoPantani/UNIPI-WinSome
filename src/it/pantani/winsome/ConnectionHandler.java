/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome;

import it.pantani.winsome.entities.WinSomePost;
import it.pantani.winsome.entities.WinSomeSession;
import it.pantani.winsome.entities.WinSomeUser;
import it.pantani.winsome.exceptions.*;
import it.pantani.winsome.rmi.WinSomeCallback;
import it.pantani.winsome.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;

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
                        case "login" -> {
                            if (arguments.length != 2) {
                                out.println("Comando errato, usa: login <username> <password>");
                                break;
                            }
                            login(arguments[0], arguments[1]);
                        }
                        case "logout" -> {
                            if (arguments.length != 1) {
                                out.println("Comando errato, usa: logout <username>");
                                break;
                            }
                            logout(arguments[0]);
                        }
                        case "listfollowing" -> {
                            listfollowing();
                        }
                        case "follow" -> {
                            if (arguments.length != 1) {
                                out.println("Comando errato, usa: follow <username>");
                                break;
                            }
                            follow(arguments[0]);
                        }
                        case "unfollow" -> {
                            if (arguments.length != 1) {
                                out.println("Comando errato, usa: unfollow <username>");
                                break;
                            }
                            unfollow(arguments[0]);
                        }
                        case "post" -> {
                            String req_body = raw_request.substring(5);
                            String[] text = req_body.split("\\|");
                            if(text.length != 2) {
                                out.println("Comando errato, usa: post <titolo>|<contenuto>");
                                break;
                            }
                            post(text[0], text[1]);
                        }
                        case "blog" -> {
                            blog();
                        }
                        case "rate" -> {
                            if(arguments.length != 2) {
                                out.println("Comando errato, usa: rate <id post> <+1/-1>");
                                break;
                            }
                            try {
                                rate(Integer.parseInt(arguments[0]), Integer.parseInt(arguments[1]));
                            } catch(NumberFormatException e) {
                                out.println("Comando errato, usa: rate <id post> <+1/-1>");
                            }
                        }
                        default -> invalidcmd();
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
            if(u.checkPassword(password)) {
                WinSomeSession wss = getSession(username);
                if(wss != null) {
                    if(wss.getSessionSocket() == clientSocket) {
                        out.println("hai gia' fatto il login in data " + new Date(wss.getTimestamp()));
                    } else {
                        out.println("questo utente e' collegato e ha fatto il login in data " + new Date(wss.getTimestamp()));
                    }
                    return;
                }
                wss = new WinSomeSession(clientSocket, username);
                ServerMain.listaSessioni.put(username, wss);
                clientSession = wss;

                out.println("login ok");
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
                    out.println("logout effettuato");
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

    private void listfollowing() {
        if(!isLogged()) {
            out.println("non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;

        ArrayList<String> following = s.getFollowing(clientSession.getUsername());
        if(following == null) {
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
        if(user_followers != null && user_followers.contains(current_user)) {
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
        if(user_followers == null || !user_followers.contains(current_user)) {
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
        } catch(PostTitleTooLongException e) {
            out.println("titolo del post troppo lungo");
        } catch(PostContentTooLongException e) {
            out.println("contenuto del post troppo lungo");
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

        ArrayList<WinSomePost> user_posts = s.getUserPosts(current_user);
        if(user_posts == null) {
            out.println("il tuo blog e' vuoto, pubblica qualcosa!");
            return;
        }
        String ret = "BLOG DI " + current_user + ":\n";
        for(WinSomePost p : user_posts) {
            int up = p.getUpvotes(), down = p.getDownvotes();

            ret += "[Post #" + p.getPostID() + "]\n";
            ret += "Titolo: " + p.getPostTitle() + "\n";
            ret += "Contenuto: " + p.getPostContent() + "\n";
            ret += "Voti: " + up + " ";
            if(up == 1) ret += "positivo"; else ret += "positivi";
            ret += ", " + down + " ";
            if(down == 1) ret += "negativo"; else ret += "negativi";
            ret += "\n";
        }
        Utils.send(out, ret);
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
        } catch(InvalidVoteException e) {
            out.println("voto non valido");
        } catch(PostNotFoundException e) {
            out.println("impossibile trovare post con id #" + post_id);
        } catch(InvalidOperationException e) {
            out.println("hai gia' votato questo post");
        } catch(OwnPostVoteException e) {
            out.println("non puoi votare un tuo stesso post");
        }
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
