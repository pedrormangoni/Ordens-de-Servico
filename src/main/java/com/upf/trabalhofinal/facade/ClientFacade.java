package com.upf.trabalhofinal.facade;

import com.upf.trabalhofinal.entity.ClientEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class ClientFacade extends AbstractFacade<ClientEntity> {

    @PersistenceContext(unitName = "my_persistence_unit")
    private EntityManager em;

    public ClientFacade() {
        super(ClientEntity.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<ClientEntity> findByName(String name) {
        return em.createQuery(
            "SELECT c FROM ClientEntity c WHERE LOWER(c.name) LIKE LOWER(:name)",
            ClientEntity.class)
            .setParameter("name", "%" + name + "%")
            .getResultList();
    }
}
