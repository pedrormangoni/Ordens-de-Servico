package com.upf.trabalhofinal.facade;

import com.upf.trabalhofinal.entity.ServiceOrderEntity;
import com.upf.trabalhofinal.entity.ServiceOrderItemEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class ServiceOrderItemFacade extends AbstractFacade<ServiceOrderItemEntity> {

    @PersistenceContext(unitName = "my_persistence_unit")
    private EntityManager em;

    public ServiceOrderItemFacade() {
        super(ServiceOrderItemEntity.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<ServiceOrderItemEntity> findByServiceOrder(ServiceOrderEntity serviceOrder) {
        return em.createQuery(
            "SELECT i FROM ServiceOrderItemEntity i WHERE i.serviceOrder = :serviceOrder",
            ServiceOrderItemEntity.class)
            .setParameter("serviceOrder", serviceOrder)
            .getResultList();
    }
}
