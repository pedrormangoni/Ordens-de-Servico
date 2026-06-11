package com.upf.trabalhofinal.facade;

import com.upf.trabalhofinal.entity.UserEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;

@Stateless
public class UserFacade extends AbstractFacade<UserEntity> {

    @PersistenceContext(unitName = "my_persistence_unit")
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
}
