package br.upf.serviceorders.controller;

import br.upf.serviceorders.entity.ClientEntity;
import br.upf.serviceorders.facade.ClientFacade;
import br.upf.serviceorders.facade.ServiceOrderFacade;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.primefaces.PrimeFaces;
import jakarta.ejb.EJBException;
import jakarta.validation.ConstraintViolationException;

@Named(value = "clientController")
@SessionScoped
public class ClientController implements Serializable {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern CPF_DIGITS_PATTERN = Pattern.compile("^\\d{11}$");

    @EJB
    private ClientFacade clientFacade;

    @EJB
    private ServiceOrderFacade serviceOrderFacade;

    private ClientEntity client;
    private List<ClientEntity> list;
    private ClientEntity selected;
    private String searchTerm;

    @PostConstruct
    public void init() {
        client = new ClientEntity();
        list = new ArrayList<>();
        findAll();
    }

    public void prepareCreate() {
        client = new ClientEntity();
    }

    public void prepareEdit(ClientEntity item) {
        selected = item;
    }

    public String formatCpfDisplay(String document) {
        String cpf = normalizeCpf(document);
        if (cpf == null || cpf.length() != 11) {
            return document;
        }
        return formatCpf(cpf);
    }

    public void create() {
        if (!validateClient(client)) {
            PrimeFaces.current().ajax().addCallbackParam("saved", false);
            return;
        }
        normalizeClient(client);
        try {
            clientFacade.create(client);
        } catch (EJBException ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, resolvePersistenceError(ex));
            PrimeFaces.current().ajax().addCallbackParam("saved", false);
            return;
        }
        addMessage(FacesMessage.SEVERITY_INFO, "Cliente salvo com sucesso.");
        findAll();
        prepareCreate();
        PrimeFaces.current().ajax().addCallbackParam("saved", true);
    }

    public void edit() {
        if (selected == null || !validateClient(selected)) {
            PrimeFaces.current().ajax().addCallbackParam("saved", false);
            return;
        }
        normalizeClient(selected);
        try {
            clientFacade.edit(selected);
        } catch (EJBException ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, resolvePersistenceError(ex));
            PrimeFaces.current().ajax().addCallbackParam("saved", false);
            return;
        }
        addMessage(FacesMessage.SEVERITY_INFO, "Cliente atualizado com sucesso.");
        findAll();
        PrimeFaces.current().ajax().addCallbackParam("saved", true);
    }

    public void delete(ClientEntity item) {
        if (item == null || item.getId() == null) {
            return;
        }
        if (!canDelete(item)) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Não é possível excluir este cliente porque existem ordens de serviço vinculadas.");
            return;
        }
        try {
            clientFacade.remove(item);
        } catch (EJBException ex) {
            addMessage(FacesMessage.SEVERITY_ERROR, resolvePersistenceError(ex));
            return;
        }
        if (selected != null && selected.equals(item)) {
            selected = null;
        }
        addMessage(FacesMessage.SEVERITY_INFO, "Cliente excluído.");
        findAll();
    }

    public boolean canDelete(ClientEntity item) {
        if (item == null || item.getId() == null) {
            return false;
        }
        return serviceOrderFacade.countByClientId(item.getId()) == 0;
    }

    public void findAll() {
        list = clientFacade.findAll();
    }

    private boolean validateClient(ClientEntity entity) {
        FacesContext context = FacesContext.getCurrentInstance();
        boolean valid = true;

        if (entity.getName() == null || entity.getName().isBlank()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Informe o nome do cliente", null));
            valid = false;
        }

        String cpf = normalizeCpf(entity.getDocument());
        if (cpf == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Informe o CPF do cliente", null));
            valid = false;
        } else if (cpf.length() != 11 || !CPF_DIGITS_PATTERN.matcher(cpf).matches() || !isValidCpf(cpf)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "CPF inválido. Informe 11 dígitos.", null));
            valid = false;
        } else {
            ClientEntity existing = clientFacade.findByDocument(cpf);
            if (existing != null && (entity.getId() == null || !existing.getId().equals(entity.getId()))) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "CPF já cadastrado para outro cliente", null));
                valid = false;
            }
        }

        String email = entity.getEmail();
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "E-mail inválido", null));
            valid = false;
        }

        return valid;
    }

    private void normalizeClient(ClientEntity entity) {
        if (entity.getName() != null) {
            entity.setName(entity.getName().trim());
        }
        entity.setDocument(normalizeCpf(entity.getDocument()));
        entity.setPhone(trimToNull(entity.getPhone()));
        entity.setEmail(trimToNull(entity.getEmail()));
        entity.setAddress(trimToNull(entity.getAddress()));
    }

    private String normalizeCpf(String cpf) {
        if (cpf == null) {
            return null;
        }
        String digits = cpf.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    private String formatCpf(String digits) {
        return digits.substring(0, 3) + "."
                + digits.substring(3, 6) + "."
                + digits.substring(6, 9) + "-"
                + digits.substring(9, 11);
    }

    private boolean isValidCpf(String cpf) {
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
        }
        int firstDigit = sum % 11;
        firstDigit = firstDigit < 2 ? 0 : 11 - firstDigit;
        if (Character.getNumericValue(cpf.charAt(9)) != firstDigit) {
            return false;
        }
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
        }
        int secondDigit = sum % 11;
        secondDigit = secondDigit < 2 ? 0 : 11 - secondDigit;
        return Character.getNumericValue(cpf.charAt(10)) == secondDigit;
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
                    .orElse("Dados inválidos para salvar o cliente.");
        }
        if (isDuplicateDocumentError(cause)) {
            return "CPF já cadastrado para outro cliente.";
        }
        if (isForeignKeyReferenceError(cause)) {
            return "Não é possível excluir este cliente porque existem ordens de serviço vinculadas.";
        }
        return "Erro ao salvar no banco. Verifique o schema.sql e a conexão com o PostgreSQL.";
    }

    private boolean isForeignKeyReferenceError(Throwable cause) {
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && (message.contains("fk_service_order_client")
                    || message.toLowerCase().contains("foreign key")
                    || message.toLowerCase().contains("chave estrangeira")
                    || message.toLowerCase().contains("still referenced")
                    || message.toLowerCase().contains("ainda é referenciada")
                    || message.toLowerCase().contains("ainda e referenciada"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean isDuplicateDocumentError(Throwable cause) {
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && (message.contains("uk_clients_document")
                    || message.contains("clients_document_key")
                    || message.toLowerCase().contains("duplicate")
                    || message.toLowerCase().contains("unique"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    public List<ClientEntity> getFilteredList() {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        if (searchTerm == null || searchTerm.isBlank()) {
            return list;
        }
        String term = searchTerm.toLowerCase().trim();
        return list.stream()
                .filter(client -> matchesSearch(client, term))
                .collect(Collectors.toList());
    }

    private boolean matchesSearch(ClientEntity client, String term) {
        return contains(client.getName(), term)
                || contains(client.getDocument(), term)
                || contains(client.getPhone(), term)
                || contains(client.getEmail(), term);
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public ClientEntity getClient() {
        return client;
    }

    public void setClient(ClientEntity client) {
        this.client = client;
    }

    public List<ClientEntity> getList() {
        return list;
    }

    public void setList(List<ClientEntity> list) {
        this.list = list;
    }

    public ClientEntity getSelected() {
        return selected;
    }

    public void setSelected(ClientEntity selected) {
        this.selected = selected;
    }
}
