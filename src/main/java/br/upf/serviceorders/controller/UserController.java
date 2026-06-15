package br.upf.serviceorders.controller;

import br.upf.serviceorders.entity.UserEntity;
import br.upf.serviceorders.facade.UserFacade;
import br.upf.filter.AuthFilter;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Named(value = "userController")
@SessionScoped
public class UserController implements Serializable {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    @EJB
    private UserFacade userFacade;

    private UserEntity user;
    private List<UserEntity> list;
    private UserEntity selected;
    private String loginEmail;
    private String loginPassword;
    private UserEntity loggedUser;

    private String registerName;
    private String registerEmail;
    private String registerPassword;
    private String registerPasswordConfirm;

    @PostConstruct
    public void init() {
        user = new UserEntity();
        list = new ArrayList<>();
        findAll();
        restoreLoggedUser();
    }

    public void prepareCreate() {
        user = new UserEntity();
    }

    public void prepareRegister() {
        registerName = null;
        registerEmail = null;
        registerPassword = null;
        registerPasswordConfirm = null;
    }

    public void create() {
        userFacade.create(user);
        findAll();
        prepareCreate();
    }

    public void edit() {
        userFacade.edit(selected);
        findAll();
    }

    public void delete() {
        userFacade.remove(selected);
        findAll();
    }

    public void findAll() {
        list = userFacade.findAll();
    }

    public String login() {
        String email = loginEmail == null ? "" : loginEmail.trim();
        String password = loginPassword == null ? "" : loginPassword;

        loggedUser = userFacade.authenticate(email, password)
                .orElse(null);

        if (loggedUser == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Não foi possível entrar",
                            "E-mail ou senha inválidos, ou usuário inativo."));
            return null;
        }

        loginPassword = null;
        storeAuthenticatedUser(loggedUser);
        return "/dashboard.xhtml?faces-redirect=true";
    }

    public String register() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (!validateRegistration(context)) {
            return null;
        }

        UserEntity newUser = new UserEntity();
        newUser.setName(registerName.trim());
        newUser.setEmail(registerEmail.trim().toLowerCase());
        newUser.setPassword(registerPassword);
        newUser.setActive(true);

        try {
            userFacade.create(newUser);
        } catch (EJBException ex) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Não foi possível concluir o cadastro.", null));
            return null;
        }

        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Conta criada com sucesso. Faça login para continuar.", null));
        prepareRegister();
        return "/login.xhtml?faces-redirect=true";
    }

    public String logout() {
        loggedUser = null;
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, Object> sessionMap = facesContext.getExternalContext().getSessionMap();
        sessionMap.remove(AuthFilter.AUTH_USER_ID);
        facesContext.getExternalContext().invalidateSession();
        return "/login.xhtml?faces-redirect=true";
    }

    public boolean isLoggedIn() {
        return loggedUser != null;
    }

    private boolean validateRegistration(FacesContext context) {
        boolean valid = true;

        if (registerName == null || registerName.isBlank()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Informe seu nome", null));
            valid = false;
        }

        String email = registerEmail == null ? "" : registerEmail.trim();
        if (email.isBlank()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Informe seu e-mail", null));
            valid = false;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "E-mail inválido", null));
            valid = false;
        } else if (userFacade.findByEmail(email).isPresent()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Este e-mail já está cadastrado", null));
            valid = false;
        }

        if (registerPassword == null || registerPassword.length() < 6) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "A senha deve ter no mínimo 6 caracteres", null));
            valid = false;
        }

        if (registerPasswordConfirm == null || !registerPasswordConfirm.equals(registerPassword)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "As senhas não conferem", null));
            valid = false;
        }

        return valid;
    }

    private void storeAuthenticatedUser(UserEntity user) {
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap()
                .put(AuthFilter.AUTH_USER_ID, user.getId());
    }

    private void restoreLoggedUser() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return;
        }
        Object userId = context.getExternalContext().getSessionMap().get(AuthFilter.AUTH_USER_ID);
        if (userId instanceof Long && loggedUser == null) {
            loggedUser = userFacade.find((Long) userId);
        }
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public List<UserEntity> getList() {
        return list;
    }

    public void setList(List<UserEntity> list) {
        this.list = list;
    }

    public UserEntity getSelected() {
        return selected;
    }

    public void setSelected(UserEntity selected) {
        this.selected = selected;
    }

    public String getLoginEmail() {
        return loginEmail;
    }

    public void setLoginEmail(String loginEmail) {
        this.loginEmail = loginEmail;
    }

    public String getLoginPassword() {
        return loginPassword;
    }

    public void setLoginPassword(String loginPassword) {
        this.loginPassword = loginPassword;
    }

    public UserEntity getLoggedUser() {
        return loggedUser;
    }

    public String getRegisterName() {
        return registerName;
    }

    public void setRegisterName(String registerName) {
        this.registerName = registerName;
    }

    public String getRegisterEmail() {
        return registerEmail;
    }

    public void setRegisterEmail(String registerEmail) {
        this.registerEmail = registerEmail;
    }

    public String getRegisterPassword() {
        return registerPassword;
    }

    public void setRegisterPassword(String registerPassword) {
        this.registerPassword = registerPassword;
    }

    public String getRegisterPasswordConfirm() {
        return registerPasswordConfirm;
    }

    public void setRegisterPasswordConfirm(String registerPasswordConfirm) {
        this.registerPasswordConfirm = registerPasswordConfirm;
    }
}
