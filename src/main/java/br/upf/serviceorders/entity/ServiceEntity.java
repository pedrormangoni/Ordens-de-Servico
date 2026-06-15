/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.upf.serviceorders.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 *
 * @author Pedro
 */
@Entity
@Table(name="services")
public class ServiceEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional=false)
    @Column(name="id")
    private Long id;
    
    @NotNull
    @Size(min=1, max=150)
    @Basic(optional=false)
    @Column(name="name")
    private String name;
    
    @Size(max = 1000)
    @Column(name="description")
    private String description;
    
    @NotNull
    @Basic(optional=false)
    @Column(name="price", precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;
    
    @Basic(optional=false)
    @Column(name="active")
    private boolean active;

    @Basic(optional=false)
    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // GETTER E SETTERS

    @Override
    public String toString() {
        return name != null ? name : "";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (price == null) {
            price = BigDecimal.ZERO;
        }
    }

    // EQUALS E HASHCODE
    
       @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        ServiceEntity other = (ServiceEntity) obj;
        return Objects.equals(this.id, other.id);
    }
}
