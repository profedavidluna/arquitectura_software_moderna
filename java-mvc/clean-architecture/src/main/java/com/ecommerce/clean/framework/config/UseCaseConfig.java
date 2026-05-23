package com.ecommerce.clean.framework.config;

import com.ecommerce.clean.usecase.*;
import com.ecommerce.clean.usecase.port.ProductGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FRAMEWORK LAYER - Spring Configuration.
 * Wires use cases with their dependencies.
 * Use cases have NO Spring annotations - they are pure Java.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public CreateProductUseCase createProductUseCase(ProductGateway gateway) {
        return new CreateProductUseCase(gateway);
    }

    @Bean
    public GetProductUseCase getProductUseCase(ProductGateway gateway) {
        return new GetProductUseCase(gateway);
    }

    @Bean
    public ListProductsUseCase listProductsUseCase(ProductGateway gateway) {
        return new ListProductsUseCase(gateway);
    }

    @Bean
    public UpdateProductUseCase updateProductUseCase(ProductGateway gateway) {
        return new UpdateProductUseCase(gateway);
    }

    @Bean
    public DeleteProductUseCase deleteProductUseCase(ProductGateway gateway) {
        return new DeleteProductUseCase(gateway);
    }

    @Bean
    public ManageStockUseCase manageStockUseCase(ProductGateway gateway) {
        return new ManageStockUseCase(gateway);
    }
}
