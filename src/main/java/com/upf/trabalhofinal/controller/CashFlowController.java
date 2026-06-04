package com.upf.trabalhofinal.controller;

import com.upf.trabalhofinal.entity.CashFlowEntity;
import com.upf.trabalhofinal.enums.CashFlowType;
import com.upf.trabalhofinal.facade.CashFlowFacade;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Named(value = "cashFlowController")
@SessionScoped
public class CashFlowController implements Serializable {

    @EJB
    private CashFlowFacade cashFlowFacade;

    private CashFlowEntity cashFlow;
    private List<CashFlowEntity> list;
    private CashFlowEntity selected;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    @PostConstruct
    public void init() {
        cashFlow = new CashFlowEntity();
        list = new ArrayList<>();
        periodStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        periodEnd = LocalDateTime.now();
        findAll();
    }

    public void prepareCreate() {
        cashFlow = new CashFlowEntity();
    }

    public void create() {
        cashFlowFacade.create(cashFlow);
        findAll();
        prepareCreate();
    }

    public void edit() {
        cashFlowFacade.edit(selected);
        findAll();
    }

    public void delete() {
        cashFlowFacade.remove(selected);
        findAll();
    }

    public void findAll() {
        list = cashFlowFacade.findAll();
    }

    public void findByType(CashFlowType type) {
        list = cashFlowFacade.findByType(type);
    }

    public void findByPeriod() {
        list = cashFlowFacade.findByPeriod(periodStart, periodEnd);
    }

    public BigDecimal getIncomeBalance() {
        return cashFlowFacade.getBalanceByType(CashFlowType.INCOME);
    }

    public BigDecimal getExpenseBalance() {
        return cashFlowFacade.getBalanceByType(CashFlowType.EXPENSE);
    }

    public BigDecimal getNetBalance() {
        return getIncomeBalance().subtract(getExpenseBalance());
    }

    public CashFlowEntity getCashFlow() {
        return cashFlow;
    }

    public void setCashFlow(CashFlowEntity cashFlow) {
        this.cashFlow = cashFlow;
    }

    public List<CashFlowEntity> getList() {
        return list;
    }

    public void setList(List<CashFlowEntity> list) {
        this.list = list;
    }

    public CashFlowEntity getSelected() {
        return selected;
    }

    public void setSelected(CashFlowEntity selected) {
        this.selected = selected;
    }

    public LocalDateTime getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDateTime periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }
}
