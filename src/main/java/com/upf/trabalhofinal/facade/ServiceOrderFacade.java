package com.upf.trabalhofinal.facade;

import com.upf.trabalhofinal.entity.ClientEntity;
import com.upf.trabalhofinal.entity.ServiceOrderEntity;
import com.upf.trabalhofinal.entity.ServiceOrderItemEntity;
import com.upf.trabalhofinal.entity.UserEntity;
import com.upf.trabalhofinal.enums.ServiceOrderStatus;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Stateless
public class ServiceOrderFacade extends AbstractFacade<ServiceOrderEntity> {

    @PersistenceContext(unitName = "my_persistence_unit")
    private EntityManager em;

    public ServiceOrderFacade() {
        super(ServiceOrderEntity.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ServiceOrderEntity open(ClientEntity client, UserEntity user,
                                   String description, List<ServiceOrderItemEntity> items) {
        ServiceOrderEntity os = new ServiceOrderEntity();
        os.setNumber(generateNumber());
        os.setStatus(ServiceOrderStatus.OPEN);
        os.setClient(client);
        os.setUser(user);
        os.setDescription(description);
        os.setOpenedAt(LocalDateTime.now());
        os.setTotalAmount(BigDecimal.ZERO);
        em.persist(os);

        BigDecimal total = BigDecimal.ZERO;
        for (ServiceOrderItemEntity item : items) {
            item.setServiceOrder(os);
            item.setTotal_price(
                item.getUnit_price().multiply(item.getQuantity())
            );
            em.persist(item);
            total = total.add(item.getTotal_price());
        }

        os.setTotalAmount(total);
        em.merge(os);
        return os;
    }

    public void complete(ServiceOrderEntity os, String paymentMethod,
                         CashFlowFacade cashFlowFacade) {
        os.setStatus(ServiceOrderStatus.COMPLETED);
        os.setCompletedAt(LocalDateTime.now());
        em.merge(os);

        var entry = new com.upf.trabalhofinal.entity.CashFlowEntity();
        entry.setType(com.upf.trabalhofinal.enums.CashFlowType.INCOME);
        entry.setServiceOrder(os);
        entry.setAmount(os.getTotalAmount());
        entry.setDescription("OS Concluida #" + os.getNumber());
        entry.setPaymentMethod(paymentMethod);
        entry.setTransactionDate(LocalDateTime.now());
        cashFlowFacade.create(entry);
    }

    public void cancel(ServiceOrderEntity os) {
        os.setStatus(ServiceOrderStatus.CANCELLED);
        em.merge(os);
    }

    public List<ServiceOrderEntity> findByStatus(ServiceOrderStatus status) {
        return em.createQuery(
            "SELECT o FROM ServiceOrderEntity o WHERE o.status = :status ORDER BY o.openedAt DESC",
            ServiceOrderEntity.class)
            .setParameter("status", status)
            .getResultList();
    }

    public List<ServiceOrderEntity> findByClient(ClientEntity client) {
        return em.createQuery(
            "SELECT o FROM ServiceOrderEntity o WHERE o.client = :client ORDER BY o.openedAt DESC",
            ServiceOrderEntity.class)
            .setParameter("client", client)
            .getResultList();
    }

    private String generateNumber() {
        return "OS-" + System.currentTimeMillis();
    }
}
