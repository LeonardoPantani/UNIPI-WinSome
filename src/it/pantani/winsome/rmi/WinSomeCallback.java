/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.rmi;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.HashMap;

public class WinSomeCallback extends RemoteObject implements WinSomeCallbackInterface {
    private static final HashMap<String, NotifyEventInterface> clients = new HashMap<>();

    @Override
    public void registerForCallback(String username, NotifyEventInterface clientInterface) throws RemoteException {
        if(!clients.containsKey(username)) {
            clients.put(username, clientInterface);
            System.out.println("[RMI]> Nuovo client registrato al callback.");
        }
    }

    @Override
    public void unregisterForCallback(String username) throws RemoteException {
        clients.remove(username);
        System.out.println("[RMI]> Rimosso client dal callback.");
    }


    public static void notifyFollowerUpdate(String username, String change) throws RemoteException {
        if(clients.containsKey(username)) {
           clients.get(username).notificationEvent(change);
        }
    }
}
