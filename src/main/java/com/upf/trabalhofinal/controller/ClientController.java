/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.upf.trabalhofinal.controller;

import com.upf.trabalhofinal.entity.ClientEntity;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Pedro
 */
@Named(value = "clientController")
@SessionScoped
public class ClientController implements Serializable {
    
    @EJB 
    private ClientEntity client = new ClientEntity();
    
    private List<ClientEntity> pessoaList = new ArrayList<>();
    
    private ClientEntity selected;
    
    public ClientEntity getSelected() {
        return selected;
    }
    
    public void setSelected(ClientEntity selected) {
        this.selected = selected;
    }
    
    
}
