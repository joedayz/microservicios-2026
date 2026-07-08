package pe.joedayz.microservicios.modulo01.solid.dip;

public class OrderService {

    private OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public void processOrder(Order order) {
        // Lógica de procesamiento de la orden
        System.out.println("Procesando orden...");
        repository.saveOrder(order);
    }
}
