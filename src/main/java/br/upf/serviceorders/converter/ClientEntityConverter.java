package br.upf.serviceorders.converter;

import br.upf.serviceorders.entity.ClientEntity;
import br.upf.serviceorders.facade.ClientFacade;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;

@FacesConverter(value = "clientEntityConverter", managed = true)
public class ClientEntityConverter implements Converter<ClientEntity> {

    @Inject
    private ClientFacade clientFacade;

    @Override
    public ClientEntity getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return clientFacade.find(Long.valueOf(value));
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, ClientEntity value) {
        if (value == null || value.getId() == null) {
            return "";
        }
        return String.valueOf(value.getId());
    }
}
