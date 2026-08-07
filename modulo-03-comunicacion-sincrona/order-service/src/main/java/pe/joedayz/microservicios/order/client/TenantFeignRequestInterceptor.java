package pe.joedayz.microservicios.order.client;

import pe.joedayz.microservicios.order.tenant.TenantContext;
import pe.joedayz.microservicios.order.tenant.TenantWebFilter;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

@Component
public class TenantFeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        template.header(TenantWebFilter.TENANT_HEADER, TenantContext.require());
    }
}
