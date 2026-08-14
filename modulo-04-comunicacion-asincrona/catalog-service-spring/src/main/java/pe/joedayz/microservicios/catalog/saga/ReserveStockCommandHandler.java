package pe.joedayz.microservicios.catalog.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;
import pe.joedayz.microservicios.catalog.domain.ProductStock;
import pe.joedayz.microservicios.catalog.events.ReserveStockCommand;
import pe.joedayz.microservicios.catalog.events.StockReservationFailedEvent;
import pe.joedayz.microservicios.catalog.events.StockReservedEvent;
import pe.joedayz.microservicios.catalog.outbox.OutboxService;
import pe.joedayz.microservicios.catalog.repository.ProductStockRepository;

/**
 * Participante de la saga: reacciona al comando "reserve-stock-command"
 * emitido por el orquestador (Order Service). Cambio de stock + registro en
 * el outbox ocurren en la MISMA transacción: si la reserva se persiste, la
 * intención de publicar el reply también quedó garantizada.
 */
@Service
public class ReserveStockCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(ReserveStockCommandHandler.class);

    private final ProductStockRepository productStockRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public ReserveStockCommandHandler(ProductStockRepository productStockRepository,
                                       OutboxService outboxService,
                                       ObjectMapper objectMapper) {
        this.productStockRepository = productStockRepository;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "reserve-stock-command", groupId = "catalog-service")
    @Transactional
    public void handle(String payload) throws Exception {
        ReserveStockCommand command = objectMapper.readValue(payload, ReserveStockCommand.class);
        var stock = productStockRepository.findBySkuAndTenantId(command.sku(), command.tenantId());

        if (stock.isEmpty() || !stock.get().hasEnoughStock(command.quantity())) {
            String reason = stock.isEmpty() ? "SKU no encontrado: " + command.sku() : "Stock insuficiente";
            log.warn("Reserva fallida para order {} (sku={}): {}", command.orderId(), command.sku(), reason);
            outboxService.append("stock-reservation-failed", command.orderId(), command.orderId(),
                    command.tenantId(),
                    new StockReservationFailedEvent(command.orderId(), command.tenantId(),
                            command.sku(), command.quantity(), reason));
            return;
        }

        ProductStock productStock = stock.get();
        productStock.reserve(command.quantity());
        productStockRepository.save(productStock);

        outboxService.append("stock-reserved", command.orderId(), command.orderId(), command.tenantId(),
                new StockReservedEvent(command.orderId(), command.tenantId(),
                        command.sku(), command.quantity(), System.currentTimeMillis()));

        log.info("Stock reservado para order {} (sku={}, qty={})", command.orderId(), command.sku(), command.quantity());
    }
}
