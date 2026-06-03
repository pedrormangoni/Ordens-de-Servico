package com.upf.trabalhofinal.facade;

import com.upf.trabalhofinal.entity.UserEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
}
