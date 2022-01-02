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
import it.pantani.winsome.shared.ConfigManager;
import it.pantani.winsome.server.utils.PasswordManager;
import it.pantani.winsome.shared.Utils;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static it.pantani.winsome.shared.Utils.getFormattedDate;

/**
 * Classe cuore del social WinSome. Implementa tutte le operazioni che il connectionhandler dovrà utilizzare per rispondere
 * alle richieste del client. Necessita del file di configurazione per caricare alcune delle impostazioni relative all'implementazione
 * del social, come l'id dell'ultimo post, i nomi della valuta, la lunghezza della parte dopo la virgola, la lunghezza dei
 * valori del singolo post eccetera.
 */
public class SocialManager {
    private final ConfigManager config;

    private ConcurrentHashMap<String, WinSomeUser> userList;
    private ConcurrentHashMap<String, ArrayList<String>> followersList;
    private ConcurrentHashMap<String, ArrayList<String>> followingList; // ridondanza
    private ConcurrentHashMap<Integer, WinSomePost> postList;
    private ConcurrentHashMap<String, WinSomeWallet> walletList;

    public AtomicInteger last_post_id;

    private String currency_name_singular;
    private String currency_name_plural;
    private int currency_decimal_places;

    private int post_max_title_length;
    private int post_max_content_length;

    /**
     * Questo costruttore inizializza tutte le strutture dati necessarie per il funzionamento del social WinSome.
     * Inoltre valida e salva le preferenze fornite nel file di configurazione, ma solo quelle relative al social, e quindi
     * non quelle relative al collegamento del client al server. Unico dettaglio di implementazione degno di nota è il fatto
     * che, per il tracciamento dei follower di un utente, si è preferito usare due liste differenti per ogni utente. Questo
     * serve per fare in modo che la ricerca di un utente nella lista richieda un tempo ridotto, al costo di più spazio
     * occupato su disco. Si può vedere anche come una specie di "backup" perché così se uno dei due file andasse perso,
     * dall'altro si riuscirebbe a recuperare tutte le informazioni necessarie.
     * @param config il file di configurazione
     * @throws ConfigurationException nel caso un parametro della configurazione fosse errato viene stampato un errore
     */
    public SocialManager(ConfigManager config) throws ConfigurationException {
        this.config = config;
        userList = new ConcurrentHashMap<>();
        followersList = new ConcurrentHashMap<>();
        followingList = new ConcurrentHashMap<>();
        walletList = new ConcurrentHashMap<>();
        postList = new ConcurrentHashMap<>();

        validateAndSavePreferences();
    }

    /**
     * Crea un post con autore l'utente username, come titolo post_title e post_content come contenuto
     * @param username username dell'utente
     * @param post_title titolo del post
     * @param post_content contenuto del post
     * @return l'id del nuovo post
     * @throws UserNotFoundException se l'utente che si passa come parametro non esiste
     * @throws InvalidOperationException se la lunghezza del titolo o del corpo sono maggiori dei limiti
     */
    public int createPost(String username, String post_title, String post_content) throws UserNotFoundException, InvalidOperationException {
        if(!userList.containsKey(username)) throw new UserNotFoundException();
        if(post_title.length() > post_max_title_length) throw new InvalidOperationException();
        if(post_content.length() > post_max_content_length) throw new InvalidOperationException();

        int idpost = last_post_id.getAndIncrement();
        postList.putIfAbsent(idpost, new WinSomePost(idpost, username, post_title, post_content));
        return idpost;
    }

    /**
     * Lascia un voto al post con post_id per conto di username
     * @param username l'username dell'utente
     * @param post_id l'id del post da votare
     * @param value valore del voto
     * @throws UserNotFoundException se l'utente fornito non esiste
     * @throws InvalidVoteException se il valore del voto non è valido
     * @throws PostNotFoundException se l'id del post non è valido
     * @throws InvalidOperationException se l'utente ha già votato a quel post
     * @throws SameUserException se il post da votare è dell'utente che sta votando
     * @throws NotInFeedException se il post da votare non è nel feed dell'utente
     */
    public void ratePost(String username, int post_id, int value) throws UserNotFoundException, InvalidVoteException, PostNotFoundException, InvalidOperationException, SameUserException, NotInFeedException {
        if(!userList.containsKey(username)) throw new UserNotFoundException();
        if(value != -1 && value != 1) throw new InvalidVoteException();
        WinSomePost toRate = postList.get(post_id);
        if(toRate == null) throw new PostNotFoundException();
        if(toRate.getAuthor().equals(username)) throw new SameUserException();
        if(isPostNotInFeed(post_id, username)) throw new NotInFeedException();
        if(toRate.findVoteByUser(username) != null) throw new InvalidOperationException();

        toRate.addVote(username, value);
    }

    /**
     * Lascia un commento ad un post con post_id per conto di username
     * @param username l'username dell'utente
     * @param post_id l'id del post da commentare
     * @param text il contenuto del commento
     * @throws PostNotFoundException se l'id del post non è valido
     * @throws SameUserException se il post da commentare è dell'utente che sta votando
     * @throws NotInFeedException se il post da commentare non è nel feed dell'utente
     */
    public void commentPost(String username, int post_id, String text) throws PostNotFoundException, SameUserException, NotInFeedException {
        WinSomePost toComment = postList.get(post_id);
        if(toComment == null) throw new PostNotFoundException();
        if(toComment.getAuthor().equals(username)) throw new SameUserException();
        if(isPostNotInFeed(post_id, username)) throw new NotInFeedException();

        toComment.addComment(username, text);
    }

    /**
     * Cancella il post post_id per conto di username
     * @param username l'username dell'utente
     * @param post_id l'id del post da commentare
     * @throws PostNotFoundException  se l'id del post non è valido
     * @throws InvalidOperationException se il post da eliminare non è stato mandato dall'utente
     */
    public void deletePost(String username, int post_id) throws PostNotFoundException, InvalidOperationException {
        WinSomePost toDelete = postList.get(post_id);
        if(toDelete == null) throw new PostNotFoundException();
        if(!toDelete.getAuthor().equals(username)) throw new InvalidOperationException();

        postList.remove(post_id);
    }

    /**
     * Restituisce l'oggetto WinSomePost con id post_id
     * @param post_id l'id del post richiesto
     * @return l'oggetto WinSomePost con quell'id, o null se non esiste
     */
    public WinSomePost getPost(int post_id) {
        return postList.get(post_id);
    }

    /**
     * Stampa un post sullo standard output formattato.
     * @param post_id l'id del post da stampare
     * @param showRewin se si vuole che sotto l'id del post appaia da quanti utenti è stato rewinnato il post
     * @param showContent se si vuole mostrare il contenuto del post
     * @param showAuthor se si vuole mostrare l'autore del post
     * @param showVotes se si vuole mostrare i voti del post
     * @param showCreationDate se si vuole mostrare la data di invio del post
     * @param showComments se si vuole mostrare i commenti del post
     * @return la stringa contenente il post formattato pronto per la stampa
     */
    public String getPostFormatted(int post_id, boolean showRewin, boolean showContent, boolean showAuthor, boolean showVotes, boolean showCreationDate, boolean showComments) {
        WinSomePost p = postList.get(post_id);
        StringBuilder ret;
        if(p == null) return null;
        ArrayList<WinSomeComment> comments_list = p.getCommentList();
        ConcurrentLinkedQueue<String> q = p.getRewinUsers();

        ret = new StringBuilder("[ Post #" + p.getPostID() + " ]\n");
        if(showRewin && q.size() != 0) { ret.append("* post rewinnato da ").append(q.size()).append(" "); if(q.size() == 1) { ret.append("utente"); } else { ret.append("utenti"); } ret.append(" *\n"); }
        ret.append("Titolo: ").append(p.getPostTitle()).append("\n");
        if(showContent) ret.append("Contenuto: ").append(p.getPostContent()).append("\n");
        if(showAuthor) ret.append("Autore: ").append(p.getAuthor()).append("\n");
        if(showVotes) {
            ret.append("Voti (").append(p.getTotalVotes()).append("): ").append(p.getNumVotesByValue(1)).append(" ");
            if(p.getNumVotesByValue(1) == 1) ret.append("positivo"); else ret.append("positivi");
            ret.append(", ").append(p.getNumVotesByValue(-1)).append(" ");
            if(p.getNumVotesByValue(-1) == 1) ret.append("negativo"); else ret.append("negativi");
            ret.append("\n");
        }
        if(showCreationDate) ret.append("Data: ").append(getFormattedDate(p.getDateSent())).append("\n");
        if(showComments && comments_list != null) {
            ret.append("Commenti (").append(comments_list.size()).append("):\n");
            for(WinSomeComment c : comments_list) {
                ret.append("- ").append(c.getAuthor()).append(": ").append(c.getContent()).append("\n");
            }
        }
        return ret.toString();
    }

    /**
     * Restituisce una lista di WinSomeUser che hanno almeno un tag in comune con tags_list
     * @param tags_list la lista di tags
     * @return lista di utenti che hanno almeno un tag in comune con tags_list
     */
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

    /**
     * Se il post con id post_id è nel feed dell'utente username restituisce vero
     * @param post_id l'id del post da verificare
     * @param username l'username di cui vedere se il post con id post_id è in quel feed
     * @return vero se il post post_id è nel feed di username, falso altrimenti
     */
    boolean isPostInFeed(int post_id, String username) {
        ArrayList<WinSomePost> user_feed = getFeed(username);
        for(WinSomePost p : user_feed) {
            if(p.getPostID() == post_id) {
                return true;
            }
        }
        return false;
    }

    /**
     * Se il post con id post_id NON è nel feed dell'utente username restituisce vero
     * @param post_id l'id del post da verificare
     * @param username l'username di cui vedere se il post con id post_id è in quel feed
     * @return vero se il post post_id NON è nel feed di username, falso altrimenti
     */
    boolean isPostNotInFeed(int post_id, String username) {
        ArrayList<WinSomePost> user_feed = getFeed(username);
        for(WinSomePost p : user_feed) {
            if(p.getPostID() == post_id) {
                return false;
            }
        }
        return true;
    }

    /**
     * Restituisce la lista di utenti che hanno il tag passato come argomento
     * @param tag il tag da cercare negli altri utenti
     * @return la lista di WinSomeUser che hanno il tag passato come argomento
     */
    public ArrayList<WinSomeUser> getUsersByTag(String tag) {
        ArrayList<WinSomeUser> ret = new ArrayList<>();
        for(WinSomeUser u : userList.values()) {
            if(u.getTags_list().contains(tag)) {
                ret.add(u);
            }
        }

        return ret;
    }

    /**
     * Restituisce la lista di WinSomePost che hanno username come autore
     * @param username l'autore di cui cercare i post
     * @return la lista di post creati da username
     */
    public ArrayList<WinSomePost> getUserPosts(String username) {
        ArrayList<WinSomePost> ret = new ArrayList<>();

        for (WinSomePost p : postList.values()) {
            if(p.getAuthor().equals(username)) {
                ret.add(p);
            }
        }

        return ret;
    }

    /**
     * Restituisce una lista di post che è riempita con i post nel blog dell'utente username
     * @param username l'username di cui vedere il blog
     * @return lista di WinSomePost nel blog di username
     */
    public ArrayList<WinSomePost> getBlog(String username) {
        ArrayList<WinSomePost> blog = new ArrayList<>();

        for (WinSomePost p : postList.values()) {
            if(p.getAuthor().equals(username) || p.isRewinUserPresent(username)) {
                blog.add(p);
            }
        }

        return blog;
    }

    /**
     * Restituisce una lista di post che è riempita con i post nel feed dell'utente username
     * @param username l'username di cui vedere il feed
     * @return lista di WinSomePost nel feed di username
     */
    public ArrayList<WinSomePost> getFeed(String username) {
        ArrayList<String> usersFollowedByUser = getFollowing(username);
        ArrayList<WinSomePost> feed = new ArrayList<>();

        for(String u : usersFollowedByUser) {
            ArrayList<WinSomePost> p = getBlog(u);
            if(p != null) feed.addAll(p);
        }

        return feed;
    }

    /**
     * Effettua il rewin del post post_id per conto di username
     * @param username l'username dell'utente che fa il rewin del post
     * @param post_id l'id del post da rewinnare
     * @throws InvalidOperationException se l'utente ha già fatto il rewin di questo post
     * @throws NotInFeedException se il post post_id non è nel feed di username
     * @throws PostNotFoundException se il post con id post_id non esiste
     * @throws UserNotFoundException se l'utente con username username non è valido
     * @throws SameUserException se si sta provando a fare il rewin di un proprio post
     */
    public void rewinPost(String username, int post_id) throws InvalidOperationException, NotInFeedException, PostNotFoundException, UserNotFoundException, SameUserException {
        if(!userList.containsKey(username)) throw new UserNotFoundException();
        WinSomePost toRewin = postList.get(post_id);
        if(toRewin == null) throw new PostNotFoundException();
        if(toRewin.getAuthor().equals(username)) throw new SameUserException();
        if(isPostNotInFeed(post_id, username)) throw new NotInFeedException();
        if(!toRewin.addRewin(username)) throw new InvalidOperationException();
    }

    /**
     * Fa seguire all'utente username l'utente newFollowing
     * @param username l'utente che seguirà newFollowing
     * @param newFollowing l'utente che avrà username come follower
     * @throws UserNotFoundException se newFollowing non esiste
     * @throws SameUserException se si sta provando a seguire se stessi
     * @throws InvalidOperationException se l'utente username segue già new Following
     */
    public void followUser(String username, String newFollowing) throws UserNotFoundException, SameUserException, InvalidOperationException {
        if(username.equals(newFollowing)) throw new SameUserException();
        if(!userList.containsKey(newFollowing)) throw new UserNotFoundException();
        if(followersList.get(newFollowing) != null && followersList.get(newFollowing).contains(username)) throw new InvalidOperationException();

        addFollowing(username, newFollowing);
        addFollower(newFollowing, username);
    }

    /**
     * Fa smettere di seguire all'utente username l'utente oldFollowing
     * @param username l'utente che non seguirà più oldFollowing
     * @param oldFollowing l'utente che avrà non avrà più username come follower
     * @throws UserNotFoundException se oldFollowing non esiste
     * @throws SameUserException se si sta provando a rimuovere il follow a se stessi
     * @throws InvalidOperationException se l'utente username non segue oldFollowing
     */
    public void unfollowUser(String username, String oldFollowing) throws UserNotFoundException, SameUserException, InvalidOperationException {
        if(username.equals(oldFollowing)) throw new SameUserException();
        if(!userList.containsKey(oldFollowing)) throw new UserNotFoundException();
        if(followersList.get(oldFollowing) == null) throw new InvalidOperationException();
        if(!followersList.get(oldFollowing).contains(username)) throw new InvalidOperationException();

        removeFollowing(username, oldFollowing);
        removeFollower(oldFollowing, username);
    }

    /**
     * Fornisce la lista di tutti i post
     * @return la lista di tutti i post, può essere vuota
     */
    public ConcurrentHashMap<Integer, WinSomePost> getPostList() {
        return postList;
    }

    /**
     * Fornisce la lista di tutti i follower
     * @return la lista di tutti i follower, può essere vuota
     */
    public ConcurrentHashMap<String, ArrayList<String>> getFollowersList() {
        return followersList;
    }

    /**
     * Fornisce la lista di tutti gli utenti che gli altri utenti seguono (ridondanza per followers)
     * @return la lista di tutti i following, può essere vuota
     */
    public ConcurrentHashMap<String, ArrayList<String>> getFollowingList() {
        return followingList;
    }

    /**
     * Fornisce la lista di tutti gli utenti registrati
     * @return la lista di utenti registrati, può essere vuota
     */
    public ConcurrentHashMap<String, WinSomeUser> getUserList() {
        return userList;
    }

    /**
     * Restituisce la lista di portafogli di tutti gli utenti
     * @return lista di tutti i portafogli
     */
    public ConcurrentHashMap<String, WinSomeWallet> getWalletList() {
        return walletList;
    }

    /**
     * Aggiunge un nuovo utente alla lista degli utenti del social
     * @param user l'utente da aggiungere
     */
    public void addUser(WinSomeUser user) {
        userList.put(user.getUsername(), user);
    }

    /**
     * Aggiunge a username il follower follower
     * @param username l'utente che avrà un nuovo follower
     * @param follower il nuovo follower di username
     */
    public void addFollower(String username, String follower) {
        if (followersList.containsKey(username)) {
            followersList.get(username).add(follower);
        } else {
            ArrayList<String> list = new ArrayList<>();
            list.add(follower);
            followersList.put(username, list);
        }
    }

    /**
     * Aggiunge a username l'utente following tra i propri seguiti
     * @param username l'utente che seguirà un nuovo utente
     * @param following il nuovo utente che username seguirà
     */
    public void addFollowing(String username, String following) {
        if (followingList.containsKey(username)) {
            followingList.get(username).add(following);
        } else {
            ArrayList<String> list = new ArrayList<>();
            list.add(following);
            followingList.put(username, list);
        }
    }

    /**
     * Fornisce la lista di follower dell'utente username
     * @param username l'username di cui si vuole la lista di follower
     * @return la lista di follower di username, può essere vuota
     */
    public ArrayList<String> getFollowers(String username) {
        ArrayList<String> ret = followersList.get(username);
        if(ret == null) return new ArrayList<>();
        return ret;
    }

    /**
     * Fornisce la lista di utenti che username segue
     * @param username l'username di cui si vuole la lista di seguiti
     * @return la lista di seguiti di username, può essere vuota (se non segue nessuno)
     */
    public ArrayList<String> getFollowing(String username) {
        ArrayList<String> ret = followingList.get(username);
        if(ret == null) return new ArrayList<>();
        return ret;
    }

    /**
     * Rimuove follower dalla lista follower di username
     * @param username l'username di cui aggiornare la lista
     * @param follower il vecchio follower di username
     */
    public void removeFollower(String username, String follower) {
        followersList.get(username).remove(follower);
    }

    /**
     * Rimuove following dalla lista di seguiti di username
     * @param username l'username di cui aggiornare la lista
     * @param following il vecchio seguito di username
     */
    public void removeFollowing(String username, String following) {
        followingList.get(username).remove(following);
    }

    /**
     * Dato un username fornisce l'oggetto utente
     * @param username l'username dell'utente da ottenere
     * @return l'oggetto WinSomeUser con l'username specificato, null se non esiste
     */
    public WinSomeUser getUser(String username) {
        return userList.get(username);
    }

    /**
     * Rimuove un utente dal social WinSome
     * @param username l'utente da rimuovere
     */
    public void removeUser(String username) {
        userList.remove(username);
        followersList.remove(username);
        followingList.remove(username);
    }

    /**
     * Trova un utente dato un username
     * @param username l'username da cercare
     * @return vero se username esiste, falso altrimenti
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean findUser(String username) {
        return userList.containsKey(username);
    }

    /**
     * Ottiene il numero degli utenti totali registrati
     * @return il numero di utenti registrati
     */
    public int getUserCount() {
        return userList.size();
    }

    /**
     * Ottiene un portafoglio WinSome dato un username
     * @param username l'username proprietario del portafoglio
     * @return il portafoglio dell'utente username, se non c'era viene inizializzato
     */
    public WinSomeWallet getWalletByUsername(String username) {
        walletList.putIfAbsent(username, new WinSomeWallet(username));
        return walletList.get(username);
    }

    // i seguenti 5 metodi servono al JsonManager per impostare i valori a quelli ottenuti dai file di persistenza all'avvio del server
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
    public void setWalletList(ConcurrentHashMap<String, WinSomeWallet> walletList) {
        this.walletList = walletList;
    }

    /**
     * Formatta un input numerico con la valuta del social WinSome approssimato a x cifre decimali (specificate nel config)
     * @param input il numero di valuta WinSome
     * @return il valore numerico formattato con la virgola (invece che col punto) + il nome della valuta winsome
     */
    public String getFormattedCurrency(double input) {
        String output = String.format("%." + currency_decimal_places + "f", Utils.roundC(input, currency_decimal_places));

        if(input == 1) {
            return output+" "+ currency_name_singular;
        } else {
            return output+" "+ currency_name_plural;
        }
    }

    /**
     * Formatta un input numerico approssimato a x cifre decimali (specificate nel config)
     * @param input il numero da formattare
     * @return il numero approssimato a x cifre decimali
     */
    public String getFormattedValue(double input) {
        return String.format("%." + currency_decimal_places + "f", Utils.roundC(input, currency_decimal_places));
    }

    /**
     * Verifica se la password fornita per autenticare l'utente u è valida
     * @param u l'utente di cui verificare la password
     * @param passwordToVerify la password da controllare
     * @return vero se la password corrisponde con quella dell'utente u, falso altrimenti
     */
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

    /**
     * Verifica che le preferenze specificate nel file di configurazione siano corrette facendo vari controlli. Se
     * anche una sola opzione è errata allora lancia un'eccezione.
     *
     * @throws ConfigurationException se una opzione della configurazione è errata
     */
    private void validateAndSavePreferences() throws ConfigurationException {
        // controllo id ultimo post
        try {
            last_post_id = new AtomicInteger(Integer.parseInt(config.getPreference("last_post_id")));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'last_post_id' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(last_post_id.get() < 0) {
            throw new ConfigurationException("valore 'last_post_id' non valido (" + last_post_id + " non può essere negativo)");
        }

        // controllo presenza nome valuta winsome singolare e plurale
        currency_name_singular = config.getPreference("currency_name_singular");
        if(currency_name_singular == null) {
            throw new ConfigurationException("valore 'currency_name_singular' non valido (non e' presente nel file di configurazione)");
        }
        currency_name_plural = config.getPreference("currency_name_plural");
        if(currency_name_plural == null) {
            throw new ConfigurationException("valore 'currency_name_plural' non valido (non e' presente nel file di configurazione)");
        }

        // controllo numero di cifre decimali valuta winsome
        try {
            currency_decimal_places = Integer.parseInt(config.getPreference("currency_decimal_places"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'currency_decimal_places' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(currency_decimal_places < 0 || currency_decimal_places > 8) {
            throw new ConfigurationException("valore 'currency_decimal_places' non valido (" + currency_decimal_places + " dovrebbe essere compreso tra 0 e 8)");
        }

        // controllo numero lunghezza massima titolo di un post
        try {
            post_max_title_length = Integer.parseInt(config.getPreference("post_max_title_length"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'post_max_title_length' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(post_max_title_length <= 0) {
            throw new ConfigurationException("valore 'post_max_title_length' non valido (" + post_max_title_length + " dovrebbe essere compreso maggiore di 0)");
        }

        // controllo numero lunghezza massima contenuto/corpo di un post
        try {
            post_max_content_length = Integer.parseInt(config.getPreference("post_max_content_length"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'post_max_content_length' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(post_max_content_length <= 0) {
            throw new ConfigurationException("valore 'post_max_content_length' non valido (" + post_max_content_length + " dovrebbe essere compreso maggiore di 0)");
        }
    }
}
