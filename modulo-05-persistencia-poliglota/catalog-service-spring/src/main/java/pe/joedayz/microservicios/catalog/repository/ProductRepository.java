package pe.joedayz.microservicios.catalog.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.joedayz.microservicios.catalog.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);
}
