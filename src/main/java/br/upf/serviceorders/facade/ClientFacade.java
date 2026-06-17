package br.upf.serviceorders.facade;

import br.upf.serviceorders.entity.ClientEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class ClientFacade extends AbstractFacade<ClientEntity> {

    @PersistenceContext(unitName = "ServiceOrders")
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

    public ClientEntity findByDocument(String document) {
        if (document == null || document.isBlank()) {
            return null;
        }
        List<ClientEntity> results = em.createQuery(
            "SELECT c FROM ClientEntity c WHERE c.document = :document",
            ClientEntity.class)
            .setParameter("document", document)
            .setMaxResults(1)
            .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<ClientEntity> findAll() {
        return em.createQuery(
                "SELECT c FROM ClientEntity c "
                + "LEFT JOIN FETCH c.createdBy "
                + "ORDER BY c.name",
                ClientEntity.class)
                .getResultList();
    }
}
