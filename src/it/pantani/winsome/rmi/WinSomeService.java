/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.rmi;

import it.pantani.winsome.ServerMain;
import it.pantani.winsome.SocialManager;
import it.pantani.winsome.entities.WinSomeUser;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class WinSomeService implements WinSomeServiceInterface {
    @Override
    public String register(String username, String password, ArrayList<String> tags_list) throws RemoteException {
        username = username.toLowerCase(); // username tutto in minuscolo

        if(!checkUsernameAvailability(username)) return "errore: esiste gia' un utente con username '" + username + "'";
        SocialManager s = ServerMain.social;

        WinSomeUser newUser = new WinSomeUser(username, password, tags_list);

        s.addUser(newUser);
        return "registrazione utente " + username + " completata";
    }

    @Override
    public ArrayList<String> initializeFollowerList(String username, String password) throws RemoteException {
        SocialManager s = ServerMain.social;
        WinSomeUser u = s.getUser(username);
        if(u != null) {
            if(u.checkPassword(password)) {
                return s.getFollowers(username);
            }
        }
        return null;
    }

    private boolean checkUsernameAvailability(String username) {
        SocialManager s = ServerMain.social;
        return !s.findUser(username);
    }
}
