package com.beat.apis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.beat.apis.booking.application.dto.BookingRefundRequest;
import com.beat.apis.booking.application.dto.BookingStatusType;
import com.beat.apis.booking.application.dto.GuestBookingRequest;
import com.beat.apis.common.application.ApiEnumMapper;
import com.beat.apis.home.application.dto.HomeFindAllResponse;
import com.beat.apis.home.application.dto.HomeFindRequest;
import com.beat.apis.home.application.dto.HomeGenreType;
import com.beat.apis.home.application.dto.HomePerformanceDetail;
import com.beat.apis.home.application.dto.HomePromotionDetail;
import com.beat.apis.member.application.dto.request.MemberLoginRequest;
import com.beat.apis.member.application.dto.request.SocialTypeRequest;
import com.beat.apis.performance.application.dto.BankNameType;
import com.beat.apis.performance.application.dto.GenreType;
import com.beat.apis.schedule.application.dto.ScheduleNumberType;
import com.beat.domain.booking.domain.BookingStatus;
import com.beat.domain.member.domain.SocialType;
import com.beat.domain.performance.domain.BankName;
import com.beat.domain.performance.domain.Genre;
import com.beat.domain.schedule.domain.ScheduleNumber;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ApisDtoJsonContractTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void apiEnumNamesStayCompatibleAcrossApiBoundaryMigration() {
		assertEquals(List.of("KAKAO"), enumNames(SocialTypeRequest.values()));

		assertEquals(List.of("BAND", "PLAY", "DANCE", "ETC"), enumNames(GenreType.values()));
		assertEquals(List.of("BAND", "PLAY", "DANCE", "ETC"), enumNames(HomeGenreType.values()));

		assertEquals(List.of(
			"FIRST", "SECOND", "THIRD", "FOURTH", "FIFTH",
			"SIXTH", "SEVENTH", "EIGHTH", "NINTH", "TENTH"), enumNames(ScheduleNumberType.values()));

		assertEquals(List.of(
			"NH_NONGHYUP", "KAKAOBANK", "KB_KOOKMIN", "TOSSBANK", "SHINHAN", "WOORI",
			"IBK_GIUP", "HANA", "SAEMAUL", "BUSAN", "IMBANK_DAEGU", "SINHYEOP",
			"WOOCHAEGUK", "SCJEIL", "SUHYEOP", "NONE"), enumNames(BankNameType.values()));

		assertEquals(List.of(
			"CHECKING_PAYMENT", "BOOKING_CONFIRMED", "BOOKING_CANCELLED",
			"REFUND_REQUESTED", "BOOKING_DELETED"), enumNames(BookingStatusType.values()));

		assertSameEnumNames(SocialTypeRequest.values(), SocialType.values());
		assertSameEnumNames(GenreType.values(), Genre.values());
		assertSameEnumNames(HomeGenreType.values(), Genre.values());
		assertSameEnumNames(ScheduleNumberType.values(), ScheduleNumber.values());
		assertSameEnumNames(BankNameType.values(), BankName.values());
		assertSameEnumNames(BookingStatusType.values(), BookingStatus.values());
	}

	@Test
	void apiEnumMapperConvertsByNameOnlyAtApplicationBoundary() {
		assertEquals(SocialType.KAKAO, ApiEnumMapper.toDomain(SocialTypeRequest.KAKAO, SocialType.class));
		assertEquals(Genre.BAND, ApiEnumMapper.toDomain(GenreType.BAND, Genre.class));
		assertEquals(Genre.BAND, ApiEnumMapper.toDomain(HomeGenreType.BAND, Genre.class));
		assertEquals(ScheduleNumber.FIRST, ApiEnumMapper.toDomain(ScheduleNumberType.FIRST, ScheduleNumber.class));
		assertEquals(BankName.KAKAOBANK, ApiEnumMapper.toDomain(BankNameType.KAKAOBANK, BankName.class));
		assertEquals(BookingStatus.CHECKING_PAYMENT,
			ApiEnumMapper.toDomain(BookingStatusType.CHECKING_PAYMENT, BookingStatus.class));

		assertEquals(SocialTypeRequest.KAKAO, ApiEnumMapper.fromDomain(SocialType.KAKAO, SocialTypeRequest.class));
		assertEquals(GenreType.BAND, ApiEnumMapper.fromDomain(Genre.BAND, GenreType.class));
		assertEquals(HomeGenreType.BAND, ApiEnumMapper.fromDomain(Genre.BAND, HomeGenreType.class));
		assertEquals(ScheduleNumberType.FIRST, ApiEnumMapper.fromDomain(ScheduleNumber.FIRST, ScheduleNumberType.class));
		assertEquals(BankNameType.KAKAOBANK, ApiEnumMapper.fromDomain(BankName.KAKAOBANK, BankNameType.class));
		assertEquals(BookingStatusType.CHECKING_PAYMENT,
			ApiEnumMapper.fromDomain(BookingStatus.CHECKING_PAYMENT, BookingStatusType.class));
	}

	@Test
	void apiEnumMapperFailureShouldExposeSourceAndTargetEnumTypes() {
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> ApiEnumMapper.toDomain(ApiOnlyStatus.API_ONLY, DomainOnlyStatus.class)
		);

		assertTrue(exception.getMessage().contains("API_ONLY"));
		assertTrue(exception.getMessage().contains(ApiOnlyStatus.class.getName()));
		assertTrue(exception.getMessage().contains(DomainOnlyStatus.class.getName()));
	}

	@Test
	void domainEnumRequestJsonStringValuesStayCompatibleAcrossApiBoundaryMigration() throws Exception {
		MemberLoginRequest memberLoginRequest = new MemberLoginRequest(SocialTypeRequest.KAKAO);
		HomeFindRequest homeFindRequest = new HomeFindRequest(HomeGenreType.BAND);
		BookingRefundRequest bookingRefundRequest = new BookingRefundRequest(1L, BankNameType.KAKAOBANK, "123", "holder");
		GuestBookingRequest guestBookingRequest = GuestBookingRequest.of(
			1L,
			2,
			ScheduleNumberType.FIRST,
			"booker",
			"010-0000-0000",
			"990101",
			"password",
			20000,
			BookingStatusType.CHECKING_PAYMENT
		);

		assertTextField(objectMapper.valueToTree(memberLoginRequest), "socialType", "KAKAO");
		assertTextField(objectMapper.valueToTree(homeFindRequest), "genre", "BAND");
		assertTextField(objectMapper.valueToTree(bookingRefundRequest), "bankName", "KAKAOBANK");
		JsonNode guestBookingJson = objectMapper.valueToTree(guestBookingRequest);
		assertTextField(guestBookingJson, "scheduleNumber", "FIRST");
		assertTextField(guestBookingJson, "bookingStatus", "CHECKING_PAYMENT");

		assertEquals(SocialTypeRequest.KAKAO,
			objectMapper.readValue("{\"socialType\":\"KAKAO\"}", MemberLoginRequest.class).socialType());
		assertEquals(HomeGenreType.BAND, objectMapper.readValue("{\"genre\":\"BAND\"}", HomeFindRequest.class).genre());
		assertEquals(BankNameType.KAKAOBANK, objectMapper.readValue(
			"{\"bookingId\":1,\"bankName\":\"KAKAOBANK\",\"accountNumber\":\"123\",\"accountHolder\":\"holder\"}",
			BookingRefundRequest.class
		).bankName());
		GuestBookingRequest parsedGuestBookingRequest = objectMapper.readValue(
			"{\"scheduleId\":1,\"purchaseTicketCount\":2,\"scheduleNumber\":\"FIRST\","
				+ "\"bookerName\":\"booker\",\"bookerPhoneNumber\":\"010-0000-0000\",\"birthDate\":\"990101\","
				+ "\"password\":\"password\",\"totalPaymentAmount\":20000,\"bookingStatus\":\"CHECKING_PAYMENT\"}",
			GuestBookingRequest.class
		);
		assertEquals(ScheduleNumberType.FIRST, parsedGuestBookingRequest.scheduleNumber());
		assertEquals(BookingStatusType.CHECKING_PAYMENT, parsedGuestBookingRequest.bookingStatus());
	}

	@Test
	void homeResponseJsonFieldNamesAndEnumValuesStayCompatible() {
		HomeFindAllResponse response = HomeFindAllResponse.of(
			List.of(HomePromotionDetail.of(1L, "promotion.png", 11L, "redirect", true, "ONE")),
			List.of(HomePerformanceDetail.of(11L, "title", "period", 30000, 3, "BAND", "poster.png", "venue"))
		);

		JsonNode json = objectMapper.valueToTree(response);
		JsonNode promotion = json.get("promotionList").get(0);
		JsonNode performance = json.get("performanceList").get(0);

		assertTrue(json.has("promotionList"));
		assertTrue(json.has("performanceList"));
		assertTrue(promotion.has("carouselNumber"));
		assertTrue(performance.has("genre"));
		assertFalse(promotion.get("carouselNumber").isObject());
		assertFalse(performance.get("genre").isObject());
		assertEquals("ONE", promotion.get("carouselNumber").asText());
		assertEquals("BAND", performance.get("genre").asText());
	}

	private enum ApiOnlyStatus {
		API_ONLY
	}

	private enum DomainOnlyStatus {
		DOMAIN_ONLY
	}

	private List<String> enumNames(Enum<?>[] values) {
		return Arrays.stream(values).map(Enum::name).toList();
	}

	private void assertSameEnumNames(Enum<?>[] apiValues, Enum<?>[] domainValues) {
		assertEquals(enumNames(domainValues), enumNames(apiValues));
	}

	private void assertTextField(JsonNode json, String fieldName, String expectedValue) {
		assertTrue(json.has(fieldName), () -> "Missing JSON field: " + fieldName);
		assertFalse(json.get(fieldName).isObject(), () -> fieldName + " must stay a JSON string");
		assertEquals(expectedValue, json.get(fieldName).asText());
	}
}
