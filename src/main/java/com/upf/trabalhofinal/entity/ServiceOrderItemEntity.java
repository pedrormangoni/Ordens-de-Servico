/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.upf.trabalhofinal.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author Pedro
 */
@Entity
@Table(name = "service_order_items ")
public class ServiceOrderItemEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Basic(optional = false)
    @Column(name = "quantity", precision = 10, scale = 2)
    private BigDecimal quantity;

    @NotNull
    @Basic(optional = false)
    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unit_price;
    
    @NotNull
    @Basic(optional = false)
    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal total_price;

    @Column(name="notes")
    private String notes;
    
    // MAPEAMENTOS
    @ManyToOne(optional = false)
    @JoinColumn(name = "service_orders", referencedColumnName = "id")
    private ServiceOrderEntity service_orders;

    @ManyToOne(optional = false)
    @JoinColumn(name = "services", referencedColumnName = "id")
    private ServiceEntity service;

    // GETTER E SETTERS
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getUnit_price() {
        return unit_price;
    }

    public void setUnit_price(BigDecimal unit_price) {
        this.unit_price = unit_price;
    }

    public BigDecimal getTotal_price() {
        return total_price;
    }

    public void setTotal_price(BigDecimal total_price) {
        this.total_price = total_price;
    }

    public ServiceOrderEntity getService_orders() {
        return service_orders;
    }

    public void setService_orders(ServiceOrderEntity service_orders) {
        this.service_orders = service_orders;
    }

    public ServiceEntity getService() {
        return service;
    }

    public void setService(ServiceEntity service) {
        this.service = service;
    }
    
    
    
}
