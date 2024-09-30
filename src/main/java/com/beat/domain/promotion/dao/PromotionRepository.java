package com.beat.domain.promotion.dao;

import com.beat.domain.promotion.domain.CarouselNumber;
import com.beat.domain.promotion.domain.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findAll();

    @Query("SELECT p.carouselNumber FROM Promotion p")
    List<CarouselNumber> findAllCarouselNumbers();

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM Promotion p WHERE p.carouselNumber IN :carouselNumbers")
    void deleteByCarouselNumbers(@Param("carouselNumbers") List<CarouselNumber> carouselNumbers);

    @Query("SELECT p FROM Promotion p WHERE p.carouselNumber = :carouselNumber")
    Optional<Promotion> findByCarouselNumber(@Param("carouselNumber") CarouselNumber carouselNumber);
}