package com.beat.apis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.beat.apis.booking.application.dto.BookingRefundRequest;
import com.beat.apis.booking.application.dto.BookingStatusType;
import com.beat.apis.booking.application.dto.GuestBookingRequest;
import com.beat.apis.booking.application.dto.GuestBookingResponse;
import com.beat.apis.booking.application.dto.MemberBookingRequest;
import com.beat.apis.booking.application.dto.MemberBookingResponse;
import com.beat.apis.common.application.converter.BankNameEnumConverter;
import com.beat.apis.common.application.converter.BookingStatusEnumConverter;
import com.beat.apis.common.application.converter.GenreEnumConverter;
import com.beat.apis.common.application.converter.ScheduleNumberEnumConverter;
import com.beat.apis.common.application.converter.SocialTypeEnumConverter;
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
import com.beat.contracts.booking.readmodel.MakerTicketBookingStatus;
import com.beat.contracts.booking.readmodel.MakerTicketScheduleNumber;
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
	void explicitEnumConvertersPreserveApplicationBoundaryMappings() {
		assertEquals(SocialType.KAKAO, SocialTypeEnumConverter.toDomain(SocialTypeRequest.KAKAO));
		assertEquals(Genre.BAND, GenreEnumConverter.toDomain(GenreType.BAND));
		assertEquals(Genre.BAND, GenreEnumConverter.toDomain(HomeGenreType.BAND));
		assertEquals(ScheduleNumber.FIRST, ScheduleNumberEnumConverter.toDomain(ScheduleNumberType.FIRST));
		assertEquals(BankName.KAKAOBANK, BankNameEnumConverter.toDomain(BankNameType.KAKAOBANK));
		assertEquals(BookingStatus.CHECKING_PAYMENT,
			BookingStatusEnumConverter.toDomainForTicketUpdate(BookingStatusType.CHECKING_PAYMENT));
		assertEquals(MakerTicketBookingStatus.CHECKING_PAYMENT,
			BookingStatusEnumConverter.toMakerTicketStatus(BookingStatusType.CHECKING_PAYMENT));
		assertEquals(MakerTicketScheduleNumber.FIRST,
			ScheduleNumberEnumConverter.toMakerTicketScheduleNumber(ScheduleNumberType.FIRST));
		assertEquals(MakerTicketScheduleNumber.FIRST,
			ScheduleNumberEnumConverter.toMakerTicketScheduleNumber(ScheduleNumber.FIRST));

		assertEquals(GenreType.BAND, GenreEnumConverter.toPerformanceApi(Genre.BAND));
		assertEquals(HomeGenreType.BAND, GenreEnumConverter.toHomeApi(Genre.BAND));
		assertEquals(ScheduleNumberType.FIRST, ScheduleNumberEnumConverter.toApi(ScheduleNumber.FIRST));
		assertEquals(BankNameType.KAKAOBANK, BankNameEnumConverter.toApi(BankName.KAKAOBANK));
		assertEquals(BookingStatusType.CHECKING_PAYMENT,
			BookingStatusEnumConverter.toApi(BookingStatus.CHECKING_PAYMENT));
		assertEquals(BookingStatusType.CHECKING_PAYMENT,
			BookingStatusEnumConverter.toApi(MakerTicketBookingStatus.CHECKING_PAYMENT));
		assertEquals("FIRST", ScheduleNumberEnumConverter.toApiName(ScheduleNumber.FIRST));
	}

	@Test
	void explicitEnumConvertersPreserveNullPassthrough() {
		assertEquals(null, SocialTypeEnumConverter.toDomain(null));
		assertEquals(null, GenreEnumConverter.toDomain((GenreType)null));
		assertEquals(null, GenreEnumConverter.toDomain((HomeGenreType)null));
		assertEquals(null, ScheduleNumberEnumConverter.toDomain(null));
		assertEquals(null, BankNameEnumConverter.toDomain(null));
		assertEquals(null, BookingStatusEnumConverter.toDomainForTicketUpdate(null));
		assertEquals(null, BookingStatusEnumConverter.toMakerTicketStatus(null));
		assertEquals(null, ScheduleNumberEnumConverter.toMakerTicketScheduleNumber((ScheduleNumberType)null));
		assertEquals(null, ScheduleNumberEnumConverter.toMakerTicketScheduleNumber((ScheduleNumber)null));

		assertEquals(null, GenreEnumConverter.toPerformanceApi(null));
		assertEquals(null, GenreEnumConverter.toHomeApi(null));
		assertEquals(null, ScheduleNumberEnumConverter.toApi(null));
		assertEquals(null, BankNameEnumConverter.toApi(null));
		assertEquals(null, BookingStatusEnumConverter.toApi((BookingStatus)null));
		assertEquals(null, BookingStatusEnumConverter.toApi((MakerTicketBookingStatus)null));
		assertEquals(null, ScheduleNumberEnumConverter.toApiName(null));
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
		MemberBookingRequest memberBookingRequest = new MemberBookingRequest(
			1L,
			ScheduleNumberType.FIRST,
			2,
			"booker",
			"010-0000-0000",
			BookingStatusType.CHECKING_PAYMENT,
			20000
		);

		assertTextField(objectMapper.valueToTree(memberLoginRequest), "socialType", "KAKAO");
		assertTextField(objectMapper.valueToTree(homeFindRequest), "genre", "BAND");
		assertTextField(objectMapper.valueToTree(bookingRefundRequest), "bankName", "KAKAOBANK");
		JsonNode guestBookingJson = objectMapper.valueToTree(guestBookingRequest);
		assertTextField(guestBookingJson, "scheduleNumber", "FIRST");
		assertTextField(guestBookingJson, "bookingStatus", "CHECKING_PAYMENT");
		JsonNode memberBookingJson = objectMapper.valueToTree(memberBookingRequest);
		assertTextField(memberBookingJson, "scheduleNumber", "FIRST");
		assertTextField(memberBookingJson, "bookingStatus", "CHECKING_PAYMENT");

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
		MemberBookingRequest parsedMemberBookingRequest = objectMapper.readValue(
			"{\"scheduleId\":1,\"scheduleNumber\":\"FIRST\",\"purchaseTicketCount\":2,"
				+ "\"bookerName\":\"booker\",\"bookerPhoneNumber\":\"010-0000-0000\","
				+ "\"bookingStatus\":\"CHECKING_PAYMENT\",\"totalPaymentAmount\":20000}",
			MemberBookingRequest.class
		);
		assertEquals(ScheduleNumberType.FIRST, parsedMemberBookingRequest.scheduleNumber());
		assertEquals(BookingStatusType.CHECKING_PAYMENT, parsedMemberBookingRequest.bookingStatus());
	}

	@Test
	void bookingResponseJsonStringValuesStayCompatibleAcrossApiBoundaryMigration() {
		GuestBookingResponse guestBookingResponse = GuestBookingResponse.of(
			10L,
			1L,
			30L,
			2,
			ScheduleNumberType.FIRST,
			"booker",
			"010-0000-0000",
			BookingStatusType.CHECKING_PAYMENT,
			BankNameType.KAKAOBANK,
			"123",
			20000,
			null
		);
		MemberBookingResponse memberBookingResponse = MemberBookingResponse.of(
			10L,
			1L,
			30L,
			2,
			ScheduleNumberType.FIRST,
			"booker",
			"010-0000-0000",
			BookingStatusType.CHECKING_PAYMENT,
			BankNameType.KAKAOBANK,
			"123",
			20000,
			null
		);

		JsonNode guestJson = objectMapper.valueToTree(guestBookingResponse);
		assertTextField(guestJson, "scheduleNumber", "FIRST");
		assertTextField(guestJson, "bookingStatus", "CHECKING_PAYMENT");
		assertTextField(guestJson, "bankName", "KAKAOBANK");

		JsonNode memberJson = objectMapper.valueToTree(memberBookingResponse);
		assertTextField(memberJson, "scheduleNumber", "FIRST");
		assertTextField(memberJson, "bookingStatus", "CHECKING_PAYMENT");
		assertTextField(memberJson, "bankName", "KAKAOBANK");
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
