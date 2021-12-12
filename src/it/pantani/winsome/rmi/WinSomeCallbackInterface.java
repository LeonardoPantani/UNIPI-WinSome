/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WinSomeCallbackInterface extends Remote {
    void registerForCallback(String username, NotifyEventInterface clientInterface) throws RemoteException;
    void unregisterForCallback(String username) throws RemoteException;
}
