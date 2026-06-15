package br.upf.serviceorders.facade;

import br.upf.serviceorders.entity.UserEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;

@Stateless
public class UserFacade extends AbstractFacade<UserEntity> {

    @PersistenceContext(unitName = "ServiceOrders")
    private EntityManager em;

    public UserFacade() {
        super(UserEntity.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public Optional<UserEntity> authenticate(String email, String password) {
        return em.createQuery(
                "SELECT u FROM UserEntity u "
                + "WHERE LOWER(u.email) = LOWER(:email) "
                + "AND u.password = :password "
                + "AND u.active = true",
                UserEntity.class)
                .setParameter("email", email)
                .setParameter("password", password)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    public Optional<UserEntity> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return em.createQuery(
                "SELECT u FROM UserEntity u WHERE LOWER(u.email) = LOWER(:email)",
                UserEntity.class)
                .setParameter("email", email.trim())
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }
}
