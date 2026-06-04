package com.upf.trabalhofinal.controller;

import com.upf.trabalhofinal.entity.ClientEntity;
import com.upf.trabalhofinal.entity.ServiceOrderEntity;
import com.upf.trabalhofinal.entity.ServiceOrderItemEntity;
import com.upf.trabalhofinal.entity.UserEntity;
import com.upf.trabalhofinal.enums.ServiceOrderStatus;
import com.upf.trabalhofinal.facade.CashFlowFacade;
import com.upf.trabalhofinal.facade.ServiceOrderFacade;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Named(value = "serviceOrderController")
@SessionScoped
public class ServiceOrderController implements Serializable {

    @EJB
    private ServiceOrderFacade serviceOrderFacade;

    @EJB
    private CashFlowFacade cashFlowFacade;

    private ServiceOrderEntity serviceOrder;
    private List<ServiceOrderEntity> list;
    private ServiceOrderEntity selected;

    private ClientEntity client;
    private UserEntity user;
    private List<ServiceOrderItemEntity> items;
    private String paymentMethod;

    @PostConstruct
    public void init() {
        serviceOrder = new ServiceOrderEntity();
        list = new ArrayList<>();
        items = new ArrayList<>();
        findAll();
    }

    public void prepareCreate() {
        serviceOrder = new ServiceOrderEntity();
        items = new ArrayList<>();
        client = null;
        user = null;
    }

    public void create() {
        serviceOrderFacade.open(client, user, serviceOrder.getDescription(), items);
        findAll();
        prepareCreate();
    }

    public void edit() {
        serviceOrderFacade.edit(selected);
        findAll();
    }

    public void delete() {
        serviceOrderFacade.remove(selected);
        findAll();
    }

    public void complete() {
        serviceOrderFacade.complete(selected, paymentMethod, cashFlowFacade);
        findAll();
    }

    public void cancel() {
        serviceOrderFacade.cancel(selected);
        findAll();
    }

    public void findAll() {
        list = serviceOrderFacade.findAll();
    }

    public void findByStatus(ServiceOrderStatus status) {
        list = serviceOrderFacade.findByStatus(status);
    }

    public void findByClient(ClientEntity client) {
        list = serviceOrderFacade.findByClient(client);
    }

    public void addItem(ServiceOrderItemEntity item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
    }

    public BigDecimal getItemsTotal() {
        if (items == null) {
            return BigDecimal.ZERO;
        }
        return items.stream()
            .map(i -> i.getTotal_price() != null ? i.getTotal_price() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public ServiceOrderEntity getServiceOrder() {
        return serviceOrder;
    }

    public void setServiceOrder(ServiceOrderEntity serviceOrder) {
        this.serviceOrder = serviceOrder;
    }

    public List<ServiceOrderEntity> getList() {
        return list;
    }

    public void setList(List<ServiceOrderEntity> list) {
        this.list = list;
    }

    public ServiceOrderEntity getSelected() {
        return selected;
    }

    public void setSelected(ServiceOrderEntity selected) {
        this.selected = selected;
    }

    public ClientEntity getClient() {
        return client;
    }

    public void setClient(ClientEntity client) {
        this.client = client;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public List<ServiceOrderItemEntity> getItems() {
        return items;
    }

    public void setItems(List<ServiceOrderItemEntity> items) {
        this.items = items;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
