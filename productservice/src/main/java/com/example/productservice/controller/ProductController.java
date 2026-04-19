package com.example.productservice.controller;

import com.example.productservice.dto.CreateProductRequestDto;
import com.example.productservice.dto.ProductResponseDto;
import com.example.productservice.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService){
        this.productService = productService;
    }

    //상품생성
    @PostMapping("create")
    public ResponseEntity<Void> create(@RequestBody CreateProductRequestDto createBoardRequestDto){
      productService.create(createBoardRequestDto);
      return ResponseEntity.noContent().build();
    }

    //상품조회(담당자 정보 포함)
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable Long productId){
        ProductResponseDto productResponseDto = productService.getProduct(productId);
        return ResponseEntity.ok(productResponseDto);
    }

    //여러상품조회(담당자 정보 포함)
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getProducts(){
        List<ProductResponseDto> productResponseDtos = productService.getProducts();
        return ResponseEntity.ok(productResponseDtos);
    }
}
