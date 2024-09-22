package com.beat.domain.promotion.dao;

import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findAll();

    List<CarouselNumber> findAllCarouselNumbers();

    void deleteByCarouselNumber(CarouselNumber carouselNumber);
}