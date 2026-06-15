package br.upf.serviceorders.facade;

import br.upf.serviceorders.entity.ClientEntity;
import br.upf.serviceorders.entity.ServiceOrderEntity;
import br.upf.serviceorders.entity.ServiceOrderItemEntity;
import br.upf.serviceorders.entity.UserEntity;
import br.upf.serviceorders.enums.ServiceOrderStatus;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Stateless
public class ServiceOrderFacade extends AbstractFacade<ServiceOrderEntity> {

    @PersistenceContext(unitName = "ServiceOrders")
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
        ClientEntity managedClient = em.getReference(ClientEntity.class, client.getId());
        UserEntity managedUser = em.getReference(UserEntity.class, user.getId());

        ServiceOrderEntity os = new ServiceOrderEntity();
        os.setNumber(generateNumber());
        os.setStatus(ServiceOrderStatus.OPEN);
        os.setClient(managedClient);
        os.setUser(managedUser);
        os.setDescription(description);
        os.setOpenedAt(LocalDateTime.now());
        os.setTotalAmount(BigDecimal.ZERO);
        em.persist(os);

        BigDecimal total = BigDecimal.ZERO;
        for (ServiceOrderItemEntity item : items) {
            ServiceOrderItemEntity managedItem = new ServiceOrderItemEntity();
            managedItem.setServiceOrder(os);
            managedItem.setService(em.getReference(
                    br.upf.serviceorders.entity.ServiceEntity.class,
                    item.getService().getId()));
            managedItem.setQuantity(item.getQuantity());
            managedItem.setUnit_price(item.getUnit_price());
            managedItem.setNotes(item.getNotes());
            managedItem.setTotal_price(item.getUnit_price().multiply(item.getQuantity()));
            em.persist(managedItem);
            total = total.add(managedItem.getTotal_price());
        }

        os.setTotalAmount(total);
        return os;
    }

    public void cancel(ServiceOrderEntity os) {
        ServiceOrderEntity managed = em.find(ServiceOrderEntity.class, os.getId());
        if (managed == null) {
            return;
        }
        if (managed.getStatus() == ServiceOrderStatus.OPEN
                || managed.getStatus() == ServiceOrderStatus.IN_PROGRESS) {
            managed.setStatus(ServiceOrderStatus.CANCELLED);
            em.merge(managed);
        }
    }

    public void startProgress(ServiceOrderEntity os) {
        ServiceOrderEntity managed = em.find(ServiceOrderEntity.class, os.getId());
        if (managed == null) {
            return;
        }
        if (managed.getStatus() == ServiceOrderStatus.OPEN) {
            managed.setStatus(ServiceOrderStatus.IN_PROGRESS);
            em.merge(managed);
        }
    }

    public void complete(ServiceOrderEntity os, String paymentMethod,
                         CashFlowFacade cashFlowFacade) {
        ServiceOrderEntity managed = em.find(ServiceOrderEntity.class, os.getId());
        if (managed == null) {
            return;
        }
        if (managed.getStatus() != ServiceOrderStatus.OPEN
                && managed.getStatus() != ServiceOrderStatus.IN_PROGRESS) {
            return;
        }
        managed.setStatus(ServiceOrderStatus.COMPLETED);
        managed.setCompletedAt(LocalDateTime.now());
        em.merge(managed);

        var entry = new br.upf.serviceorders.entity.CashFlowEntity();
        entry.setType(br.upf.serviceorders.enums.CashFlowType.INCOME);
        entry.setServiceOrder(managed);
        entry.setAmount(managed.getTotalAmount());
        entry.setDescription("OS Concluida #" + managed.getNumber());
        entry.setPaymentMethod(paymentMethod);
        entry.setTransactionDate(LocalDateTime.now());
        cashFlowFacade.create(entry);
    }

    public ServiceOrderEntity findDetail(Long id) {
        if (id == null) {
            return null;
        }
        return em.createQuery(
                "SELECT o FROM ServiceOrderEntity o "
                + "LEFT JOIN FETCH o.client "
                + "LEFT JOIN FETCH o.user "
                + "WHERE o.id = :id",
                ServiceOrderEntity.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public List<ServiceOrderEntity> findAllOrdered() {
        return em.createQuery(
                "SELECT o FROM ServiceOrderEntity o "
                + "LEFT JOIN FETCH o.client "
                + "ORDER BY o.openedAt DESC",
                ServiceOrderEntity.class)
                .getResultList();
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
        Long seq = em.createQuery(
                "SELECT COALESCE(MAX(o.id), 0) + 1 FROM ServiceOrderEntity o", Long.class)
                .getSingleResult();
        return String.format("OS-%04d", seq);
    }
}
