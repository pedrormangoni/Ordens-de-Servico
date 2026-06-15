package br.upf.serviceorders.facade;

import br.upf.serviceorders.entity.CashFlowEntity;
import br.upf.serviceorders.enums.CashFlowType;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Stateless
public class CashFlowFacade extends AbstractFacade<CashFlowEntity> {

    @PersistenceContext(unitName = "ServiceOrders")
    private EntityManager em;

    public CashFlowFacade() {
        super(CashFlowEntity.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<CashFlowEntity> findAllOrdered() {
        return em.createQuery(
            "SELECT c FROM CashFlowEntity c LEFT JOIN FETCH c.serviceOrder ORDER BY c.transactionDate DESC",
            CashFlowEntity.class)
            .getResultList();
    }

    public List<CashFlowEntity> findByType(CashFlowType type) {
        return em.createQuery(
            "SELECT c FROM CashFlowEntity c WHERE c.type = :type ORDER BY c.transactionDate DESC",
            CashFlowEntity.class)
            .setParameter("type", type)
            .getResultList();
    }

    public List<CashFlowEntity> findByPeriod(LocalDateTime start, LocalDateTime end) {
        return em.createQuery(
            "SELECT c FROM CashFlowEntity c LEFT JOIN FETCH c.serviceOrder "
            + "WHERE c.transactionDate BETWEEN :start AND :end ORDER BY c.transactionDate DESC",
            CashFlowEntity.class)
            .setParameter("start", start)
            .setParameter("end", end)
            .getResultList();
    }

    public BigDecimal getBalanceByType(CashFlowType type) {
        BigDecimal result = em.createQuery(
            "SELECT COALESCE(SUM(c.amount), 0) FROM CashFlowEntity c WHERE c.type = :type",
            BigDecimal.class)
            .setParameter("type", type)
            .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }
}
