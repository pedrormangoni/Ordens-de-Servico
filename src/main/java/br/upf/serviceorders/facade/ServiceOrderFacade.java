package br.upf.serviceorders.facade;

import br.upf.serviceorders.entity.ClientEntity;
import br.upf.serviceorders.entity.ServiceOrderEntity;
import br.upf.serviceorders.entity.ServiceOrderItemEntity;
import br.upf.serviceorders.entity.UserEntity;
import br.upf.serviceorders.enums.ServiceOrderStatus;
import br.upf.serviceorders.util.MonetaryAmounts;
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
            BigDecimal quantity = MonetaryAmounts.normalize(item.getQuantity());
            BigDecimal unitPrice = MonetaryAmounts.normalize(item.getUnit_price());
            BigDecimal lineTotal = MonetaryAmounts.lineTotal(unitPrice, quantity);

            ServiceOrderItemEntity managedItem = new ServiceOrderItemEntity();
            managedItem.setServiceOrder(os);
            managedItem.setService(em.getReference(
                    br.upf.serviceorders.entity.ServiceEntity.class,
                    item.getService().getId()));
            managedItem.setQuantity(quantity);
            managedItem.setUnit_price(unitPrice);
            managedItem.setNotes(item.getNotes());
            managedItem.setTotal_price(lineTotal);
            em.persist(managedItem);
            total = total.add(lineTotal);
        }

        os.setTotalAmount(MonetaryAmounts.normalize(total));
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

    public void startProgress(ServiceOrderEntity os, String paymentMethod) {
        ServiceOrderEntity managed = em.find(ServiceOrderEntity.class, os.getId());
        if (managed == null) {
            return;
        }
        if (managed.getStatus() == ServiceOrderStatus.OPEN) {
            managed.setPaymentMethod(resolvePaymentMethod(paymentMethod, managed.getPaymentMethod()));
            managed.setStatus(ServiceOrderStatus.IN_PROGRESS);
            em.merge(managed);
        }
    }

    public void updatePaymentMethod(ServiceOrderEntity os, String paymentMethod) {
        ServiceOrderEntity managed = em.find(ServiceOrderEntity.class, os.getId());
        if (managed == null) {
            return;
        }
        if (managed.getStatus() == ServiceOrderStatus.OPEN
                || managed.getStatus() == ServiceOrderStatus.IN_PROGRESS) {
            managed.setPaymentMethod(normalizePaymentMethod(paymentMethod));
            em.merge(managed);
        }
    }

    public void complete(ServiceOrderEntity os, String paymentMethod,
                         CashFlowFacade cashFlowFacade, UserEntity registeredBy) {
        ServiceOrderEntity managed = em.find(ServiceOrderEntity.class, os.getId());
        if (managed == null) {
            return;
        }
        if (managed.getStatus() != ServiceOrderStatus.OPEN
                && managed.getStatus() != ServiceOrderStatus.IN_PROGRESS) {
            return;
        }
        String resolvedPaymentMethod = resolvePaymentMethod(paymentMethod, managed.getPaymentMethod());
        managed.setPaymentMethod(resolvedPaymentMethod);
        managed.setStatus(ServiceOrderStatus.COMPLETED);
        managed.setCompletedAt(LocalDateTime.now());
        em.merge(managed);

        var entry = new br.upf.serviceorders.entity.CashFlowEntity();
        entry.setType(br.upf.serviceorders.enums.CashFlowType.INCOME);
        entry.setServiceOrder(managed);
        entry.setAmount(managed.getTotalAmount());
        entry.setDescription("OS Concluida #" + managed.getNumber());
        entry.setPaymentMethod(resolvedPaymentMethod);
        entry.setTransactionDate(LocalDateTime.now());
        if (registeredBy != null && registeredBy.getId() != null) {
            entry.setCreatedBy(em.getReference(UserEntity.class, registeredBy.getId()));
        }
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
                + "LEFT JOIN FETCH o.user "
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

    public long countByClientId(Long clientId) {
        if (clientId == null) {
            return 0;
        }
        return em.createQuery(
                "SELECT COUNT(o) FROM ServiceOrderEntity o WHERE o.client.id = :clientId",
                Long.class)
                .setParameter("clientId", clientId)
                .getSingleResult();
    }

    private String generateNumber() {
        Long seq = em.createQuery(
                "SELECT COALESCE(MAX(o.id), 0) + 1 FROM ServiceOrderEntity o", Long.class)
                .getSingleResult();
        return String.format("OS-%04d", seq);
    }

    private String resolvePaymentMethod(String requested, String current) {
        String normalized = normalizePaymentMethod(requested);
        return normalized != null ? normalized : current;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null) {
            return null;
        }
        String trimmed = paymentMethod.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
