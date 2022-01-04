/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server.rmi;

import it.pantani.winsome.shared.rmi.WinSomeServiceInterface;
import it.pantani.winsome.server.ServerMain;
import it.pantani.winsome.server.SocialManager;
import it.pantani.winsome.server.entities.WinSomeUser;
import it.pantani.winsome.server.utils.PasswordManager;
import it.pantani.winsome.shared.Utils;

import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Classe che permette al client di registrarsi sul social WinSome. I seguenti metodi sono
 * chiamati dal client ma eseguiti lato server.
 */
public class WinSomeService implements WinSomeServiceInterface {
    /**
     * Registra un utente sul social WinSome e stampa un messaggio di log contenente l'esito della richiesta.
     * @param username il nome dell'utente da registrare
     * @param password la password (in chiaro) dell'utente da registrare
     * @param tags_list la lista di tags che contengono le categorie che interessano all'utente che si sta registrando
     * @return Utils.SOCIAL_REGISTRATION_FAILED se l'username fornito è già registrato, Utils.SOCIAL_REGISTRATION_SUCCESS altrimenti
     */
    public String register(String username, String password, ArrayList<String> tags_list) throws RemoteException {
        username = username.toLowerCase(); // username viene convertito tutto in minuscolo
        System.out.print("[WSS]> Registrazione dell'utente '" + username + "' " + tags_list + "...");

        if(!checkUsernameAvailability(username)) {
            System.out.println(" fallita");
            return Utils.SOCIAL_REGISTRATION_FAILED; // se l'username esiste già
        }

        SocialManager s = ServerMain.social;
        WinSomeUser newUser = new WinSomeUser(username, PasswordManager.hashPassword(password), tags_list);
        s.addUser(newUser);
        System.out.println(" successo");
        return Utils.SOCIAL_REGISTRATION_SUCCESS;
    }

    /**
     * Fornisce la lista di followers, dato un username e una password. Viene utilizzato la prima volta. Visto che
     * con solo l'username si potrebbe ottenere la lista bypassando l'autenticazione, è richiesta anche la password.
     * Non è un grande problema perché il client conosce la password inserita dall'utente in fase di login.
     * @param username l'username di cui ricevere la lista followers la prima volta
     * @param password la password dell'username
     * @return lista dei follower dell'utente username (se username non ha follower la lista è vuota)
     */
    public ArrayList<String> initializeFollowerList(String username, String password) throws RemoteException {
        SocialManager s = ServerMain.social;
        WinSomeUser u = s.getUser(username);
        if(u != null) {
            if(s.checkUserPassword(u, password)) {
                return s.getFollowers(username);
            }
        }
        return new ArrayList<>(); // restituisco lista vuota altrimenti
    }

    /**
     * Controlla se un username è disponibile per la registrazione
     * @param username l'username di cui verificare la disponibilità
     * @return vero se l'username non è ancora registrato, falso altrimenti
     */
    private static boolean checkUsernameAvailability(String username) {
        SocialManager s = ServerMain.social;
        return !s.findUser(username);
    }
}
