/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server;

import it.pantani.winsome.server.entities.*;
import it.pantani.winsome.server.exceptions.*;
import it.pantani.winsome.server.rmi.WinSomeCallback;
import it.pantani.winsome.shared.ConfigManager;
import it.pantani.winsome.shared.Utils;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pantani.winsome.server.utils.RandomGenerator.generateRandomValue;
import static it.pantani.winsome.shared.Utils.getFormattedDate;

/**
 * Una delle classi principali che si occupa di gestire la connessione con un client. Quello che fa in breve è ricevere
 * un clientSocket da gestire, ascolta le richieste in arrivo dal client connesso a quel socket, le elabora e genera una
 * risposta appropriata.
 *
 * clientSocket -> il socket da gestire ricevuto dal main
 * clientSession -> contiene informazioni sulla sessione dell'utente attualmente connesso (inizialmente null)
 * chCode -> codice del ConnectionHandler, usato nelle stampe di alcuni messaggi
 */
public class ConnectionHandler implements Runnable {
    private final Socket clientSocket;
    private WinSomeSession clientSession;
    private final int chCode;

    private PrintWriter out = null;
    private BufferedReader in = null;

    /**
     * Costruttore della classe ConnectionHandler. Si occupa di inizializzare i valori clientSocket, clientSession e chCode.
     * @param clientSocket socket del client a cui è stata accetta la connessione nel main
     * @param chCode codice assegnato nel main a questo ConnectionHandler
     */
    public ConnectionHandler(Socket clientSocket, int chCode) {
        this.clientSocket = clientSocket;
        this.clientSession = null;
        this.chCode = chCode;
    }

    /**
     * Metodo eseguito dal threadpool.
     */
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch(IOException e) {
            e.printStackTrace();
        }

        // se gli stream di input e output sono nulli dò errore subito e termino
        if(out == null || in == null) {
            System.err.println("[CH #" + chCode + "]> Errore durante instaurazione connessione.");
            return;
        } else {
            System.out.println("[CH #" + chCode + "]> Sto gestendo il socket: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
        }

        String raw_request;
        while(true) {
            try {
                // leggo la richiesta del client
                raw_request = Utils.receive(in);
                if(raw_request != null) { // se è valida allora mi preparo a gestirla
                    // divido la richiesta in operazione e argomenti
                    String[] temp = raw_request.split(" ");
                    String request = temp[0];
                    String[] arguments = new String[temp.length-1];
                    System.arraycopy(temp, 1, arguments, 0, temp.length - 1);

                    // stampo la richiesta in console (se è una richiesta di login non mostro la password)
                    System.out.print("[" + clientSocket.getInetAddress() + ":" + clientSocket.getPort() + "]> ");
                    if(arguments.length == 2 && request.equals("login")) {
                        System.out.println(request + " " + arguments[0] + " ******");
                    } else {
                        System.out.println(raw_request);
                    }

                    // gestisco la richiesta in maniera appropriata
                    switch (request) {
                        case "login": {
                            if (arguments.length != 2) {
                                Utils.send(out, "comando errato, usa: login <username> <password>");
                                break;
                            }
                            login(arguments[0], arguments[1]);
                            break;
                        }
                        case "logout": {
                            if(clientSession == null) {
                                Utils.send(out, "non hai effettuato il login");
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
                                Utils.send(out, "comando errato, usa: follow <username>");
                                break;
                            }
                            follow(arguments[0]);
                            break;
                        }
                        case "unfollow": {
                            if (arguments.length != 1) {
                                Utils.send(out, "comando errato, usa: unfollow <username>");
                                break;
                            }
                            unfollow(arguments[0]);
                            break;
                        }
                        case "post": {
                            // se il numero di parentesi non è pari allora restituisco un errore
                            if(raw_request.chars().filter(ch -> ch == '"').count() % 2 != 0) {
                                Utils.send(out, "comando errato, usa: post \"<titolo>\" \"<contenuto>\"");
                                break;
                            }

                            // ottengo i valori tra parentesi
                            Pattern p = Pattern.compile("\"([^\"]*)\"");
                            Matcher m = p.matcher(raw_request);
                            ArrayList<String> text = new ArrayList<>();
                            while(m.find()) {
                                text.add(m.group(1));
                            }

                            // se i parametri forniti non sono nè 1 nè 2 allora restituisco errore
                            if(text.size() != 1 && text.size() != 2) {
                                Utils.send(out, "comando errato, usa: post \"<titolo>\" \"<contenuto>\"");
                                break;
                            }

                            // il titolo del post non deve essere vuoto
                            if(text.get(0).trim().length() == 0) {
                                Utils.send(out, "comando errato, il titolo del post non puo' essere vuoto");
                                break;
                            }

                            // scelta progettuale: il post deve avere sia titolo che contenuto obbligatoriamente
                            post(text);
                            break;
                        }
                        case "blog": {
                            blog();
                            break;
                        }
                        case "rewin": {
                            if(arguments.length != 1) {
                                Utils.send(out, "comando errato, usa: rewin <id post>");
                                break;
                            }
                            try {
                                rewinPost(Integer.parseInt(arguments[0]));
                            } catch(NumberFormatException e) {
                                Utils.send(out, "comando errato, usa: rewin <id post>");
                            }
                            break;
                        }
                        case "rate": {
                            if(arguments.length != 2) {
                                Utils.send(out, "comando errato, usa: rate <id post> <+1/-1>");
                                break;
                            }
                            try {
                                rate(Integer.parseInt(arguments[0]), Integer.parseInt(arguments[1]));
                            } catch(NumberFormatException e) {
                                Utils.send(out, "comando errato, usa: rate <id post> <+1/-1>");
                            }
                            break;
                        }
                        case "showfeed": {
                            showfeed();
                            break;
                        }
                        case "showpost": {
                            if(arguments.length != 1) {
                                Utils.send(out, "comando errato, usa: showpost <id post>");
                                break;
                            }
                            try {
                                showpost(Integer.parseInt(arguments[0]));
                            } catch(NumberFormatException e) {
                                Utils.send(out, "comando errato, usa: showpost <id post>");
                            }
                            break;
                        }
                        case "comment": {
                            if(arguments.length < 2) {
                                Utils.send(out, "comando errato, usa: comment <id post> <testo>");
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
                                Utils.send(out, "comando errato, usa: comment <id post> <testo>");
                            }
                            break;
                        }
                        case "delete": {
                            if(arguments.length != 1) {
                                Utils.send(out, "comando errato, usa: delete <id post>");
                                break;
                            }
                            try {
                                deletePost(Integer.parseInt(arguments[0]));
                            } catch(NumberFormatException e) {
                                Utils.send(out, "comando errato, usa: delete <id post>");
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

        // se esco dal while vuol dire che la connessione è terminata (con una IOException)
        ServerMain.socketsList.remove(clientSocket); // rimuovo il socket del client dalla lista
        if(clientSession != null) ServerMain.sessionsList.remove(clientSession.getUsername()); // se l'utente era loggato, ne rimuovo la sessione
        System.out.println("[CH #" + chCode + "]> Collegamento col client " + clientSocket.getInetAddress() + ":" + clientSocket.getPort() + " terminato.");
    }

    /**
     * Eseguito se il server non conosce come gestire la richiesta del client.
     */
    private void invalidcmd() {
        Utils.send(out, "comando non riconosciuto");
    }

    /**
     * Se l'username esiste, la password corrisponde e non esiste già una sessione attiva allora genero una nuova
     * sessione da aggiungere alla lista delle sessioni attive e invio il messaggio di conferma di login al client.
     * @param username username dell'utente da loggare
     * @param password password in chiaro dell'utente da loggare
     */
    private void login(String username, String password) {
        if(clientSession != null) {
            Utils.send(out, "sei gia' collegato con l'account '" + clientSession.getUsername() + "'");
            return;
        }
        SocialManager s = ServerMain.social;
        username = username.toLowerCase();
        WinSomeUser u = s.getUser(username);

        if(u != null) {
            if(s.checkUserPassword(u, password)) {
                WinSomeSession wss = ServerMain.sessionsList.get(username);
                if(wss != null) {
                    if(wss.getSessionSocket() == clientSocket) {
                        Utils.send(out, "hai gia' fatto il login in data " + getFormattedDate(wss.getTimestamp()));
                    } else {
                        Utils.send(out, "questo utente e' collegato e ha fatto il login in data " + getFormattedDate(wss.getTimestamp()));
                    }
                    return;
                }
                wss = new WinSomeSession(clientSocket, username);
                ServerMain.sessionsList.put(username, wss);
                clientSession = wss;

                Utils.send(out, Utils.SOCIAL_LOGIN_SUCCESS);
            } else {
                Utils.send(out, "password errata");
            }
        } else {
            Utils.send(out, "utente non trovato");
        }
    }

    /**
     * Se l'utente esiste, la sessione è attiva e il socket del client che manda il logout è uguale a quello della
     * sessione che vuole terminare allora rimuovo la sessione del client e invio il messaggio di conferma al client.
     * @param username l'username di cui fare il logout
     */
    private void logout(String username) {
        SocialManager s = ServerMain.social;
        username = username.toLowerCase();
        WinSomeUser u = s.getUser(username);

        if(u != null) {
            if(ServerMain.sessionsList.get(username) != null) {
                WinSomeSession wss = ServerMain.sessionsList.get(username);
                if (wss.getSessionSocket() == clientSocket) { // deve corrispondere il socket della sessione
                    ServerMain.sessionsList.remove(username);
                    clientSession = null;
                    Utils.send(out, Utils.SOCIAL_LOGOUT_SUCCESS);
                } else {
                    Utils.send(out, "non hai effettuato il login"); // se un altro client prova a fare logout
                }
            } else {
                Utils.send(out, "non hai effettuato il login");
            }
        } else {
            Utils.send(out, "utente non trovato");
        }
    }

    /**
     * Mostra all'utente una lista di utenti che hanno almeno un tag in comune con lui. Se l'utente, in fase di
     * registrazione, non ha fornito nessun tag allora invio subito un messaggio di errore. Altrimenti stampo la lista.
     */
    private void listusers() {
        if(isNotLogged()) {
            Utils.send(out, "non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        Set<String> current_user_tags = s.getUser(current_user).getTags_list();
        if(current_user_tags.size() == 0) {
            Utils.send(out, "non hai tag impostati, quindi non ci sono utenti con tag in comune con te");
            return;
        }

        // ottengo gli utenti con almeno un tag uguale e rimuovo me stesso
        ArrayList<WinSomeUser> usersWithTag = s.getUsersWithSimilarTags(current_user_tags);
        usersWithTag.removeIf(u -> Objects.equals(u.getUsername(), current_user));
        if(usersWithTag.size() == 0) { // se dopo la rimozione ci sono 0 utenti allora restituisco questo errore
            Utils.send(out, "nessun utente ha almeno un tag in comune con te :(");
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

    /**
     * Mostra all'utente la lista di utenti che segue. Se questo non ne segue nessuno stampa subito un errore,
     * altrimenti stampa la lista.
     */
    private void listfollowing() {
        if(isNotLogged()) {
            Utils.send(out, "non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;

        ArrayList<String> following = s.getFollowing(clientSession.getUsername());
        if(following.size() == 0) {
            Utils.send(out, "non segui alcun utente");
            return;
        }

        StringBuilder output = new StringBuilder();
        output.append("UTENTI CHE SEGUI:\n");
        for(String u : following) {
            output.append("* ").append(u).append("\n");
        }
        Utils.send(out, output.toString());
    }

    /**
     * Fa seguire all'utente attualmente loggato l'utente username. Se l'operazione ha successo, notifico username
     * che ha ora un nuovo follower
     * @param username l'utente che avrà un nuovo follower
     */
    private void follow(String username) {
        if(isNotLogged()) {
            Utils.send(out, "non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        username = username.toLowerCase();
        String current_user = clientSession.getUsername();

        try {
            s.followUser(current_user, username);
            Utils.send(out, "ora segui '" + username + "'");
            try {
                WinSomeCallback.notifyFollowerUpdate(username, "+" + current_user);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } catch(UserNotFoundException e) {
            Utils.send(out, "quell'utente non esiste");
        } catch(SameUserException e) {
            Utils.send(out, "non puoi seguire te stesso");
        } catch(InvalidOperationException e) {
            Utils.send(out, "segui gia' quell'utente");
        }
    }

    /**
     * Fa smettere di seguire username all'utente attualmente connesso. Se l'operazione ha successo, notifico username
     * che ha perso un follower.
     * @param username l'utente che perderà un follower
     */
    private void unfollow(String username) {
        if(isNotLogged()) {
            Utils.send(out, "non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        username = username.toLowerCase();
        String current_user = clientSession.getUsername();

        try {
            s.unfollowUser(current_user, username);
            Utils.send(out, "non segui piu' '" + username + "'");
            try {
                WinSomeCallback.notifyFollowerUpdate(username, "-" + current_user);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } catch(UserNotFoundException e) {
            Utils.send(out, "quell'utente non esiste");
        } catch(SameUserException e) {
            Utils.send(out, "non puoi smettere di seguire te stesso");
        } catch(InvalidOperationException e) {
            Utils.send(out, "non segui quell'utente");
        }
    }

    /**
     * Fa inviare all'utente attualmente connesso un post con un titolo e un corpo.
     * @param arguments_list lista degli argomenti solo i primi due valori sono considerati e sono rispettivamente:
     *                       titolo del post                -> indice 0
     *                       contenuto del post (opzionale) -> indice 1
     */
    private void post(ArrayList<String> arguments_list) {
        if(isNotLogged()) {
            Utils.send(out, "non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        String post_title = arguments_list.get(0).trim();
        String post_content = "";

        if(arguments_list.size() >= 2) post_content = arguments_list.get(1).trim();

        try {
            int new_post_id = s.createPost(current_user, post_title, post_content);
            Utils.send(out, "post pubblicato (#" + new_post_id + ")");
        } catch(InvalidOperationException e) {
            Utils.send(out, "titolo o contenuto del post troppo lungo");
        } catch(UserNotFoundException e) {
            Utils.send(out, "utente non trovato");
        }
    }

    /**
     * Mostra all'utente la lista dei propri post e di quelli rewinnati da lui. Se il blog è vuoto è stampato subito
     * un errore.
     */
    private void blog() {
        if(isNotLogged()) {
            Utils.send(out, "non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        ArrayList<WinSomePost> user_posts = s.getBlog(current_user);
        if(user_posts.size() == 0) {
            Utils.send(out, "il tuo blog e' vuoto, pubblica qualcosa!");
            return;
        }
        StringBuilder ret = new StringBuilder("BLOG DI " + current_user + ":\n");
        for(WinSomePost p : user_posts) {
            ret.append(s.getPostFormatted(p.getPostID(), true, true, true, true, true, true));
        }
        Utils.send(out, ret.toString());
    }

    /**
     * Fa rewinnare all'utente attualmente connesso il post post_id. Questa operazione ha successo solo se: è nel suo feed,
     * non lo ha già rewinnato e non è dello stesso utente.
     * @param post_id l'id del post da rewinnare
     */
    private void rewinPost(int post_id) {
        if(isNotLogged()) {
            Utils.send(out, "non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        try {
            s.rewinPost(current_user, post_id);
            Utils.send(out, "rewin del post #" + post_id + " effettuato!");
        } catch(InvalidOperationException e) {
            Utils.send(out, "hai gia' fatto il rewin di questo post");
        } catch(NotInFeedException e) {
            Utils.send(out, "questo post non e' nel tuo feed");
        } catch(PostNotFoundException e) {
            Utils.send(out, "impossibile trovare post con id #" + post_id);
        } catch(SameUserException e) {
            Utils.send(out, "non puoi fare il rewin su un tuo post");
        } catch(UserNotFoundException e) {
            e.printStackTrace();
            Utils.send(out, "errore interno");
        }
    }

    /**
     * Fa valutare all'utente attualmente connesso il post post_id con voto vote. Questa operazione ha successo solo se:
     * il voto è +1/-1 (da specifica), non è già stato espresso, il post è nel feed dell'utente e non è dello stesso utente
     * che lo pubblica.
     * @param post_id l'id del post da votare
     * @param vote il valore del voto
     */
    private void rate(int post_id, int vote) {
        if(isNotLogged()) {
            Utils.send(out, "non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        try {
            s.ratePost(current_user, post_id, vote);
            if(vote == 1) {
                Utils.send(out, "hai messo +1 al post #" + post_id);
            } else {
                Utils.send(out, "hai messo -1 al post #" + post_id);
            }
        } catch(InvalidVoteException e) {
            Utils.send(out, "voto non valido");
        } catch(PostNotFoundException e) {
            Utils.send(out, "impossibile trovare post con id #" + post_id);
        } catch(InvalidOperationException e) {
            Utils.send(out, "hai gia' votato questo post");
        } catch(SameUserException e) {
            Utils.send(out, "non puoi votare un tuo stesso post");
        } catch(NotInFeedException e) {
            Utils.send(out, "questo post non e' nel tuo feed");
        } catch(UserNotFoundException e) {
            e.printStackTrace();
            Utils.send(out, "errore interno");
        }
    }

    /**
     * Mostra all'utente il proprio feed, composto dai post contenuti nei blog degli utenti che segue. Notare come nel
     * feed possano esserci anche propri post, se chi li ha nel proprio feed li ha rewinnati.
     */
    private void showfeed() {
        if(isNotLogged()) {
            Utils.send(out, "non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        ArrayList<WinSomePost> feed = s.getFeed(current_user);
        if(feed.size() == 0) {
            Utils.send(out, "feed vuoto");
            return;
        }

        StringBuilder output = new StringBuilder();
        output.append("FEED DI ").append(current_user.toUpperCase()).append(":\n");
        for(WinSomePost p : feed) {
            output.append(s.getPostFormatted(p.getPostID(), false, false, true, false, false, false));
        }
        Utils.send(out, output.toString());
    }

    /**
     * Mostra all'utente un post dato un id. Dà errore se il post non esiste.
     * @param post_id l'id del post da mostrare
     */
    private void showpost(int post_id) {
        if(isNotLogged()) {
            Utils.send(out, "non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        WinSomePost p = s.getPost(post_id);

        if(p == null) {
            Utils.send(out, "impossibile trovare post con id #" + post_id);
            return;
        }
        Utils.send(out, s.getPostFormatted(p.getPostID(), true, true, true, true, true, true));
    }

    /**
     * Fa aggiungere un commento al post post_id con contenuto text all'utente attualmente connesso. L'operazione ha successo
     * solo se il post è nel proprio feed e non è il proprio.
     * @param post_id l'id del post da commentare
     * @param text il testo del commento
     */
    private void addComment(int post_id, String text) {
        if(isNotLogged()) {
            Utils.send(out, "non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        try {
            s.commentPost(current_user, post_id, text);
            Utils.send(out, "commento pubblicato");
        } catch(PostNotFoundException e) {
            Utils.send(out, "impossibile trovare post con id #" + post_id);
        } catch(SameUserException e) {
            Utils.send(out, "non puoi commentare sotto un tuo stesso post");
        } catch(NotInFeedException e) {
            Utils.send(out, "questo post non e' nel tuo feed");
        }
    }

    /**
     * Fa cancellare il post post_id all'utente attualmente connesso. L'operazione ha successo solo se l'utente
     * che richiede la cancellazione è lo stesso che ha mandato quel post.
     * @param post_id l'id del post da cancellare
     */
    private void deletePost(int post_id) {
        if(isNotLogged()) {
            Utils.send(out, "non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        String current_user = clientSession.getUsername();

        try {
            s.deletePost(current_user, post_id);
            Utils.send(out, "post #" + post_id + " eliminato");
        } catch(PostNotFoundException e) {
            Utils.send(out, "impossibile trovare post con id #" + post_id);
        } catch(InvalidOperationException e) {
            Utils.send(out, "non puoi eliminare un post che non e' tuo");
        }
    }

    /**
     * Mostra all'utente il proprio wallet di WinSome, completo di bilancio e transazioni. Tutti i valori delle transazioni
     * hanno un numero di cifre decimali pari a quello specificato nel file di configurazione. Pertanto se la transazione
     * è molto piccola, è possibile che, scegliendo poche cifre decimali, la transazione sia di tipo +0,00 wincoins.
     */
    private void getWallet() {
        if(isNotLogged()) {
            Utils.send(out, "non hai effettuato il login");
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

    /**
     * Mostra all'utente il bilancio del proprio portafoglio WinSome, con il tasso di conversione attuale e il relativo
     * valore in Bitcoin. Il tasso di conversione è ottenuto casualmente dal servizio random.org
     */
    private void getWalletInBitcoin() {
        if(isNotLogged()) {
            Utils.send(out, "non hai effettuato il login");
            return;
        }
        SocialManager s = ServerMain.social;
        ConfigManager c = ServerMain.config;
        int precision = Integer.parseInt(c.getPreference("currency_decimal_places"));
        String current_user = clientSession.getUsername();

        WinSomeWallet user_wallet = s.getWalletByUsername(current_user);
        ConcurrentLinkedQueue<WinSomeTransaction> user_transactions = user_wallet.getTransactions();

        long calcTimeStart = System.currentTimeMillis();
        double conversionRate = generateRandomValue();
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

    // metodi per rendere più chiaro il codice
    /**
     * L'utente è collegato solo se clientSession non è null
     * @return true se l'utente è collegato, false altrimenti
     */
    public boolean isLogged() {
        return clientSession != null;
    }

    /**
     * L'utente è collegato solo se clientSession non è null
     * @return true se l'utente NON è collegato, false altrimenti
     */
    public boolean isNotLogged() {
        return clientSession == null;
    }
}
