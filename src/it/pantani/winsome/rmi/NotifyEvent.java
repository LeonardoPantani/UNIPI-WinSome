/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.rmi;

import it.pantani.winsome.ClientMain;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

public class NotifyEvent extends RemoteObject implements NotifyEventInterface {
    @Override
    public void notificationEvent(String update) throws RemoteException {
        String follower;
        if(update.startsWith("+")) {
            follower = update.substring(2);
            ClientMain.listaFollower.add(follower);
            System.out.println("[RMI]> Ha iniziato a seguirti: " + follower);
        } else if(update.startsWith("-")) {
            follower = update.substring(2);
            ClientMain.listaFollower.remove(follower);
            System.out.println("[RMI]> Non ti segue piu': " + follower);
        } else {
            System.err.println("[!] Ricezione notifica non valida!");
        }
    }
}
