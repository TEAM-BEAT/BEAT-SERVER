package com.beat.infra.persistence.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.beat.domain.booking.dao.BookingRepository;
import com.beat.domain.booking.domain.Booking;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.infra.persistence.booking.entity.BookingJpaEntity;
import com.beat.infra.persistence.booking.mapper.BookingPersistenceMapper;

@Repository
public class BookingRepositoryImpl implements BookingRepository {

	private final BookingJpaRepository bookingJpaRepository;
	private final BookingPersistenceMapper bookingPersistenceMapper;

	public BookingRepositoryImpl(BookingJpaRepository bookingJpaRepository,
		BookingPersistenceMapper bookingPersistenceMapper) {
		this.bookingJpaRepository = bookingJpaRepository;
		this.bookingPersistenceMapper = bookingPersistenceMapper;
	}

	@Override
	public Booking save(Booking booking) {
		BookingJpaEntity savedEntity = bookingJpaRepository.save(bookingPersistenceMapper.toEntity(booking));
		return bookingPersistenceMapper.toDomain(savedEntity);
	}

	@Override
	public Optional<Booking> findById(Long id) {
		return bookingJpaRepository.findById(id).map(bookingPersistenceMapper::toDomain);
	}

	@Override
	public List<Booking> findAll() {
		return bookingJpaRepository.findAll().stream()
			.map(bookingPersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public Optional<List<Booking>> findByBookerNameAndBookerPhoneNumberAndPasswordAndBirthDate(
		String bookerName,
		String bookerPhoneNumber,
		String password,
		String birthDate
	) {
		return bookingJpaRepository.findByBookerNameAndBookerPhoneNumberAndPasswordAndBirthDate(
			bookerName,
			bookerPhoneNumber,
			password,
			birthDate
		).map(entities -> entities.stream()
			.map(bookingPersistenceMapper::toDomain)
			.toList());
	}

	@Override
	public Optional<Booking> findFirstByBookerNameAndBookerPhoneNumberAndBirthDateAndPassword(
		String bookerName,
		String bookerPhoneNumber,
		String birthDate,
		String password
	) {
		return bookingJpaRepository.findFirstByBookerNameAndBookerPhoneNumberAndBirthDateAndPassword(
			bookerName,
			bookerPhoneNumber,
			birthDate,
			password
		).map(bookingPersistenceMapper::toDomain);
	}

	@Override
	public List<Booking> findByUserId(Long userId) {
		return bookingJpaRepository.findByUserId(userId).stream()
			.map(bookingPersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public boolean existsActiveBookingByScheduleIds(List<Long> scheduleIds, List<BookingStatus> excludedStatuses) {
		if (scheduleIds == null || scheduleIds.isEmpty()) {
			return false;
		}
		return bookingJpaRepository.existsActiveBookingByScheduleIds(scheduleIds, excludedStatuses);
	}

	@Override
	public int deleteInactiveBookingsByScheduleIds(List<Long> scheduleIds, List<BookingStatus> inactiveStatuses) {
		if (scheduleIds == null || scheduleIds.isEmpty()) {
			return 0;
		}
		return bookingJpaRepository.deleteInactiveBookingsByScheduleIds(scheduleIds, inactiveStatuses);
	}
}
