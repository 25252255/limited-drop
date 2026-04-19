package com.example.productservice.service;

import com.example.productservice.client.MemberClient;
import com.example.productservice.domain.Product;
import com.example.productservice.domain.ProductRepository;
import com.example.productservice.dto.CreateProductRequestDto;
import com.example.productservice.dto.SellerDto;
import com.example.productservice.dto.MemberResponseDto;
import com.example.productservice.dto.ProductResponseDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final MemberClient memberClient;

    public ProductService(ProductRepository productRepository, MemberClient memberClient) {
        this.productRepository = productRepository;
        this.memberClient = memberClient;
    }

    @Transactional
    public void create(CreateProductRequestDto createProductRequestDto){
        Product product = new Product(
                createProductRequestDto.getName(),
                createProductRequestDto.getPrice(),
                createProductRequestDto.getInitialStock(),
                createProductRequestDto.getProductStatus(),
                createProductRequestDto.getSaleStartAt(),
                createProductRequestDto.getMemberId()
        );

        this.productRepository.save(product);
    }

    public ProductResponseDto getProduct(Long productId){
        //상품아이디로 상품 불러오기
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        //사용자 정보 불러오기
        Optional<MemberResponseDto> optionalMemberResponseDto = memberClient.fetchMember(product.getMemberId());

        SellerDto sellerDto = null;
        if (optionalMemberResponseDto.isPresent()){
            MemberResponseDto memberResponseDto = optionalMemberResponseDto.get();
            sellerDto = new SellerDto(
                    memberResponseDto.getMemberId(),
                    memberResponseDto.getName()
            );
        }
        /*
        //장애 전파에 대비 x
        MemberResponseDto memberResponseDto = memberClient.fetchMember(product.getMemberId());

        SellerDto sellerDto = new SellerDto(
                memberResponseDto.getMemberId(),
                memberResponseDto.getName()
        );*/

        //응답값 조합
        ProductResponseDto productResponseDto = new ProductResponseDto(
                product.getName(),
                product.getPrice(),
                product.getSaleStartAt(),
                sellerDto
        );

        return productResponseDto;
    }

    public List<ProductResponseDto> getProducts(){
        List<Product> products = productRepository.findAll();

        //필요한 사용자 정보(memberIds) 목록 추출
        List<Long> memberIds = products.stream()
                .map(Product::getProductId)
                .distinct()
                .toList();

        List<MemberResponseDto> memberResponseDtos = memberClient.fetchMembersByIds(memberIds);

        //추출된 memberId를 Key로 하는 Map을 생성
        Map<Long, SellerDto> sellerMap = new HashMap<>();
        for (MemberResponseDto memberResponseDto : memberResponseDtos){
            Long memberId = memberResponseDto.getMemberId();
            String name = memberResponseDto.getName();
            sellerMap.put(memberId, new SellerDto(memberId, name));
        }

        return products.stream()
                .map(product -> new ProductResponseDto(
                        product.getName(),
                        product.getPrice(),
                        product.getSaleStartAt(),
                        sellerMap.get(product.getMemberId())
                ))
                .toList();
    }
}
