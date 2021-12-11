/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.methods;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface WinSomeServiceInterface extends Remote {
    int register(String username, String password, List<String> tags_list) throws RemoteException;
}
