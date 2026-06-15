package br.upf.serviceorders.facade;

import br.upf.serviceorders.entity.ServiceOrderEntity;
import br.upf.serviceorders.entity.ServiceOrderItemEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class ServiceOrderItemFacade extends AbstractFacade<ServiceOrderItemEntity> {

    @PersistenceContext(unitName = "ServiceOrders")
    private EntityManager em;

    public ServiceOrderItemFacade() {
        super(ServiceOrderItemEntity.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<ServiceOrderItemEntity> findByServiceOrder(ServiceOrderEntity serviceOrder) {
        if (serviceOrder == null || serviceOrder.getId() == null) {
            return List.of();
        }
        return em.createQuery(
            "SELECT i FROM ServiceOrderItemEntity i "
            + "JOIN FETCH i.service "
            + "WHERE i.serviceOrder.id = :orderId "
            + "ORDER BY i.id",
            ServiceOrderItemEntity.class)
            .setParameter("orderId", serviceOrder.getId())
            .getResultList();
    }
}
