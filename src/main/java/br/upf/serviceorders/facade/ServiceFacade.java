package br.upf.serviceorders.facade;

import br.upf.serviceorders.entity.ServiceEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class ServiceFacade extends AbstractFacade<ServiceEntity> {

    @PersistenceContext(unitName = "ServiceOrders")
    private EntityManager em;

    public ServiceFacade() {
        super(ServiceEntity.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<ServiceEntity> findActive() {
        return em.createQuery(
            "SELECT s FROM ServiceEntity s WHERE s.active = true ORDER BY s.name",
            ServiceEntity.class)
            .getResultList();
    }
}
