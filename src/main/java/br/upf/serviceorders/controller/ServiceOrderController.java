package br.upf.serviceorders.controller;

import br.upf.serviceorders.entity.ClientEntity;
import br.upf.serviceorders.entity.ServiceEntity;
import br.upf.serviceorders.entity.ServiceOrderEntity;
import br.upf.serviceorders.entity.ServiceOrderItemEntity;
import br.upf.serviceorders.entity.UserEntity;
import br.upf.serviceorders.enums.ServiceOrderStatus;
import br.upf.serviceorders.facade.CashFlowFacade;
import br.upf.serviceorders.facade.ClientFacade;
import br.upf.serviceorders.facade.ServiceFacade;
import br.upf.serviceorders.facade.ServiceOrderFacade;
import br.upf.serviceorders.facade.ServiceOrderItemFacade;
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;

@Named(value = "serviceOrderController")
@SessionScoped
public class ServiceOrderController implements Serializable {

    @EJB
    private ServiceOrderFacade serviceOrderFacade;

    @EJB
    private ServiceOrderItemFacade serviceOrderItemFacade;

    @EJB
    private CashFlowFacade cashFlowFacade;

    @EJB
    private ServiceFacade serviceFacade;

    @EJB
    private ClientFacade clientFacade;

    @Inject
    private UserController userController;

    private ServiceOrderEntity serviceOrder;
    private List<ServiceOrderEntity> list;
    private ServiceOrderEntity selected;
    private List<ServiceOrderItemEntity> selectedItems;

    private ClientEntity client;
    private List<ClientEntity> clients;
    private List<ServiceEntity> activeServices;
    private ServiceEntity selectedService;
    private BigDecimal itemQuantity;
    private List<ServiceOrderItemEntity> items;
    private String searchTerm;
    private ServiceOrderStatus statusFilter;

    @PostConstruct
    public void init() {
        serviceOrder = new ServiceOrderEntity();
        list = new ArrayList<>();
        items = new ArrayList<>();
        selectedItems = new ArrayList<>();
        itemQuantity = BigDecimal.ONE;
        clients = new ArrayList<>();
        activeServices = new ArrayList<>();
        findAll();
    }

    public void onRowSelect(SelectEvent<ServiceOrderEntity> event) {
        openDetails(event.getObject());
    }

    public void onRowUnselect(org.primefaces.event.UnselectEvent<ServiceOrderEntity> event) {
        selected = null;
        selectedItems = Collections.emptyList();
    }

    public boolean hasSelection() {
        return selected != null;
    }

    public void openDetails(ServiceOrderEntity item) {
        if (item == null || item.getId() == null) {
            return;
        }
        selected = serviceOrderFacade.findDetail(item.getId());
        selectedItems = serviceOrderItemFacade.findByServiceOrder(selected);
    }

    private void refreshSelected() {
        if (selected != null && selected.getId() != null) {
            selected = serviceOrderFacade.findDetail(selected.getId());
            selectedItems = serviceOrderItemFacade.findByServiceOrder(selected);
        }
    }

    public void prepareCreate() {
        serviceOrder = new ServiceOrderEntity();
        items = new ArrayList<>();
        client = null;
        selectedService = null;
        itemQuantity = BigDecimal.ONE;
        clients = clientFacade.findAll();
        activeServices = serviceFacade.findActive();
    }

    public void addServiceItem() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (selectedService == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Selecione um serviço", null));
            return;
        }
        if (itemQuantity == null || !MonetaryAmounts.isPositiveWithinLimit(itemQuantity)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Informe uma quantidade entre 0,01 e 99.999.999,99", null));
            return;
        }

        ServiceEntity service = serviceFacade.find(selectedService.getId());
        if (service == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Serviço não encontrado", null));
            return;
        }

        BigDecimal unitPrice = MonetaryAmounts.normalize(service.getPrice());
        if (!MonetaryAmounts.isWithinLimit(unitPrice)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "O preço do serviço \"" + service.getName()
                    + "\" é inválido. Edite o cadastro do serviço.", null));
            return;
        }

        BigDecimal quantity = MonetaryAmounts.normalize(itemQuantity);
        BigDecimal lineTotal = MonetaryAmounts.lineTotal(unitPrice, quantity);
        if (!MonetaryAmounts.isWithinLimit(lineTotal)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "O subtotal deste item excede o limite permitido (R$ 99.999.999,99).", null));
            return;
        }

        BigDecimal projectedTotal = getItemsTotal().add(lineTotal);
        if (!MonetaryAmounts.isWithinLimit(projectedTotal)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "O total da ordem excederia o limite permitido (R$ 99.999.999,99).", null));
            return;
        }

        ServiceOrderItemEntity item = new ServiceOrderItemEntity();
        item.setService(service);
        item.setQuantity(quantity);
        item.setUnit_price(unitPrice);
        item.setTotal_price(lineTotal);
        items.add(item);

        selectedService = null;
        itemQuantity = BigDecimal.ONE;
    }

    public void removeServiceItem(ServiceOrderItemEntity item) {
        if (items != null) {
            items.remove(item);
        }
    }

    public void create() {
        PrimeFaces.current().ajax().addCallbackParam("saved", false);
        FacesContext context = FacesContext.getCurrentInstance();
        if (client == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Selecione um cliente", null));
            return;
        }
        if (items == null || items.isEmpty()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Adicione ao menos um serviço à ordem", null));
            return;
        }
        if (!MonetaryAmounts.isWithinLimit(getItemsTotal())) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "O total da ordem excede o limite permitido (R$ 99.999.999,99).", null));
            return;
        }

        UserEntity loggedUser = userController.getLoggedUser();
        if (loggedUser == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Usuário não autenticado. Faça login novamente.", null));
            return;
        }

        try {
            serviceOrderFacade.open(client, loggedUser, serviceOrder.getDescription(), items);
        } catch (EJBException ex) {
            String message = MonetaryAmounts.isNumericOverflow(ex)
                    ? "O total da ordem excede o limite permitido (R$ 99.999.999,99). "
                    + "Verifique os preços dos serviços cadastrados."
                    : "Erro ao salvar a ordem de serviço.";
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
            return;
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Ordem de serviço criada com sucesso.", null));
        findAll();
        prepareCreate();
        PrimeFaces.current().ajax().addCallbackParam("saved", true);
    }

    public void prepareEdit() {
        refreshSelected();
    }

    public void edit() {
        serviceOrderFacade.edit(selected);
        findAll();
        refreshSelected();
        addMessage(FacesMessage.SEVERITY_INFO, "Ordem atualizada com sucesso.");
    }

    public void deleteSelected() {
        if (selected == null) {
            return;
        }
        if (!canDelete()) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Não é possível excluir esta ordem porque existem lançamentos no fluxo de caixa vinculados.");
            return;
        }
        try {
            serviceOrderFacade.remove(selected);
        } catch (EJBException ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, resolveDeleteError(ex));
            return;
        }
        selected = null;
        selectedItems = Collections.emptyList();
        findAll();
        addMessage(FacesMessage.SEVERITY_INFO, "Ordem excluída com sucesso.");
    }

    public boolean canDelete() {
        if (selected == null || selected.getId() == null) {
            return false;
        }
        return cashFlowFacade.countByServiceOrderId(selected.getId()) == 0;
    }

    public void startProgressSelected() {
        if (selected == null) {
            return;
        }
        serviceOrderFacade.startProgress(selected, selected.getPaymentMethod());
        findAll();
        refreshSelected();
        addMessage(FacesMessage.SEVERITY_INFO, "Ordem marcada como em andamento.");
    }

    public void savePaymentMethod() {
        if (selected == null || !isActiveStatus()) {
            return;
        }
        serviceOrderFacade.updatePaymentMethod(selected, selected.getPaymentMethod());
        refreshSelected();
    }

    public void completeSelected() {
        if (selected == null) {
            return;
        }
        serviceOrderFacade.complete(selected, selected.getPaymentMethod(), cashFlowFacade, userController.getLoggedUser());
        findAll();
        refreshSelected();
        addMessage(FacesMessage.SEVERITY_INFO, "Ordem concluída e registrada no fluxo de caixa.");
    }

    public void cancelSelected() {
        if (selected == null) {
            return;
        }
        serviceOrderFacade.cancel(selected);
        findAll();
        refreshSelected();
        addMessage(FacesMessage.SEVERITY_INFO, "Ordem cancelada.");
    }

    public void findAll() {
        list = serviceOrderFacade.findAllOrdered();
    }

    public List<ServiceOrderEntity> getFilteredList() {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .filter(this::matchesStatusFilter)
                .filter(this::matchesSearchFilter)
                .collect(Collectors.toList());
    }

    public void onStatusFilterChange() {
        selected = null;
        selectedItems = Collections.emptyList();
    }

    public ServiceOrderStatus[] getStatusOptions() {
        return ServiceOrderStatus.values();
    }

    private boolean matchesStatusFilter(ServiceOrderEntity item) {
        if (statusFilter == null) {
            return true;
        }
        return item.getStatus() == statusFilter;
    }

    private boolean matchesSearchFilter(ServiceOrderEntity item) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return true;
        }
        return matchesSearch(item, searchTerm.toLowerCase().trim());
    }

    public boolean isActiveStatus() {
        return selected != null
                && (selected.getStatus() == ServiceOrderStatus.OPEN
                || selected.getStatus() == ServiceOrderStatus.IN_PROGRESS);
    }

    public boolean canStartProgress() {
        return selected != null && selected.getStatus() == ServiceOrderStatus.OPEN;
    }

    public boolean canComplete() {
        return isActiveStatus();
    }

    public boolean canCancel() {
        return isActiveStatus();
    }

    public boolean canEdit() {
        return isActiveStatus();
    }

    public String getStatusSeverity(ServiceOrderStatus status) {
        if (status == null) {
            return "secondary";
        }
        if (status == ServiceOrderStatus.OPEN) {
            return "info";
        }
        if (status == ServiceOrderStatus.IN_PROGRESS) {
            return "warn";
        }
        if (status == ServiceOrderStatus.COMPLETED) {
            return "success";
        }
        if (status == ServiceOrderStatus.CANCELLED) {
            return "danger";
        }
        return "secondary";
    }

    private boolean matchesSearch(ServiceOrderEntity item, String term) {
        return contains(item.getNumber(), term)
                || contains(formatOrderLabel(item), term)
                || (item.getClient() != null && contains(item.getClient().getName(), term))
                || contains(item.getDescription(), term)
                || contains(formatStatus(item.getStatus()), term)
                || contains(formatCreatedBy(item), term)
                || (item.getStatus() != null && contains(item.getStatus().name(), term));
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    private void addMessage(FacesMessage.Severity severity, String summary) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, null));
    }

    private String resolveDeleteError(EJBException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && (message.contains("fk_cash_flow_service_order")
                    || message.toLowerCase().contains("foreign key")
                    || message.toLowerCase().contains("chave estrangeira")
                    || message.toLowerCase().contains("still referenced")
                    || message.toLowerCase().contains("ainda é referenciada")
                    || message.toLowerCase().contains("ainda e referenciada"))) {
                return "Não é possível excluir esta ordem porque existem lançamentos no fluxo de caixa vinculados.";
            }
            cause = cause.getCause();
        }
        return "Erro ao excluir a ordem de serviço.";
    }

    public void findByStatus(ServiceOrderStatus status) {
        list = serviceOrderFacade.findByStatus(status);
    }

    public void findByClient(ClientEntity clientFilter) {
        list = serviceOrderFacade.findByClient(clientFilter);
    }

    public BigDecimal getItemsTotal() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(i -> i.getTotal_price() != null ? i.getTotal_price() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String formatServiceOption(ServiceEntity service) {
        if (service == null || service.getPrice() == null) {
            return service != null ? service.getName() : "";
        }
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return service.getName() + " - " + currency.format(service.getPrice());
    }

    public String formatStatus(ServiceOrderStatus status) {
        if (status == null) {
            return "";
        }
        if (status == ServiceOrderStatus.OPEN) {
            return "Aberta";
        }
        if (status == ServiceOrderStatus.IN_PROGRESS) {
            return "Em andamento";
        }
        if (status == ServiceOrderStatus.COMPLETED) {
            return "Concluída";
        }
        if (status == ServiceOrderStatus.CANCELLED) {
            return "Cancelada";
        }
        return status.name();
    }

    public String formatOrderLabel(ServiceOrderEntity item) {
        if (item == null || item.getNumber() == null || item.getNumber().isBlank()) {
            return item != null && item.getId() != null ? "#" + item.getId() : "—";
        }
        return item.getNumber();
    }

    public String formatCreatedBy(ServiceOrderEntity item) {
        if (item == null || item.getUser() == null || item.getUser().getName() == null) {
            return "—";
        }
        return item.getUser().getName();
    }

    public ServiceOrderEntity getServiceOrder() {
        return serviceOrder;
    }

    public void setServiceOrder(ServiceOrderEntity serviceOrder) {
        this.serviceOrder = serviceOrder;
    }

    public List<ServiceOrderEntity> getList() {
        return list;
    }

    public void setList(List<ServiceOrderEntity> list) {
        this.list = list;
    }

    public ServiceOrderEntity getSelected() {
        return selected;
    }

    public void setSelected(ServiceOrderEntity selected) {
        this.selected = selected;
    }

    public List<ServiceOrderItemEntity> getSelectedItems() {
        return selectedItems;
    }

    public ClientEntity getClient() {
        return client;
    }

    public void setClient(ClientEntity client) {
        this.client = client;
    }

    public List<ClientEntity> getClients() {
        return clients;
    }

    public List<ServiceEntity> getActiveServices() {
        return activeServices;
    }

    public ServiceEntity getSelectedService() {
        return selectedService;
    }

    public void setSelectedService(ServiceEntity selectedService) {
        this.selectedService = selectedService;
    }

    public BigDecimal getItemQuantity() {
        return itemQuantity;
    }

    public void setItemQuantity(BigDecimal itemQuantity) {
        this.itemQuantity = itemQuantity;
    }

    public List<ServiceOrderItemEntity> getItems() {
        return items;
    }

    public void setItems(List<ServiceOrderItemEntity> items) {
        this.items = items;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public ServiceOrderStatus getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(ServiceOrderStatus statusFilter) {
        this.statusFilter = statusFilter;
    }
}
