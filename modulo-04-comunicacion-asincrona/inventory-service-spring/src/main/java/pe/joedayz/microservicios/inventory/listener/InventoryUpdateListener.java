package pe.joedayz.microservicios.inventory.listener;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.joedayz.microservicios.inventory.domain.InventoryItem;
import pe.joedayz.microservicios.inventory.events.InventoryUpdatedEvent;
import pe.joedayz.microservicios.inventory.events.StockReservedEvent;
import pe.joedayz.microservicios.inventory.repository.InventoryItemRepository;

/**
 * Participante pasivo de la saga: no responde al orquestador, solo reacciona
 * a "stock-reserved" para mantener el inventario físico sincronizado con la
 * reserva lógica hecha en Catalog Service.
 */
@Service
public class InventoryUpdateListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryUpdateListener.class);

    private final InventoryItemRepository inventoryItemRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryUpdateListener(InventoryItemRepository inventoryItemRepository,
                                    KafkaTemplate<String, Object> kafkaTemplate) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "stock-reserved", groupId = "inventory-service")
    @Transactional
    public void handle(StockReservedEvent event) {
        InventoryItem item = inventoryItemRepository.findBySkuAndTenantId(event.sku(), event.tenantId())
                .orElseThrow(() -> new NoSuchElementException("SKU no encontrado en inventario: " + event.sku()));

        item.decrement(event.quantity());
        inventoryItemRepository.save(item);

        InventoryUpdatedEvent updated = new InventoryUpdatedEvent(event.orderId(), event.tenantId(),
                event.sku(), item.getPhysicalQuantity(), System.currentTimeMillis());
        kafkaTemplate.send("inventory-updated", event.orderId(), updated);

        log.info("Inventario actualizado para sku {} (order={}): quedan {}",
                event.sku(), event.orderId(), item.getPhysicalQuantity());
    }
}
