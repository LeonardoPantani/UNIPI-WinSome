/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.rmi;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.HashMap;

/**
 * Classe che permette al client di registrarsi al server per poter ricevere notifiche
 * riguardo all'aggiornamento della lista followers. Il server mantiene una hashmap
 * con chiave username in modo da associare un utente alla relativa interfaccia su cui
 * inviare la notifica.
 */
public class WinSomeCallback extends RemoteObject implements WinSomeCallbackInterface {
    private static final HashMap<String, NotifyEventInterface> clients = new HashMap<>();

    /**
     * Permette al client di registrarsi al callback in modo che il server sappia a
     * chi inviare le notifiche.
     * @param username il nome dell'utente attualmente loggato
     * @param clientInterface l'interfaccia del client
     */
    public void registerForCallback(String username, NotifyEventInterface clientInterface) throws RemoteException {
        if(!clients.containsKey(username)) {
            clients.put(username, clientInterface);
        }
    }

    /**
     * Permette al client di rimuoversi dalla lista da contattare in caso di aggiornamenti
     * alla lista followers.
     * @param username il nome dell'utente da scollegare
     */
    public void unregisterForCallback(String username) throws RemoteException {
        clients.remove(username);
    }

    /**
     * Esegue nel client il metodo notificationEvent. Deve essere specificato l'username e il cambiamento
     * nella lista followers.
     * @param username l'username dell'utente loggato di cui notificare la modifica
     * @param change il cambiamento nella lista follower da notificare
     */
    public static void notifyFollowerUpdate(String username, String change) throws RemoteException {
        if(clients.containsKey(username)) {
           clients.get(username).notificationEvent(change);
        }
    }
}
