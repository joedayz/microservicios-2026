package pe.joedayz.microservicios.catalog.service;

import java.util.List;
import java.util.NoSuchElementException;

import jakarta.transaction.Transactional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import pe.joedayz.microservicios.catalog.api.dto.ProductRequest;
import pe.joedayz.microservicios.catalog.api.dto.ProductResponse;
import pe.joedayz.microservicios.catalog.domain.Product;
import pe.joedayz.microservicios.catalog.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(cacheNames = "catalog-products", key = "T(pe.joedayz.microservicios.catalog.service.ProductCacheKeys).listKey()")
    public List<ProductResponse> listProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Cacheable(cacheNames = "catalog-product-by-sku", key = "T(pe.joedayz.microservicios.catalog.service.ProductCacheKeys).skuKey(#sku)")
    public ProductResponse getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(ProductResponse::from)
                .orElseThrow(() -> new NoSuchElementException("SKU no encontrado: " + sku));
    }

    @Transactional
    @CacheEvict(cacheNames = "catalog-products", key = "T(pe.joedayz.microservicios.catalog.service.ProductCacheKeys).listKey()")
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new IllegalArgumentException("El SKU ya existe: " + request.sku());
        }

        Product saved = productRepository.save(new Product(
                request.sku(),
                request.name(),
                request.description(),
                request.category(),
                request.price(),
                request.currency()));
        return ProductResponse.from(saved);
    }
}
