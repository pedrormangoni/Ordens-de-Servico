package com.upf.trabalhofinal.controller;

import com.upf.trabalhofinal.entity.ServiceEntity;
import com.upf.trabalhofinal.facade.ServiceFacade;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named(value = "serviceController")
@SessionScoped
public class ServiceController implements Serializable {

    @EJB
    private ServiceFacade serviceFacade;

    private ServiceEntity service;
    private List<ServiceEntity> list;
    private ServiceEntity selected;

    @PostConstruct
    public void init() {
        service = new ServiceEntity();
        list = new ArrayList<>();
        findAll();
    }

    public void prepareCreate() {
        service = new ServiceEntity();
    }

    public void create() {
        serviceFacade.create(service);
        findAll();
        prepareCreate();
    }

    public void edit() {
        serviceFacade.edit(selected);
        findAll();
    }

    public void delete() {
        serviceFacade.remove(selected);
        findAll();
    }

    public void findAll() {
        list = serviceFacade.findAll();
    }

    public ServiceEntity getService() {
        return service;
    }

    public void setService(ServiceEntity service) {
        this.service = service;
    }

    public List<ServiceEntity> getList() {
        return list;
    }

    public void setList(List<ServiceEntity> list) {
        this.list = list;
    }

    public ServiceEntity getSelected() {
        return selected;
    }

    public void setSelected(ServiceEntity selected) {
        this.selected = selected;
    }
}
