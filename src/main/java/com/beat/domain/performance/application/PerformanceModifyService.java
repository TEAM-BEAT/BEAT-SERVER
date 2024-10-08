package com.beat.domain.performance.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.domain.booking.dao.BookingRepository;
import com.beat.domain.cast.dao.CastRepository;
import com.beat.domain.cast.domain.Cast;
import com.beat.domain.cast.exception.CastErrorCode;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.performance.application.dto.modify.PerformanceModifyRequest;
import com.beat.domain.performance.application.dto.modify.PerformanceModifyResponse;
import com.beat.domain.performance.application.dto.modify.cast.CastModifyRequest;
import com.beat.domain.performance.application.dto.modify.cast.CastModifyResponse;
import com.beat.domain.performance.application.dto.modify.performanceImage.PerformanceImageModifyRequest;
import com.beat.domain.performance.application.dto.modify.performanceImage.PerformanceImageModifyResponse;
import com.beat.domain.performance.application.dto.modify.schedule.ScheduleModifyRequest;
import com.beat.domain.performance.application.dto.modify.schedule.ScheduleModifyResponse;
import com.beat.domain.performance.application.dto.modify.staff.StaffModifyRequest;
import com.beat.domain.performance.application.dto.modify.staff.StaffModifyResponse;
import com.beat.domain.performance.dao.PerformanceImageRepository;
import com.beat.domain.performance.dao.PerformanceRepository;
import com.beat.domain.performance.domain.Performance;
import com.beat.domain.performance.domain.PerformanceImage;
import com.beat.domain.performance.exception.PerformanceErrorCode;
import com.beat.domain.performance.exception.PerformanceImageErrorCode;
import com.beat.domain.schedule.dao.ScheduleRepository;
import com.beat.domain.schedule.domain.Schedule;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.beat.domain.schedule.exception.ScheduleErrorCode;
import com.beat.domain.staff.dao.StaffRepository;
import com.beat.domain.staff.domain.Staff;
import com.beat.domain.staff.exception.StaffErrorCode;
import com.beat.global.common.exception.BadRequestException;
import com.beat.global.common.exception.ForbiddenException;
import com.beat.global.common.exception.NotFoundException;
import com.beat.global.common.scheduler.application.JobSchedulerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceModifyService {

	private final PerformanceRepository performanceRepository;
	private final ScheduleRepository scheduleRepository;
	private final MemberRepository memberRepository;
	private final CastRepository castRepository;
	private final StaffRepository staffRepository;
	private final BookingRepository bookingRepository;
	private final PerformanceImageRepository performanceImageRepository;
	private final JobSchedulerService jobSchedulerService;

	@Transactional
	public PerformanceModifyResponse modifyPerformance(Long memberId, PerformanceModifyRequest request) {
		log.info("Starting updatePerformance for memberId: {}, performanceId: {}", memberId, request.performanceId());

		Member member = validateMember(memberId);
		Long userId = member.getUser().getId();

		Performance performance = findPerformance(request.performanceId());

		validateOwnership(userId, performance);

		List<Long> scheduleIds = scheduleRepository.findIdsByPerformanceId(request.performanceId());
		boolean isBookerExist = bookingRepository.existsByScheduleIdIn(scheduleIds);

		if (isBookerExist && request.ticketPrice() != performance.getTicketPrice()) {
			log.error("Ticket price update failed due to existing bookings for performanceId: {}", performance.getId());
			throw new BadRequestException(PerformanceErrorCode.PRICE_UPDATE_NOT_ALLOWED);
		}

		updatePerformanceDetails(performance, request, isBookerExist);

		List<ScheduleModifyResponse> modifiedSchedules = processSchedules(request.scheduleModifyRequests(),
			performance);
		List<CastModifyResponse> modifiedCasts = processCasts(request.castModifyRequests(), performance);
		List<StaffModifyResponse> modifiedStaffs = processStaffs(request.staffModifyRequests(), performance);
		List<PerformanceImageModifyResponse> modifiedPerformanceImages = processPerformanceImages(
			request.performanceImageModifyRequests(), performance);

		PerformanceModifyResponse response = completeModifyResponse(performance, modifiedSchedules, modifiedCasts,
			modifiedStaffs, modifiedPerformanceImages);

		log.info("Successfully completed updatePerformance for performanceId: {}", request.performanceId());
		return response;
	}

	private Member validateMember(Long memberId) {
		log.debug("Validating memberId: {}", memberId);
		return memberRepository.findById(memberId)
			.orElseThrow(() -> {
				log.error("Member not found: memberId: {}", memberId);
				return new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND);
			});
	}

	private Performance findPerformance(Long performanceId) {
		log.debug("Finding performance with performanceId: {}", performanceId);
		return performanceRepository.findById(performanceId)
			.orElseThrow(() -> {
				log.error("Performance not found: performanceId: {}", performanceId);
				return new NotFoundException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND);
			});
	}

	private void validateOwnership(Long userId, Performance performance) {
		if (!performance.getUsers().getId().equals(userId)) {
			log.error("User ID {} does not own performance ID {}", userId, performance.getId());
			throw new ForbiddenException(PerformanceErrorCode.NOT_PERFORMANCE_OWNER);
		}
	}

	private void updatePerformanceDetails(Performance performance, PerformanceModifyRequest request,
		boolean isBookerExist) {
		log.debug("Updating performance details for performanceId: {}", performance.getId());

		performance.update(
			request.performanceTitle(),
			request.genre(),
			request.runningTime(),
			request.performanceDescription(),
			request.performanceAttentionNote(),
			request.bankName(),
			request.accountNumber(),
			request.accountHolder(),
			request.posterImage(),
			request.performanceTeamName(),
			request.performanceVenue(),
			request.performanceContact(),
			request.performancePeriod(),
			request.totalScheduleCount()
		);

		if (!isBookerExist) {
			log.debug("Updating ticket price to {}", request.ticketPrice());
			performance.updateTicketPrice(request.ticketPrice());
		}

		performanceRepository.save(performance);
		log.debug("Performance details updated for performanceId: {}", performance.getId());
	}

	private List<ScheduleModifyResponse> processSchedules(List<ScheduleModifyRequest> scheduleRequests,
		Performance performance) {
		List<Long> existingScheduleIds = scheduleRepository.findIdsByPerformanceId(performance.getId());

		List<Long> requestScheduleIds = scheduleRequests.stream()
			.map(ScheduleModifyRequest::scheduleId)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());

		List<Long> schedulesToDelete = existingScheduleIds.stream()
			.filter(id -> !requestScheduleIds.contains(id))
			.collect(Collectors.toList());

		deleteRemainingSchedules(schedulesToDelete);

		List<Schedule> schedules = scheduleRequests.stream()
			.map(request -> {
				Schedule schedule;
				if (request.scheduleId() == null) {
					schedule = addSchedule(request, performance);
				} else {
					schedule = updateSchedule(request, performance);
				}

				// isBooking이 true인 경우만 스케줄러에 등록
				if (schedule.isBooking()) {
					jobSchedulerService.addScheduleIfNotExists(schedule);
				}

				return schedule;
			})
			.collect(Collectors.toList());

		assignScheduleNumbers(schedules);

		return schedules.stream()
			.map(schedule -> ScheduleModifyResponse.of(
				schedule.getId(),
				schedule.getPerformanceDate(),
				schedule.getTotalTicketCount(),
				calculateDueDate(schedule.getPerformanceDate()),
				schedule.getScheduleNumber()
			))
			.collect(Collectors.toList());
	}

	private void assignScheduleNumbers(List<Schedule> schedules) {
		schedules.sort(Comparator.comparing(Schedule::getPerformanceDate));

		for (int i = 0; i < schedules.size(); i++) {
			ScheduleNumber scheduleNumber = ScheduleNumber.values()[i];
			schedules.get(i).updateScheduleNumber(scheduleNumber);
		}
	}

	private Schedule addSchedule(ScheduleModifyRequest request, Performance performance) {
		log.debug("Adding schedules for performanceId: {}", performance.getId());

		if (request.performanceDate().isBefore(LocalDateTime.now())) {
			throw new BadRequestException(PerformanceErrorCode.PAST_SCHEDULE_NOT_ALLOWED);
		}

		long existingSchedulesCount = scheduleRepository.countByPerformanceId(performance.getId());

		if ((existingSchedulesCount + 1) > 10) {
			throw new BadRequestException(PerformanceErrorCode.MAX_SCHEDULE_LIMIT_EXCEEDED);
		}

		Schedule schedule = Schedule.create(
			request.performanceDate(),
			request.totalTicketCount(),
			0,
			true,
			ScheduleNumber.FIRST, // 임시로 1회차
			performance
		);

		Schedule savedSchedule = scheduleRepository.save(schedule);
		log.debug("Added schedule with scheduleId: {} for performanceId: {}", savedSchedule.getId(),
			performance.getId());
		return savedSchedule;
	}

	private Schedule updateSchedule(ScheduleModifyRequest request, Performance performance) {
		log.debug("Updating schedules for scheduleId: {}", request.scheduleId());

		Schedule schedule = scheduleRepository.findById(request.scheduleId())
			.orElseThrow(() -> {
				log.error("Schedule not found: scheduleId: {}", request.scheduleId());
				return new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND);
			});

		if (!schedule.getPerformance().equals(performance)) {
			throw new ForbiddenException(ScheduleErrorCode.SCHEDULE_NOT_BELONG_TO_PERFORMANCE);
		}

		// 종료된 스케줄(기존 스케쥴 날짜가 과거인 경우)은 날짜 변경 불가
		if (schedule.getPerformanceDate().isBefore(LocalDateTime.now())) {
			if (!schedule.getPerformanceDate().isEqual(request.performanceDate())) {
				throw new BadRequestException(
					PerformanceErrorCode.SCHEDULE_MODIFICATION_NOT_ALLOWED_FOR_ENDED_SCHEDULE);
			}
			// 날짜 변경이 없으면 그대로 반환
			return schedule;
		}

		// 종료되지 않은 스케줄을 과거 날짜로 수정 시 에러 발생
		if (request.performanceDate().isBefore(LocalDateTime.now())) {
			throw new BadRequestException(PerformanceErrorCode.PAST_SCHEDULE_NOT_ALLOWED);
		}

		// 티켓 수 관련 검증
		if (request.totalTicketCount() != schedule.getTotalTicketCount()) {
			// 판매된 티켓 수보다 적은 totalTicketCount로 변경하려는 경우 예외 처리
			if (request.totalTicketCount() < schedule.getSoldTicketCount()) {
				throw new BadRequestException(PerformanceErrorCode.INVALID_TICKET_COUNT);
			}

			// 매진 상태로 변경 (soldTicketCount와 totalTicketCount가 동일하고, 기존 isBooking이 true인 경우)
			if (request.totalTicketCount() == schedule.getSoldTicketCount() && schedule.isBooking()) {
				schedule.updateIsBooking(false);  // 매진 처리 (isBooking = false)
			}

			// 매진이 풀리는 경우 (totalTicketCount 증가, 기존 isBooking이 false인 경우)
			else if (request.totalTicketCount() > schedule.getTotalTicketCount() && !schedule.isBooking()) {
				schedule.updateIsBooking(true);  // 매진 풀림 처리 (isBooking = true)
			}
		}

		jobSchedulerService.cancelScheduledTaskForPerformance(schedule);

		schedule.update(
			request.performanceDate(),
			request.totalTicketCount(),
			schedule.getScheduleNumber()  // 기존 scheduleNumber 유지
		);
		return scheduleRepository.save(schedule);
	}

	private void deleteRemainingSchedules(List<Long> scheduleIds) {
		if (scheduleIds == null || scheduleIds.isEmpty()) {
			log.debug("No schedules to delete");
			return;
		}

		scheduleIds.forEach(scheduleId -> {
			Schedule schedule = scheduleRepository.findById(scheduleId)
				.orElseThrow(() -> {
					log.error("Schedule not found: scheduleId: {}", scheduleId);
					return new NotFoundException(ScheduleErrorCode.NO_SCHEDULE_FOUND);
				});

			jobSchedulerService.cancelScheduledTaskForPerformance(schedule);

			scheduleRepository.delete(schedule);
			log.debug("Deleted schedule with scheduleId: {}", scheduleId);
		});
	}

	private List<CastModifyResponse> processCasts(List<CastModifyRequest> castRequests, Performance performance) {
		log.debug("Processing casts for performanceId: {}", performance.getId());

		List<Long> existingCastIds = castRepository.findIdsByPerformanceId(performance.getId());

		List<CastModifyResponse> responses = castRequests.stream()
			.map(request -> {
				if (request.castId() == null) {
					return addCast(request, performance);
				} else {
					existingCastIds.remove(request.castId());
					return updateCast(request, performance);
				}
			})
			.toList();

		deleteRemainingCasts(existingCastIds);

		return responses;
	}

	private CastModifyResponse addCast(CastModifyRequest request, Performance performance) {
		log.debug("Adding casts for performanceId: {}", performance.getId());

		Cast cast = Cast.create(
			request.castName(),
			request.castRole(),
			request.castPhoto(),
			performance
		);
		Cast savedCast = castRepository.save(cast);
		log.debug("Added cast with castId: {} for performanceId: {}", savedCast.getId(), performance.getId());
		return CastModifyResponse.of(
			savedCast.getId(),
			savedCast.getCastName(),
			savedCast.getCastRole(),
			savedCast.getCastPhoto()
		);
	}

	private CastModifyResponse updateCast(CastModifyRequest request, Performance performance) {
		log.debug("Updating casts for castId: {}", request.castId());

		Cast cast = castRepository.findById(request.castId())
			.orElseThrow(() -> {
				log.error("Cast not found: castId: {}", request.castId());
				return new NotFoundException(CastErrorCode.CAST_NOT_FOUND);
			});

		if (!cast.getPerformance().equals(performance)) {
			throw new ForbiddenException(CastErrorCode.CAST_NOT_BELONG_TO_PERFORMANCE);
		}

		cast.update(
			request.castName(),
			request.castRole(),
			request.castPhoto()
		);
		castRepository.save(cast);
		log.debug("Updated cast with castId: {}", cast.getId());
		return CastModifyResponse.of(
			cast.getId(),
			cast.getCastName(),
			cast.getCastRole(),
			cast.getCastPhoto()
		);
	}

	private void deleteRemainingCasts(List<Long> castIds) {
		if (castIds == null || castIds.isEmpty()) {
			log.debug("No casts to delete");
			return;
		}

		castIds.forEach(castId -> {
			Cast cast = castRepository.findById(castId)
				.orElseThrow(() -> {
					log.error("Cast not found: castId: {}", castId);
					return new NotFoundException(CastErrorCode.CAST_NOT_FOUND);
				});
			castRepository.delete(cast);
			log.debug("Deleted cast with castId: {}", castId);
		});
	}

	private List<StaffModifyResponse> processStaffs(List<StaffModifyRequest> staffRequests, Performance performance) {
		log.debug("Processing staffs for performanceId: {}", performance.getId());

		List<Long> existingStaffIds = staffRepository.findIdsByPerformanceId(performance.getId());

		List<StaffModifyResponse> responses = staffRequests.stream()
			.map(request -> {
				if (request.staffId() == null) {
					return addStaff(request, performance);
				} else {
					existingStaffIds.remove(request.staffId()); // 요청에 포함된 ID는 삭제 후보에서 제거
					return updateStaff(request, performance);
				}
			})
			.collect(Collectors.toList());

		deleteRemainingStaffs(existingStaffIds);

		return responses;
	}

	private StaffModifyResponse addStaff(StaffModifyRequest request, Performance performance) {
		log.debug("Adding staffs for performanceId: {}", performance.getId());

		Staff staff = Staff.create(
			request.staffName(),
			request.staffRole(),
			request.staffPhoto(),
			performance
		);
		Staff savedStaff = staffRepository.save(staff);
		log.debug("Added staff with staffId: {} for performanceId: {}", savedStaff.getId(), performance.getId());
		return StaffModifyResponse.of(
			savedStaff.getId(),
			savedStaff.getStaffName(),
			savedStaff.getStaffRole(),
			savedStaff.getStaffPhoto()
		);
	}

	private StaffModifyResponse updateStaff(StaffModifyRequest request, Performance performance) {
		log.debug("Updating staffs for staffId: {}", request.staffId());

		Staff staff = staffRepository.findById(request.staffId())
			.orElseThrow(() -> {
				log.error("Staff not found: staffId: {}", request.staffId());
				return new NotFoundException(StaffErrorCode.STAFF_NOT_FOUND);
			});

		if (!staff.getPerformance().equals(performance)) {
			throw new ForbiddenException(StaffErrorCode.STAFF_NOT_BELONG_TO_PERFORMANCE);
		}

		staff.update(
			request.staffName(),
			request.staffRole(),
			request.staffPhoto()
		);
		staffRepository.save(staff);
		log.debug("Updated staff with staffId: {}", staff.getId());
		return StaffModifyResponse.of(
			staff.getId(),
			staff.getStaffName(),
			staff.getStaffRole(),
			staff.getStaffPhoto()
		);
	}

	private void deleteRemainingStaffs(List<Long> staffIds) {
		if (staffIds == null || staffIds.isEmpty()) {
			log.debug("No staffs to delete");
			return;
		}

		staffIds.forEach(staffId -> {
			Staff staff = staffRepository.findById(staffId)
				.orElseThrow(() -> {
					log.error("Staff not found: staffId: {}", staffId);
					return new NotFoundException(StaffErrorCode.STAFF_NOT_FOUND);
				});
			staffRepository.delete(staff);
			log.debug("Deleted staff with staffId: {}", staffId);
		});
	}

	private int calculateDueDate(LocalDateTime performanceDate) {
		return (int)ChronoUnit.DAYS.between(LocalDate.now(), performanceDate.toLocalDate());
	}

	private List<PerformanceImageModifyResponse> processPerformanceImages(
		List<PerformanceImageModifyRequest> performanceImageRequests, Performance performance) {
		log.debug("Processing performanceImages for performanceId: {}", performance.getId());

		List<Long> existingPerformanceImageIds = performanceImageRepository.findIdsByPerformanceId(performance.getId());

		List<PerformanceImageModifyResponse> responses = performanceImageRequests.stream()
			.map(request -> {
				if (request.performanceImageId() == null) {
					return addPerformanceImage(request, performance);
				} else {
					existingPerformanceImageIds.remove(request.performanceImageId());
					return updatePerformanceImage(request, performance);
				}
			})
			.toList();

		deleteRemainingPerformanceImages(existingPerformanceImageIds);

		return responses;
	}

	private PerformanceImageModifyResponse addPerformanceImage(PerformanceImageModifyRequest request,
		Performance performance) {
		log.debug("Adding performanceImages for performanceId: {}", performance.getId());

		PerformanceImage performanceImage = PerformanceImage.create(
			request.performanceImage(),
			performance
		);
		PerformanceImage savedPerformanceImage = performanceImageRepository.save(performanceImage);
		log.debug("Added performanceImage: {}", savedPerformanceImage.getId());
		return PerformanceImageModifyResponse.of(
			savedPerformanceImage.getId(),
			savedPerformanceImage.getPerformanceImage()
		);
	}

	private PerformanceImageModifyResponse updatePerformanceImage(PerformanceImageModifyRequest request,
		Performance performance) {
		log.debug("Updating performanceImages for performanceId: {}", performance.getId());

		PerformanceImage performanceImage = performanceImageRepository.findById(request.performanceImageId())
			.orElseThrow(() -> {
				log.error("PerformanceImage not found: performanceId: {}", request.performanceImageId());
				return new NotFoundException(PerformanceImageErrorCode.PERFORMANCE_IMAGE_NOT_FOUND);
			});

		if (!performanceImage.getPerformance().equals(performance)) {
			throw new ForbiddenException(PerformanceImageErrorCode.PERFORMANCE_IMAGE_NOT_BELONG_TO_PERFORMANCE);
		}

		performanceImage.update(
			request.performanceImage()
		);
		performanceImageRepository.save(performanceImage);
		log.debug("Updated performanceImage: {}", performanceImage.getId());
		return PerformanceImageModifyResponse.of(
			performanceImage.getId(),
			performanceImage.getPerformanceImage()
		);
	}

	private void deleteRemainingPerformanceImages(List<Long> performanceImageIds) {
		if (performanceImageIds == null || performanceImageIds.isEmpty()) {
			log.debug("No performanceImages to delete");
			return;
		}

		performanceImageIds.forEach(performanceImageId -> {
			PerformanceImage performanceImage = performanceImageRepository.findById(performanceImageId)
				.orElseThrow(() -> {
					log.error("PerformanceImage not found: performanceImageId: {}", performanceImageId);
					return new NotFoundException(PerformanceImageErrorCode.PERFORMANCE_IMAGE_NOT_FOUND);
				});
			performanceImageRepository.delete(performanceImage);
			log.debug("Deleted performanceImage: {}", performanceImageId);
		});
	}

	private PerformanceModifyResponse completeModifyResponse(
		Performance performance,
		List<ScheduleModifyResponse> scheduleModifyResponses,
		List<CastModifyResponse> castModifyResponses,
		List<StaffModifyResponse> staffModifyResponses,
		List<PerformanceImageModifyResponse> performanceImageModifyResponses
	) {
		log.debug("Creating PerformanceModifyResponse for performanceId: {}", performance.getId());
		PerformanceModifyResponse response = PerformanceModifyResponse.of(
			performance.getUsers().getId(),
			performance.getId(),
			performance.getPerformanceTitle(),
			performance.getGenre(),
			performance.getRunningTime(),
			performance.getPerformanceDescription(),
			performance.getPerformanceAttentionNote(),
			performance.getBankName(),
			performance.getAccountNumber(),
			performance.getAccountHolder(),
			performance.getPosterImage(),
			performance.getPerformanceTeamName(),
			performance.getPerformanceVenue(),
			performance.getPerformanceContact(),
			performance.getPerformancePeriod(),
			performance.getTicketPrice(),
			performance.getTotalScheduleCount(),
			scheduleModifyResponses,
			castModifyResponses,
			staffModifyResponses,
			performanceImageModifyResponses
		);
		log.info("PerformanceModifyResponse created successfully for performanceId: {}", performance.getId());
		return response;
	}
}