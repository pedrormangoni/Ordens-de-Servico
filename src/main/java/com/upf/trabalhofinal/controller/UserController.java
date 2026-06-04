package com.upf.trabalhofinal.controller;

import com.upf.trabalhofinal.entity.UserEntity;
import com.upf.trabalhofinal.facade.UserFacade;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named(value = "userController")
@SessionScoped
public class UserController implements Serializable {

    @EJB
    private UserFacade userFacade;

    private UserEntity user;
    private List<UserEntity> list;
    private UserEntity selected;

    @PostConstruct
    public void init() {
        user = new UserEntity();
        list = new ArrayList<>();
        findAll();
    }

    public void prepareCreate() {
        user = new UserEntity();
    }

    public void create() {
        userFacade.create(user);
        findAll();
        prepareCreate();
    }

    public void edit() {
        userFacade.edit(selected);
        findAll();
    }

    public void delete() {
        userFacade.remove(selected);
        findAll();
    }

    public void findAll() {
        list = userFacade.findAll();
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public List<UserEntity> getList() {
        return list;
    }

    public void setList(List<UserEntity> list) {
        this.list = list;
    }

    public UserEntity getSelected() {
        return selected;
    }

    public void setSelected(UserEntity selected) {
        this.selected = selected;
    }
}
