/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.rmi;

import it.pantani.winsome.client.ClientMain;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

/**
 * Classe usata per la notifica dell'aggiornamento della lista di follower. Il server
 * non fa altro che eseguire il metodo NotificationEvent tramite RMI sul client, il quale
 * poi aggiornerà la lista dei follower salvata localmente dal client.
 * Nota: la lista di follower viene inizializzata la prima volta al login e da quel momento
 * in poi è aggiornata tramite questo metodo.
 */
public class NotifyEvent extends RemoteObject implements NotifyEventInterface {
    /**
     * Aggiorna la lista follower in base alla stringa ricevuta.
     * @param update deve essere formattata così: "+/-*nome_utente*" dove + indica nuovo follower
     *               e - indica che quell'utente ha smesso di seguire l'utente loggato nel client
     * @throws RemoteException se si verifica un errore nella comunicazione
     */
    public void notificationEvent(String update) throws RemoteException {
        String follower;
        if(update.startsWith("+")) {
            follower = update.substring(1);
            ClientMain.listaFollower.add(follower);
            //System.out.println("[RMI]> Ha iniziato a seguirti: " + follower);
        } else if(update.startsWith("-")) {
            follower = update.substring(1);
            ClientMain.listaFollower.remove(follower);
            //System.out.println("[RMI]> Non ti segue piu': " + follower);
        } else {
            System.err.println("[!] Ricezione notifica non valida.");
        }
    }
}
