package br.upf.serviceorders.converter;

import br.upf.serviceorders.entity.ServiceEntity;
import br.upf.serviceorders.facade.ServiceFacade;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;

@FacesConverter(value = "serviceEntityConverter", managed = true)
public class ServiceEntityConverter implements Converter<ServiceEntity> {

    @Inject
    private ServiceFacade serviceFacade;

    @Override
    public ServiceEntity getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return serviceFacade.find(Long.valueOf(value));
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, ServiceEntity value) {
        if (value == null || value.getId() == null) {
            return "";
        }
        return String.valueOf(value.getId());
    }
}
