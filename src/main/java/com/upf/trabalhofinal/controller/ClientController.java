/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.upf.trabalhofinal.controller;

import com.upf.trabalhofinal.entity.ClientEntity;
import com.upf.trabalhofinal.facade.ClientFacade;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named(value = "clientController")
@SessionScoped
public class ClientController implements Serializable {

    @EJB
    private ClientFacade clientFacade;

    private ClientEntity client;
    private List<ClientEntity> list;
    private ClientEntity selected;

    @PostConstruct
    public void init() {
        client = new ClientEntity();
        list = new ArrayList<>();
        findAll();
    }

    public void prepareCreate() {
        client = new ClientEntity();
    }

    public void create() {
        clientFacade.create(client);
        findAll();
        prepareCreate();
    }

    public void edit() {
        clientFacade.edit(selected);
        findAll();
    }

    public void delete() {
        clientFacade.remove(selected);
        findAll();
    }

    public void findAll() {
        list = clientFacade.findAll();
    }

    public ClientEntity getClient() {
        return client;
    }

    public void setClient(ClientEntity client) {
        this.client = client;
    }

    public List<ClientEntity> getList() {
        return list;
    }

    public void setList(List<ClientEntity> list) {
        this.list = list;
    }

    public ClientEntity getSelected() {
        return selected;
    }

    public void setSelected(ClientEntity selected) {
        this.selected = selected;
    }
}
