package pe.joedayz.microservicios.modulo01.tenant;

/**
 * Transporta el tenant activo durante el procesamiento de una peticion.
 *
 * <p>En un microservicio real, un filtro/interceptor lee el {@code tenant_id} del
 * header {@code X-Tenant-ID} o de un claim del JWT al inicio del request y lo guarda
 * aqui; a partir de ese momento cualquier query, evento o clave de cache lo usa
 * automaticamente. Al terminar el request se limpia con {@link #clear()}.
 *
 * <p>Usamos un {@link ThreadLocal} para no tener que pasar el TenantId por parametro
 * en cada metodo. En un stack reactivo (WebFlux) se usaria el Reactor Context.
 */
public final class TenantContext {

    private static final ThreadLocal<TenantId> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(TenantId tenantId) {
        CURRENT.set(tenantId);
    }

    /**
     * @return el tenant activo; falla si no hay ninguno (evita fugas de datos por olvido).
     */
    public static TenantId require() {
        TenantId tenant = CURRENT.get();
        if (tenant == null) {
            throw new IllegalStateException(
                    "No hay tenant en el contexto: toda operacion multi-tenant requiere un TenantId");
        }
        return tenant;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
