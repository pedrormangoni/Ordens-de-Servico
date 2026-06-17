package br.upf.serviceorders.controller;

import br.upf.serviceorders.entity.ServiceEntity;
import br.upf.serviceorders.entity.UserEntity;
import br.upf.serviceorders.facade.ServiceFacade;
import br.upf.serviceorders.util.MonetaryAmounts;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.ConstraintViolationException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.primefaces.PrimeFaces;

@Named(value = "serviceController")
@SessionScoped
public class ServiceController implements Serializable {

    @EJB
    private ServiceFacade serviceFacade;

    @Inject
    private UserController userController;

    private ServiceEntity service;
    private List<ServiceEntity> list;
    private ServiceEntity selected;
    private String searchTerm;

    @PostConstruct
    public void init() {
        service = newService();
        list = new ArrayList<>();
        findAll();
    }

    public void prepareCreate() {
        service = newService();
    }

    public void prepareEdit(ServiceEntity item) {
        selected = item;
    }

    public void create() {
        if (!validateService(service)) {
            PrimeFaces.current().ajax().addCallbackParam("saved", false);
            return;
        }
        UserEntity loggedUser = userController.getLoggedUser();
        if (loggedUser == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Usuário não autenticado. Faça login novamente.");
            PrimeFaces.current().ajax().addCallbackParam("saved", false);
            return;
        }
        normalizeService(service);
        service.setCreatedBy(loggedUser);
        try {
            serviceFacade.create(service);
        } catch (EJBException ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, resolvePersistenceError(ex));
            PrimeFaces.current().ajax().addCallbackParam("saved", false);
            return;
        }
        addMessage(FacesMessage.SEVERITY_INFO, "Serviço salvo com sucesso.");
        findAll();
        prepareCreate();
        PrimeFaces.current().ajax().addCallbackParam("saved", true);
    }

    public void edit() {
        if (selected == null || !validateService(selected)) {
            PrimeFaces.current().ajax().addCallbackParam("saved", false);
            return;
        }
        normalizeService(selected);
        try {
            serviceFacade.edit(selected);
        } catch (EJBException ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, resolvePersistenceError(ex));
            PrimeFaces.current().ajax().addCallbackParam("saved", false);
            return;
        }
        addMessage(FacesMessage.SEVERITY_INFO, "Serviço atualizado com sucesso.");
        findAll();
        PrimeFaces.current().ajax().addCallbackParam("saved", true);
    }

    public void delete(ServiceEntity item) {
        serviceFacade.remove(item);
        if (selected != null && selected.equals(item)) {
            selected = null;
        }
        addMessage(FacesMessage.SEVERITY_INFO, "Serviço excluído.");
        findAll();
    }

    public void findAll() {
        list = serviceFacade.findAll();
    }

    public List<ServiceEntity> getFilteredList() {
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

    private boolean matchesSearch(ServiceEntity item, String term) {
        return contains(item.getName(), term)
                || contains(item.getDescription(), term)
                || contains(formatCreatedBy(item), term);
    }

    public String formatCreatedBy(ServiceEntity item) {
        if (item == null || item.getCreatedBy() == null || item.getCreatedBy().getName() == null) {
            return "—";
        }
        return item.getCreatedBy().getName();
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    private boolean validateService(ServiceEntity entity) {
        FacesContext context = FacesContext.getCurrentInstance();
        boolean valid = true;

        if (entity.getName() == null || entity.getName().isBlank()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Informe o nome do serviço", null));
            valid = false;
        }

        if (entity.getPrice() == null || !MonetaryAmounts.isWithinLimit(entity.getPrice())) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Informe um preço entre R$ 0,00 e R$ 99.999.999,99", null));
            valid = false;
        }

        return valid;
    }

    private void normalizeService(ServiceEntity entity) {
        if (entity.getName() != null) {
            entity.setName(entity.getName().trim());
        }
        entity.setDescription(trimToNull(entity.getDescription()));
        entity.setPrice(MonetaryAmounts.normalize(entity.getPrice()));
    }

    private ServiceEntity newService() {
        ServiceEntity entity = new ServiceEntity();
        entity.setActive(true);
        entity.setPrice(BigDecimal.ZERO);
        return entity;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void addMessage(FacesMessage.Severity severity, String summary) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, null));
    }

    private String resolvePersistenceError(EJBException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof ConstraintViolationException) {
            ConstraintViolationException violation = (ConstraintViolationException) cause;
            return violation.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .findFirst()
                    .orElse("Dados inválidos para salvar o serviço.");
        }
        if (MonetaryAmounts.isNumericOverflow(ex)) {
            return "O preço informado excede o limite permitido (R$ 99.999.999,99).";
        }
        return "Erro ao salvar no banco. Verifique a conexão e os dados informados.";
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public ServiceEntity getService() {
        return service;
    }

    public void setService(ServiceEntity service) {
        this.service = service;
    }

    public List<ServiceEntity> getList() {
        return list;
    }

    public void setList(List<ServiceEntity> list) {
        this.list = list;
    }

    public ServiceEntity getSelected() {
        return selected;
    }

    public void setSelected(ServiceEntity selected) {
        this.selected = selected;
    }
}
