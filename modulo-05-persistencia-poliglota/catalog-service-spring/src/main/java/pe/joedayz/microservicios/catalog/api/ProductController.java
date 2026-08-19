package pe.joedayz.microservicios.catalog.api;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.joedayz.microservicios.catalog.api.dto.ProductRequest;
import pe.joedayz.microservicios.catalog.api.dto.ProductResponse;
import pe.joedayz.microservicios.catalog.service.ProductService;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productService.listProducts();
    }

    @GetMapping("/{sku}")
    public ProductResponse getBySku(@PathVariable String sku) {
        return productService.getProductBySku(sku);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }
}
