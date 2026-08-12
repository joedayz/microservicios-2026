package pe.joedayz.microservicios.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.joedayz.microservicios.order.domain.Order;

public interface OrderRepository extends JpaRepository<Order, String> {
}
