package com.beat.domain.admin.application;

import com.beat.domain.admin.port.in.AdminUseCase;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.port.in.PromotionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService implements AdminUseCase {

    private final PromotionUseCase promotionUseCase;

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> findAllPromotionsSortedByCarouselNumber() {
        List<Promotion> promotions = promotionUseCase.findAllPromotions();
        promotions.sort(Comparator.comparing(p -> p.getCarouselNumber().ordinal()));
        return promotions;
    }
}