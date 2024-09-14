package com.beat.domain.admin.port.in;

import com.beat.domain.promotion.domain.Promotion;

import java.util.List;

public interface AdminUseCase {
    List<Promotion> findAllPromotionsSortedByCarouselNumber();
}