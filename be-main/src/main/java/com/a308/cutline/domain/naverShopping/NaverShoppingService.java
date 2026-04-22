package com.a308.cutline.domain.naverShopping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NaverShoppingService {

    @Qualifier("naverShoppingWebClient")
    private final WebClient webClient;

    public List<NaverShoppingItem> searchProducts(String query, Integer maxPrice,int count) {
        try {
//            log.info("네이버 쇼핑 API 호출 시작 - 검색어: {}, 최대가격: {}", query, maxPrice);
//            log.info("검색 쿼리: {}", query);
//            log.info("최대 가격: {}", maxPrice);

            // API 호출 시 display는 요청 개수(count)보다 크게 설정 (네이버 최대: 100)
//            int apiDisplay = Math.min(100, count * 5); // 5개 요청 시 25개를 요청해서 필터링에 대비

            int apiDisplay = 10;
            // 네이버 쇼핑 API 호출
            NaverShoppingResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("")  // baseUrl에 이미 경로가 포함되어 있으므로 빈 경로
                            .queryParam("query", query)
                            .queryParam("display", apiDisplay)  // 최대 20개 상품 조회
                            .queryParam("sort", "sim")  // 정확도순 정렬
                            .build())
                    .retrieve()
                    .bodyToMono(NaverShoppingResponse.class)
                    .block(); // 동기 호출

            if (response == null || response.getItems() == null) {
                log.warn("네이버 쇼핑 API 응답이 비어있습니다.");
                throw new RuntimeException("네이버 쇼핑 API 응답이 비어있습니다."); // 빈 리스트 대신 예외 발생
            }

            log.info("네이버 쇼핑 API 응답 받음 - 총 {}개 상품", response.getItems().size());

            // 가격 필터링 및 상위 5개 선택
            List<NaverShoppingItem> filteredItems = response.getItems().stream()
                    .filter(item -> {
                        try {
                            int price = Integer.parseInt(item.getLprice());
                            boolean inRange = price <= maxPrice && price >= maxPrice * 0.6; // 30%~100% 범위

                            return inRange;
                        } catch (NumberFormatException e) {
                            log.warn("가격 파싱 실패: {}", item.getLprice());
                            return false;
                        }
                    })
                    .limit(count) // 상위 5개만 선택
                    .collect(Collectors.toList());

            log.info("필터링 완료 - 최종 {}개 상품 반환", filteredItems.size());
            return filteredItems;

        } catch (WebClientResponseException e) {
            log.error("네이버 쇼핑 API 호출 실패: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("네이버 쇼핑 API 호출 실패: " + e.getMessage(), e); // 빈 리스트 대신 예외 발생
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("예상치 못한 오류 발생: " + e.getMessage(), e); // 빈 리스트 대신 예외 발생
        }

    }
    // 💡 기존의 인자 2개짜리 메서드 호출에 대응하기 위해 오버로딩 (선택 사항)
    public List<NaverShoppingItem> searchProducts(String query, Integer maxPrice) {
        // 기존 메서드에서 5개로 고정하고 싶다면
        return searchProducts(query, maxPrice, 5);
    }
}