package com.example.productservice.service;

import com.example.productservice.client.MemberClient;
import com.example.productservice.domain.Product;
import com.example.productservice.domain.ProductRepository;
import com.example.productservice.domain.ProductStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MemberClient memberClient;

    @InjectMocks
    private ProductService productService;

    @Test
    void getProductsRequestsMembersBySellerIds() {
        Product firstProduct = product("상품 A", 10L);
        Product secondProduct = product("상품 B", 20L);
        Product anotherProductFromSameSeller = product("상품 C", 10L);

        when(productRepository.findAll()).thenReturn(
                List.of(firstProduct, secondProduct, anotherProductFromSameSeller)
        );
        when(memberClient.fetchMembersByIds(List.of(10L, 20L)))
                .thenReturn(List.of());

        productService.getProducts();

        verify(memberClient)
                .fetchMembersByIds(List.of(10L, 20L));
    }

    private Product product(String name, Long memberId) {
        return new Product(
                name,
                10_000L,
                100,
                ProductStatus.READY,
                LocalDateTime.now().plusDays(1),
                memberId
        );
    }
}