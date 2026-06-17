package br.upf.serviceorders.controller;

import br.upf.serviceorders.entity.CashFlowEntity;
import br.upf.serviceorders.entity.UserEntity;
import br.upf.serviceorders.enums.CashFlowType;
import br.upf.serviceorders.facade.CashFlowFacade;
import br.upf.serviceorders.util.MonetaryAmounts;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
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

    @Inject
    private UserController userController;

    private CashFlowEntity cashFlow;
    private List<CashFlowEntity> list;
    private String searchTerm;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;

    @PostConstruct
    public void init() {
        cashFlow = new CashFlowEntity();
        list = new ArrayList<>();
        periodStartDate = null;
        periodEndDate = null;
    }

    public void refresh() {
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
        UserEntity loggedUser = userController.getLoggedUser();
        if (loggedUser == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Usuário não autenticado. Faça login novamente.");
            PrimeFaces.current().ajax().addCallbackParam("saved", false);
            return;
        }
        normalizeCashFlow(cashFlow);
        cashFlow.setCreatedBy(loggedUser);
        try {
            cashFlowFacade.create(cashFlow);
        } catch (EJBException ex) {
            if (MonetaryAmounts.isNumericOverflow(ex)) {
                addMessage(FacesMessage.SEVERITY_ERROR,
                        "O valor informado excede o limite permitido (R$ 99.999.999,99).");
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar o lançamento.");
            }
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
        list = cashFlowFacade.findFiltered(periodStartDate, periodEndDate);
    }

    public void applyPeriodFilter() {
        if (periodStartDate != null && periodEndDate != null && periodStartDate.isAfter(periodEndDate)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "A data inicial não pode ser maior que a final.");
            return;
        }
        list = cashFlowFacade.findFiltered(periodStartDate, periodEndDate);
    }

    public void clearPeriodFilter() {
        periodStartDate = null;
        periodEndDate = null;
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

    public String formatCreatedBy(CashFlowEntity item) {
        if (item.getCreatedBy() == null || item.getCreatedBy().getName() == null) {
            return "—";
        }
        return item.getCreatedBy().getName();
    }

    private boolean matchesSearch(CashFlowEntity item, String term) {
        return contains(item.getDescription(), term)
                || contains(item.getPaymentMethod(), term)
                || contains(formatType(item.getType()), term)
                || contains(formatServiceOrder(item), term)
                || contains(formatCreatedBy(item), term)
                || (item.getAmount() != null && item.getAmount().toPlainString().contains(term));
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    private BigDecimal sumByType(CashFlowType type) {
        List<CashFlowEntity> source = getFilteredList();
        if (source.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return source.stream()
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

        if (entity.getAmount() == null || !MonetaryAmounts.isPositiveWithinLimit(entity.getAmount())) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Informe um valor entre R$ 0,01 e R$ 99.999.999,99", null));
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
        entity.setAmount(MonetaryAmounts.normalize(entity.getAmount()));
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
