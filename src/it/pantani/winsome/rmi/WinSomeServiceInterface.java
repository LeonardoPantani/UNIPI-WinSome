/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface WinSomeServiceInterface extends Remote {
    String register(String username, String password, ArrayList<String> tags_list) throws RemoteException;
    ArrayList<String> initializeFollowerList(String username, String password) throws RemoteException; // TODO rendere migliore sta roba
    // TODO il client da .jar non si avvia perché il .jar legge la classe main del server e non del client, quindi ottimizzare tutte le classi (vedi file sul desktop) e poi dividere le classi in package diversi
}
