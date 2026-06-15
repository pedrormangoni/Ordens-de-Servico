package br.upf.serviceorders.controller;

import br.upf.serviceorders.entity.CashFlowEntity;
import br.upf.serviceorders.enums.CashFlowType;
import br.upf.serviceorders.facade.CashFlowFacade;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.primefaces.PrimeFaces;

@Named(value = "cashFlowController")
@SessionScoped
public class CashFlowController implements Serializable {

    @EJB
    private CashFlowFacade cashFlowFacade;

    private CashFlowEntity cashFlow;
    private List<CashFlowEntity> list;
    private String searchTerm;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;

    @PostConstruct
    public void init() {
        cashFlow = new CashFlowEntity();
        list = new ArrayList<>();
        resetPeriodDates();
        applyPeriodFilter();
    }

    public void prepareCreate() {
        cashFlow = new CashFlowEntity();
        cashFlow.setType(CashFlowType.INCOME);
        cashFlow.setTransactionDate(LocalDateTime.now());
    }

    public void create() {
        if (!validateCashFlow(cashFlow)) {
            PrimeFaces.current().ajax().addCallbackParam("saved", false);
            return;
        }
        normalizeCashFlow(cashFlow);
        try {
            cashFlowFacade.create(cashFlow);
        } catch (EJBException ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar o lançamento.");
            PrimeFaces.current().ajax().addCallbackParam("saved", false);
            return;
        }
        addMessage(FacesMessage.SEVERITY_INFO, "Lançamento registrado com sucesso.");
        applyPeriodFilter();
        prepareCreate();
        PrimeFaces.current().ajax().addCallbackParam("saved", true);
    }

    public void delete(CashFlowEntity item) {
        if (item.getServiceOrder() != null) {
            addMessage(FacesMessage.SEVERITY_WARN,
                    "Lançamentos vinculados a ordens de serviço não podem ser excluídos.");
            return;
        }
        cashFlowFacade.remove(item);
        addMessage(FacesMessage.SEVERITY_INFO, "Lançamento excluído.");
        applyPeriodFilter();
    }

    public void findAll() {
        list = cashFlowFacade.findAllOrdered();
    }

    public void applyPeriodFilter() {
        if (periodStartDate != null && periodEndDate != null) {
            if (periodStartDate.isAfter(periodEndDate)) {
                addMessage(FacesMessage.SEVERITY_ERROR, "A data inicial não pode ser maior que a final.");
                return;
            }
            list = cashFlowFacade.findByPeriod(
                    periodStartDate.atStartOfDay(),
                    periodEndDate.atTime(23, 59, 59));
        } else {
            findAll();
        }
    }

    public void clearPeriodFilter() {
        resetPeriodDates();
        searchTerm = null;
        applyPeriodFilter();
    }

    public List<CashFlowEntity> getFilteredList() {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        if (searchTerm == null || searchTerm.isBlank()) {
            return list;
        }
        String term = searchTerm.toLowerCase().trim();
        return list.stream()
                .filter(item -> matchesSearch(item, term))
                .collect(Collectors.toList());
    }

    public BigDecimal getFilteredIncome() {
        return sumByType(CashFlowType.INCOME);
    }

    public BigDecimal getFilteredExpense() {
        return sumByType(CashFlowType.EXPENSE);
    }

    public BigDecimal getFilteredNetBalance() {
        return getFilteredIncome().subtract(getFilteredExpense());
    }

    public String formatType(CashFlowType type) {
        if (type == CashFlowType.INCOME) {
            return "Entrada";
        }
        if (type == CashFlowType.EXPENSE) {
            return "Saída";
        }
        return "";
    }

    public String formatServiceOrder(CashFlowEntity item) {
        if (item.getServiceOrder() == null || item.getServiceOrder().getNumber() == null) {
            return "—";
        }
        return item.getServiceOrder().getNumber();
    }

    private boolean matchesSearch(CashFlowEntity item, String term) {
        return contains(item.getDescription(), term)
                || contains(item.getPaymentMethod(), term)
                || contains(formatType(item.getType()), term)
                || contains(formatServiceOrder(item), term);
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    private BigDecimal sumByType(CashFlowType type) {
        if (list == null || list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return list.stream()
                .filter(item -> item.getType() == type)
                .map(CashFlowEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean validateCashFlow(CashFlowEntity entity) {
        FacesContext context = FacesContext.getCurrentInstance();
        boolean valid = true;

        if (entity.getType() == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Selecione o tipo do lançamento", null));
            valid = false;
        }

        if (entity.getDescription() == null || entity.getDescription().isBlank()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Informe a descrição", null));
            valid = false;
        }

        if (entity.getAmount() == null || entity.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Informe um valor maior que zero", null));
            valid = false;
        }

        return valid;
    }

    private void normalizeCashFlow(CashFlowEntity entity) {
        entity.setDescription(entity.getDescription().trim());
        if (entity.getPaymentMethod() != null) {
            String method = entity.getPaymentMethod().trim();
            entity.setPaymentMethod(method.isEmpty() ? null : method);
        }
        if (entity.getTransactionDate() == null) {
            entity.setTransactionDate(LocalDateTime.now());
        }
    }

    private void resetPeriodDates() {
        periodStartDate = LocalDate.now().withDayOfMonth(1);
        periodEndDate = LocalDate.now();
    }

    private void addMessage(FacesMessage.Severity severity, String summary) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, null));
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

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public LocalDate getPeriodStartDate() {
        return periodStartDate;
    }

    public void setPeriodStartDate(LocalDate periodStartDate) {
        this.periodStartDate = periodStartDate;
    }

    public LocalDate getPeriodEndDate() {
        return periodEndDate;
    }

    public void setPeriodEndDate(LocalDate periodEndDate) {
        this.periodEndDate = periodEndDate;
    }
}
