package br.upf.serviceorders.facade;

import br.upf.serviceorders.entity.CashFlowEntity;
import br.upf.serviceorders.enums.CashFlowType;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
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
        return findFiltered(null, null);
    }

    public List<CashFlowEntity> findFiltered(LocalDate startDate, LocalDate endDate) {
        StringBuilder jpql = new StringBuilder(
                "SELECT c FROM CashFlowEntity c LEFT JOIN FETCH c.createdBy");
        boolean hasWhere = false;

        if (startDate != null) {
            jpql.append(" WHERE c.transactionDate >= :start");
            hasWhere = true;
        }
        if (endDate != null) {
            jpql.append(hasWhere ? " AND" : " WHERE");
            jpql.append(" c.transactionDate <= :end");
        }
        jpql.append(" ORDER BY c.transactionDate DESC");

        var query = em.createQuery(jpql.toString(), CashFlowEntity.class);
        if (startDate != null) {
            query.setParameter("start", startDate.atStartOfDay());
        }
        if (endDate != null) {
            query.setParameter("end", endDate.atTime(23, 59, 59));
        }

        List<CashFlowEntity> items = query.getResultList();
        initializeRelations(items);
        return items;
    }

    public List<CashFlowEntity> findByType(CashFlowType type) {
        return em.createQuery(
            "SELECT c FROM CashFlowEntity c WHERE c.type = :type ORDER BY c.transactionDate DESC",
            CashFlowEntity.class)
            .setParameter("type", type)
            .getResultList();
    }

    public List<CashFlowEntity> findByPeriod(LocalDateTime start, LocalDateTime end) {
        LocalDate startDate = start != null ? start.toLocalDate() : null;
        LocalDate endDate = end != null ? end.toLocalDate() : null;
        return findFiltered(startDate, endDate);
    }

    private void initializeRelations(List<CashFlowEntity> items) {
        for (CashFlowEntity item : items) {
            if (item.getServiceOrder() != null) {
                item.getServiceOrder().getNumber();
            }
            if (item.getCreatedBy() != null) {
                item.getCreatedBy().getName();
            }
        }
    }

    public BigDecimal getBalanceByType(CashFlowType type) {
        BigDecimal result = em.createQuery(
            "SELECT COALESCE(SUM(c.amount), 0) FROM CashFlowEntity c WHERE c.type = :type",
            BigDecimal.class)
            .setParameter("type", type)
            .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    public long countByServiceOrderId(Long serviceOrderId) {
        if (serviceOrderId == null) {
            return 0;
        }
        return em.createQuery(
                "SELECT COUNT(c) FROM CashFlowEntity c WHERE c.serviceOrder.id = :serviceOrderId",
                Long.class)
                .setParameter("serviceOrderId", serviceOrderId)
                .getSingleResult();
    }
}
