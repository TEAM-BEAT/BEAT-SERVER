package com.beat.domain.booking.dao;

import com.beat.domain.booking.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<List<Booking>> findByBookerNameAndBookerPhoneNumberAndPasswordAndBirthDate(
            String bookerName,
            String bookerPhoneNumber,
            String password,
            String birthDate
    );

    Optional<Booking> findFirstByBookerNameAndBookerPhoneNumberAndBirthDateAndPassword(
            String bookerName,
            String bookerPhoneNumber,
            String birthDate,
            String password
    );

    List<Booking> findByUsersId(Long userId);
}