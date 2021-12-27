/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server.entities;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Classe che rappresenta un post di WinSome. E' una delle principali e contiene tutte le informazioni necessarie che
 * permettono il funzionamento del social. Tutti gli attributi si riferiscono all'oggetto eccetto rewinUsers (gli utenti
 * che hanno fatto rewin al post) e numIterations (necessario per il calcolo dei premi) che, seppur non essendo inerenti
 * completamente all'oggetto post (e violando diciamo il principio di modularità dell'Object Oriented Programming),
 * si è deciso di salvarli qui per maggiore comodità.
 */
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

    /**
     * Questo costruttore inizializza un oggetto di tipo Post. Il post come entità è immutabile, tuttavia, per comodità
     * l'unico attributo che invece è modificabile è numIterations che viene incrementato ad ogni controllo del post
     * da parte del gestore premi. La data di invio del post è impostata in automatico.
     * @param postID id del post
     * @param author autore del post
     * @param postTitle titolo del post
     * @param postContent corpo (contenuto) del post
     */
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

    /**
     * Fornisce l'id del post
     * @return id del post
     */
    public int getPostID() {
        return postID;
    }

    /**
     * Fornisce l'autore del post
     * @return autore del post
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Fornisce il titolo del post
     * @return titolo del post
     */
    public String getPostTitle() {
        return postTitle;
    }

    /**
     * Fornisce il corpo (contenuto) del post
     * @return corpo (contenuto) del post
     */
    public String getPostContent() {
        return postContent;
    }

    /**
     * Fornisce la data di invio del post
     * @return data di invio del post in tempo UNIX
     */
    public long getDateSent() {
        return dateSent;
    }

    /**
     * Fornisce la lista dei voti
     * @return lista dei voti del post
     */
    public ConcurrentHashMap<String, WinSomeVote> getVoteList() {
        return votes;
    }

    /**
     * Fornisce la lista dei voti inviati dopo una certa data
     * @param after_this_date la minima data (in tempo UNIX) che i voti devono avere per essere restituiti
     * @return lista dei voti del post inviati dopo after_this_date
     */
    public ConcurrentHashMap<String, WinSomeVote> getVoteList(long after_this_date) {
        ConcurrentHashMap<String, WinSomeVote> ret = new ConcurrentHashMap<>();

        for(WinSomeVote v : votes.values()) {
            if(v.getDateSent() >= after_this_date) {
                ret.put(v.getAuthor(), v);
            }
        }

        return ret;
    }

    /**
     * Fornisce il totale di voti del post
     * @return numero di voti totale
     */
    public int getTotalVotes() {
        return votes.size();
    }

    /**
     * Fornisce il numero di voti con un certo valore
     * Si è voluti rimanere generici perché un'altra implementazione del social potrebbe per esempio
     * far assumere voti diversi da 1 e -1.
     * @param findValue il valore del voto che si intende cercare
     * @return il numero di voti che corrispondono al criterio
     */
    public int getNumVotesByValue(int findValue) {
        int found = 0;

        for(WinSomeVote value : votes.values()) {
            if(value.getVote() == findValue) found++;
        }

        return found;
    }

    /**
     * Fornisce la lista di commenti del post
     * @return lista di commenti al post
     */
    public ArrayList<WinSomeComment> getCommentList() {
        return comments;
    }

    /**
     * Fornisce la lista i commenti inviati dopo una certa data
     * @param after_this_date la minima data (in tempo UNIX) che i commenti devono avere per essere restituiti
     * @return lista dei commenti al post inviati dopo after_this_date
     */
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

    /**
     * Aggiunge un voto al post
     * @param username l'username dell'utente che aggiunge il voto
     * @param value il valore del voto dell'utente
     */
    public void addVote(String username, int value) {
        votes.putIfAbsent(username, new WinSomeVote(username, value));
    }

    /**
     * Aggiunge un commento al post
     * @param username l'username dell'utente che aggiunge un commento
     * @param content il contenuto del commento dell'utente
     */
    public void addComment(String username, String content) {
        comments.add(new WinSomeComment(username, content));
    }

    /**
     * Fornisce il voto di un utente su questo post
     * @param username l'username dell'utente
     * @return se l'utente ha votato questo post restituisce il valore del voto, null altrimenti
     */
    public Integer findVoteByUser(String username) {
        WinSomeVote v = votes.get(username);
        if(v == null) return null;
        return v.getVote();
    }

    /**
     * Fornisce una lista di commenti fatti da un utente
     * @param username l'username dell'utente
     * @return la lista di commenti fatti dall'utente su questo post
     */
    public ArrayList<WinSomeComment> findCommentsByUser(String username) {
        ArrayList<WinSomeComment> ret = new ArrayList<>();

        for(WinSomeComment c : comments) {
            if(c.getAuthor().equals(username)) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * Fornisce una lista di commenti fatti da un utente dopo una certa data
     * @param username l'username dell'utente
     * @param after_this_date la minima data (in tempo UNIX) che i commenti devono avere per essere restituiti
     * @return lista dei commenti al post inviati da username dopo after_this_date
     */
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

    /**
     * Fornisce la lista di tutti gli utenti che hanno commentato questo post
     * @return lista degli utenti che hanno commentato questo post
     */
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

    /**
     * Fornisce la lista di tutti gli utenti che hanno commentato questo post dopo una certa data
     * @param after_this_date la minima data (in tempo UNIX) che i commenti di un utente devono avere perché questo utente appaia in lista
     * @return lista degli utenti che hanno almeno un commento con una data superiore o uguale a after_this_date
     */
    public ArrayList<String> getUsersCommenting(long after_this_date) {
        ArrayList<String> ret = new ArrayList<>();

        for(WinSomeComment c : comments) {
            if(!ret.contains(c.getAuthor()) && c.getDateSent() >= after_this_date) {
                ret.add(c.getAuthor());
            }
        }

        return ret;
    }

    /**
     * Fornisce la lista di utenti che hanno fatto il rewin di questo post
     * @return lista di utenti che hanno fatto il rewin
     */
    public ConcurrentLinkedQueue<String> getRewinUsers() {
        return rewinUsers;
    }

    /**
     * Aggiunge username alla lista di utenti che hanno fatto il rewin di questo post
     * @param username l'username che vorrebbe fare il rewin
     * @return vero se l'utente non ha mai fatto il rewin di questo post in passato, falso altrimenti
     */
    public boolean addRewin(String username) {
        if(rewinUsers.contains(username)) {
            return false;
        } else {
            rewinUsers.add(username);
            return true;
        }
    }

    /**
     * Verifica se l'utente ha mai fatto un rewin di questo post
     * @param username l'utente di cui si vuole verificare la condizione
     * @return vero se l'utente ha già fatto il rewin di questo post, falso altrimenti
     */
    public boolean isRewinUserPresent(String username) {
        return rewinUsers.contains(username);
    }

    /**
     * Fornisce il numero di iterazioni che il gestore premi ha fatto su questo post
     * @return numero di iterazioni su questo post
     */
    public int getNumIterations() {
        return numIterations;
    }

    /**
     * Aggiunge un'iterazione su questo post
     * @return il numero di iterazioni totali dopo l'incremento
     */
    public int addIteration() {
        return ++this.numIterations;
    }
}
