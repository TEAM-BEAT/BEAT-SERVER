package com.beat.domain.promotion.port.in;

import com.beat.domain.promotion.domain.Promotion;

import java.util.List;

public interface PromotionUseCase {
    List<Promotion> findAllPromotions();
}