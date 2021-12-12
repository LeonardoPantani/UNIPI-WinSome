/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.rmi;

import it.pantani.winsome.ServerMain;
import it.pantani.winsome.entities.WinSomeUser;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class WinSomeService implements WinSomeServiceInterface {
    @Override
    public String register(String username, String password, ArrayList<String> tags_list) throws RemoteException {
        if(!checkUsernameAvailability(username)) return "errore: esiste gia' un utente con username '" + username + "'";

        WinSomeUser newUser = new WinSomeUser(username, password, tags_list);
        ServerMain.listaUtenti.add(newUser);
        return "registrazione utente " + username + " completata";
    }

    private boolean checkUsernameAvailability(String username) {
        for(WinSomeUser u : ServerMain.listaUtenti) {
            if(u.getUsername().equalsIgnoreCase(username)) {
                return false;
            }
        }

        return true;
    }
}
