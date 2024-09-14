package com.beat.domain.promotion.application;

import com.beat.domain.promotion.dao.PromotionRepository;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.promotion.port.in.PromotionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService implements PromotionUseCase {

    private final PromotionRepository promotionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> findAllPromotions() {
        return promotionRepository.findAll();
    }
}
