package br.upf.serviceorders.controller;

import br.upf.serviceorders.entity.ServiceOrderEntity;
import br.upf.serviceorders.enums.CashFlowType;
import br.upf.serviceorders.enums.ServiceOrderStatus;
import br.upf.serviceorders.facade.CashFlowFacade;
import br.upf.serviceorders.facade.ClientFacade;
import br.upf.serviceorders.facade.ServiceOrderFacade;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Named(value = "dashboardController")
@ViewScoped
public class DashboardController implements Serializable {

    @EJB
    private ServiceOrderFacade serviceOrderFacade;

    @EJB
    private ClientFacade clientFacade;

    @EJB
    private CashFlowFacade cashFlowFacade;

    private long openOrdersCount;
    private long activeClientsCount;
    private long statusOpenCount;
    private long statusInProgressCount;
    private long statusCompletedCount;
    private long statusCancelledCount;
    private BigDecimal revenueTotal;
    private BigDecimal expenseTotal;
    private BigDecimal netBalance;
    private int completionRate;
    private List<ServiceOrderEntity> recentOrders;

    @PostConstruct
    public void init() {
        List<ServiceOrderEntity> orders = serviceOrderFacade.findAll();
        activeClientsCount = clientFacade.findAll().size();

        statusOpenCount = orders.stream()
                .filter(o -> o.getStatus() == ServiceOrderStatus.OPEN)
                .count();
        statusInProgressCount = orders.stream()
                .filter(o -> o.getStatus() == ServiceOrderStatus.IN_PROGRESS)
                .count();
        statusCompletedCount = orders.stream()
                .filter(o -> o.getStatus() == ServiceOrderStatus.COMPLETED)
                .count();
        statusCancelledCount = orders.stream()
                .filter(o -> o.getStatus() == ServiceOrderStatus.CANCELLED)
                .count();

        openOrdersCount = statusOpenCount + statusInProgressCount;

        revenueTotal = cashFlowFacade.getBalanceByType(CashFlowType.INCOME);
        expenseTotal = cashFlowFacade.getBalanceByType(CashFlowType.EXPENSE);
        netBalance = revenueTotal.subtract(expenseTotal);

        if (orders.isEmpty()) {
            completionRate = 0;
        } else {
            completionRate = (int) Math.round((statusCompletedCount * 100.0) / orders.size());
        }

        recentOrders = orders.stream()
                .sorted(Comparator.comparing(ServiceOrderEntity::getOpenedAt).reversed())
                .limit(5)
                .toList();
    }

    public long getOpenOrdersCount() {
        return openOrdersCount;
    }

    public long getActiveClientsCount() {
        return activeClientsCount;
    }

    public long getStatusOpenCount() {
        return statusOpenCount;
    }

    public long getStatusInProgressCount() {
        return statusInProgressCount;
    }

    public long getStatusCompletedCount() {
        return statusCompletedCount;
    }

    public long getStatusCancelledCount() {
        return statusCancelledCount;
    }

    public BigDecimal getRevenueTotal() {
        return revenueTotal;
    }

    public BigDecimal getExpenseTotal() {
        return expenseTotal;
    }

    public BigDecimal getNetBalance() {
        return netBalance;
    }

    public int getCompletionRate() {
        return completionRate;
    }

    public List<ServiceOrderEntity> getRecentOrders() {
        return recentOrders;
    }
}
