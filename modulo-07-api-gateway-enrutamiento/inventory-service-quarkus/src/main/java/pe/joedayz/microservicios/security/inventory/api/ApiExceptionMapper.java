package pe.joedayz.microservicios.security.inventory.api;

import java.util.NoSuchElementException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof NoSuchElementException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorBody(exception.getMessage()))
                    .build();
        }
        if (exception instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorBody(exception.getMessage()))
                    .build();
        }
        if (exception instanceof jakarta.ws.rs.WebApplicationException webApplicationException) {
            return webApplicationException.getResponse();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorBody("Error interno"))
                .build();
    }

    public record ErrorBody(String message) {
    }
}
