package br.upf.serviceorders.controller;

import br.upf.serviceorders.entity.ServiceOrderEntity;
import br.upf.serviceorders.entity.ServiceOrderItemEntity;
import br.upf.serviceorders.facade.ServiceOrderItemFacade;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named(value = "serviceOrderItemController")
@SessionScoped
public class ServiceOrderItemController implements Serializable {

    @EJB
    private ServiceOrderItemFacade serviceOrderItemFacade;

    private ServiceOrderItemEntity item;
    private List<ServiceOrderItemEntity> list;
    private ServiceOrderItemEntity selected;

    @PostConstruct
    public void init() {
        item = new ServiceOrderItemEntity();
        list = new ArrayList<>();
        findAll();
    }

    public void prepareCreate() {
        item = new ServiceOrderItemEntity();
    }

    public void create() {
        serviceOrderItemFacade.create(item);
        findAll();
        prepareCreate();
    }

    public void edit() {
        serviceOrderItemFacade.edit(selected);
        findAll();
    }

    public void delete() {
        serviceOrderItemFacade.remove(selected);
        findAll();
    }

    public void findAll() {
        list = serviceOrderItemFacade.findAll();
    }

    public void findByServiceOrder(ServiceOrderEntity serviceOrder) {
        list = serviceOrderItemFacade.findByServiceOrder(serviceOrder);
    }

    public ServiceOrderItemEntity getItem() {
        return item;
    }

    public void setItem(ServiceOrderItemEntity item) {
        this.item = item;
    }

    public List<ServiceOrderItemEntity> getList() {
        return list;
    }

    public void setList(List<ServiceOrderItemEntity> list) {
        this.list = list;
    }

    public ServiceOrderItemEntity getSelected() {
        return selected;
    }

    public void setSelected(ServiceOrderItemEntity selected) {
        this.selected = selected;
    }
}
