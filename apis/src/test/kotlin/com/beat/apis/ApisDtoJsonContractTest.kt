package com.beat.apis

import com.beat.apis.booking.api.request.BookingRefundRequest
import com.beat.apis.booking.api.request.GuestBookingRequest
import com.beat.apis.booking.api.request.MemberBookingRequest
import com.beat.apis.booking.api.response.GuestBookingResponse
import com.beat.apis.booking.api.response.MemberBookingResponse
import com.beat.apis.booking.api.type.BookingStatusType
import com.beat.application.frontoffice.booking.booker.result.BookingCreationResult
import com.beat.apis.home.api.response.HomeFindAllResponse
import com.beat.apis.home.api.type.HomeGenreType
import com.beat.apis.home.application.result.HomeFindAllResult
import com.beat.apis.home.application.result.HomePerformanceResult
import com.beat.apis.home.application.result.HomePromotionResult
import com.beat.apis.member.api.request.MemberLoginRequest
import com.beat.apis.member.api.type.SocialTypeRequest
import com.beat.application.frontoffice.member.command.SocialLoginType
import com.beat.apis.performance.api.type.BankNameType
import com.beat.apis.performance.api.type.GenreType
import com.beat.application.frontoffice.performance.maker.command.PerformanceBankName
import com.beat.application.frontoffice.performance.maker.command.PerformanceGenre
import com.beat.application.frontoffice.performance.maker.command.PerformanceScheduleNumber
import com.beat.apis.performance.api.response.BookingPerformanceDetailScheduleResponse
import com.beat.apis.performance.api.response.PerformanceDetailScheduleResponse
import com.beat.apis.performance.api.response.PerformanceModifyDetailResponse
import com.beat.application.frontoffice.performance.booker.query.BookingPerformanceScheduleResult
import com.beat.application.frontoffice.performance.booker.query.PerformanceDetailScheduleResult
import com.beat.application.frontoffice.performance.maker.query.PerformanceEditResult
import com.beat.application.frontoffice.performance.maker.PerformanceMutationResult
import com.beat.apis.schedule.api.response.TicketAvailabilityResponse
import com.beat.application.frontoffice.schedule.booker.query.TicketAvailabilityResult
import com.beat.apis.schedule.api.type.ScheduleNumberType
import com.beat.application.frontoffice.ticket.maker.command.TicketBookingStatus
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.member.model.SocialType
import com.beat.domain.performance.model.Genre
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.sharedkernel.vo.BankName
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApisDtoJsonContractTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `required request collections reject null`() {
        val validator = Validation.buildDefaultValidatorFactory().validator
        val request = com.beat.apis.ticket.api.request.TicketRefundRequest(1L, null)

        assertEquals(1, validator.validate(request).size)
    }

    @Test
    fun `api enum names stay compatible across api boundary migration`() {
        assertEquals(listOf("KAKAO"), enumNames(SocialTypeRequest.entries.toTypedArray()))
        assertEquals(listOf("BAND", "PLAY", "DANCE", "ETC"), enumNames(GenreType.entries.toTypedArray()))
        assertEquals(listOf("BAND", "PLAY", "DANCE", "ETC"), enumNames(HomeGenreType.entries.toTypedArray()))
        assertEquals(
            listOf("FIRST", "SECOND", "THIRD", "FOURTH", "FIFTH", "SIXTH", "SEVENTH", "EIGHTH", "NINTH", "TENTH"),
            enumNames(ScheduleNumberType.entries.toTypedArray()),
        )
        assertEquals(
            listOf(
                "NH_NONGHYUP", "KAKAOBANK", "KB_KOOKMIN", "TOSSBANK", "SHINHAN", "WOORI",
                "IBK_GIUP", "HANA", "SAEMAUL", "BUSAN", "IMBANK_DAEGU", "SINHYEOP",
                "WOOCHAEGUK", "SCJEIL", "SUHYEOP", "NONE",
            ),
            enumNames(BankNameType.entries.toTypedArray()),
        )
        assertEquals(
            listOf("CHECKING_PAYMENT", "BOOKING_CONFIRMED", "BOOKING_CANCELLED", "REFUND_REQUESTED", "BOOKING_DELETED"),
            enumNames(BookingStatusType.entries.toTypedArray()),
        )

        assertSameEnumNames(SocialTypeRequest.entries.toTypedArray(), SocialType.entries.toTypedArray())
        assertSameEnumNames(GenreType.entries.toTypedArray(), Genre.entries.toTypedArray())
        assertSameEnumNames(HomeGenreType.entries.toTypedArray(), Genre.entries.toTypedArray())
        assertSameEnumNames(ScheduleNumberType.entries.toTypedArray(), ScheduleNumber.entries.toTypedArray())
        assertSameEnumNames(BankNameType.entries.toTypedArray(), BankName.entries.toTypedArray())
        assertSameEnumNames(BookingStatusType.entries.toTypedArray(), BookingStatus.entries.toTypedArray())
        assertSameEnumNames(SocialTypeRequest.entries.toTypedArray(), SocialLoginType.entries.toTypedArray())
        assertSameEnumNames(GenreType.entries.toTypedArray(), PerformanceGenre.entries.toTypedArray())
        assertSameEnumNames(BankNameType.entries.toTypedArray(), PerformanceBankName.entries.toTypedArray())
        assertSameEnumNames(ScheduleNumberType.entries.toTypedArray(), PerformanceScheduleNumber.entries.toTypedArray())
        assertSameEnumNames(BookingStatusType.entries.toTypedArray(), TicketBookingStatus.entries.toTypedArray())
    }

    @Test
    fun `domain enum request json string values stay compatible across api boundary migration`() {
        val memberLoginRequest = MemberLoginRequest(SocialTypeRequest.KAKAO)
        val bookingRefundRequest = BookingRefundRequest(1L, BankNameType.KAKAOBANK, "123", "holder")
        val guestBookingRequest = GuestBookingRequest(
            1L, 2, ScheduleNumberType.FIRST, "booker", "010-0000-0000", "990101", "password", 20000,
            BookingStatusType.CHECKING_PAYMENT,
        )
        val memberBookingRequest = MemberBookingRequest(
            1L, ScheduleNumberType.FIRST, 2, "booker", "010-0000-0000", BookingStatusType.CHECKING_PAYMENT, 20000,
        )

        assertTextField(objectMapper.valueToTree(memberLoginRequest), "socialType", "KAKAO")
        assertTextField(objectMapper.valueToTree(bookingRefundRequest), "bankName", "KAKAOBANK")
        val guestBookingJson = objectMapper.valueToTree<JsonNode>(guestBookingRequest)
        assertTextField(guestBookingJson, "scheduleNumber", "FIRST")
        assertTextField(guestBookingJson, "bookingStatus", "CHECKING_PAYMENT")
        val memberBookingJson = objectMapper.valueToTree<JsonNode>(memberBookingRequest)
        assertTextField(memberBookingJson, "scheduleNumber", "FIRST")
        assertTextField(memberBookingJson, "bookingStatus", "CHECKING_PAYMENT")

        assertEquals(
            SocialTypeRequest.KAKAO,
            objectMapper.readValue("""{"socialType":"KAKAO"}""", MemberLoginRequest::class.java).socialType,
        )
        assertEquals(
            BankNameType.KAKAOBANK,
            objectMapper.readValue(
                """{"bookingId":1,"bankName":"KAKAOBANK","accountNumber":"123","accountHolder":"holder"}""",
                BookingRefundRequest::class.java,
            ).bankName,
        )
        val parsedGuestBookingRequest = objectMapper.readValue(
            """{"scheduleId":1,"purchaseTicketCount":2,"scheduleNumber":"FIRST","bookerName":"booker",""" +
                """"bookerPhoneNumber":"010-0000-0000","birthDate":"990101","password":"password",""" +
                """"totalPaymentAmount":20000,"bookingStatus":"CHECKING_PAYMENT"}""",
            GuestBookingRequest::class.java,
        )
        assertEquals(ScheduleNumberType.FIRST, parsedGuestBookingRequest.scheduleNumber)
        assertEquals(BookingStatusType.CHECKING_PAYMENT, parsedGuestBookingRequest.bookingStatus)
        val parsedMemberBookingRequest = objectMapper.readValue(
            """{"scheduleId":1,"scheduleNumber":"FIRST","purchaseTicketCount":2,"bookerName":"booker",""" +
                """"bookerPhoneNumber":"010-0000-0000","bookingStatus":"CHECKING_PAYMENT","totalPaymentAmount":20000}""",
            MemberBookingRequest::class.java,
        )
        assertEquals(ScheduleNumberType.FIRST, parsedMemberBookingRequest.scheduleNumber)
        assertEquals(BookingStatusType.CHECKING_PAYMENT, parsedMemberBookingRequest.bookingStatus)
    }

    @Test
    fun `booking response json string values stay compatible across api boundary migration`() {
        val creationResult = BookingCreationResult(
            bookingId = 10L,
            scheduleId = 1L,
            userId = 30L,
            purchaseTicketCount = 2,
            scheduleNumber = "FIRST",
            bookerName = "booker",
            bookerPhoneNumber = "010-0000-0000",
            bookingStatus = "CHECKING_PAYMENT",
            bankName = "KAKAOBANK",
            accountNumber = "123",
            totalPaymentAmount = 20000,
            createdAt = null,
        )
        val guestBookingResponse = GuestBookingResponse.from(creationResult)
        val memberBookingResponse = MemberBookingResponse.from(creationResult)

        val guestJson = objectMapper.valueToTree<JsonNode>(guestBookingResponse)
        assertTextField(guestJson, "scheduleNumber", "FIRST")
        assertTextField(guestJson, "bookingStatus", "CHECKING_PAYMENT")
        assertTextField(guestJson, "bankName", "KAKAOBANK")

        val memberJson = objectMapper.valueToTree<JsonNode>(memberBookingResponse)
        assertTextField(memberJson, "scheduleNumber", "FIRST")
        assertTextField(memberJson, "bookingStatus", "CHECKING_PAYMENT")
        assertTextField(memberJson, "bankName", "KAKAOBANK")
    }

    @Test
    fun `home response json field names and enum values stay compatible`() {
        val response = HomeFindAllResponse.from(
            HomeFindAllResult(
                promotionList = listOf(
                    HomePromotionResult(1L, "promotion.png", 11L, "redirect", true, "ONE"),
                ),
                performanceList = listOf(
                    HomePerformanceResult(11L, "title", "period", 30000, 3, "BAND", "poster.png", "venue"),
                ),
            ),
        )

        val json = objectMapper.valueToTree<JsonNode>(response)
        val promotion = json.get("promotionList").get(0)
        val performance = json.get("performanceList").get(0)

        assertTrue(json.has("promotionList"))
        assertTrue(json.has("performanceList"))
        assertTrue(promotion.has("carouselNumber"))
        assertBooleanField(promotion, "isExternal", true)
        assertFalse(promotion.has("external"))
        assertTrue(performance.has("genre"))
        assertFalse(promotion.get("carouselNumber").isObject)
        assertFalse(performance.get("genre").isObject)
        assertEquals("ONE", promotion.get("carouselNumber").asText())
        assertEquals("BAND", performance.get("genre").asText())
    }

    @Test
    fun `boolean response json field names stay compatible`() {
        val availability = TicketAvailabilityResponse.from(
            TicketAvailabilityResult(1L, "FIRST", 10, 2, 8, 1, true),
        )
        val detailSchedule = PerformanceDetailScheduleResponse.from(
            PerformanceDetailScheduleResult(1L, null, "FIRST", 3, true),
        )
        val bookingSchedule = BookingPerformanceDetailScheduleResponse.from(
            BookingPerformanceScheduleResult(1L, null, "FIRST", 8, true, 3),
        )
        val modifyDetail = PerformanceModifyDetailResponse.from(
            PerformanceEditResult(
                PerformanceMutationResult(
                    userId = 1L,
                    performanceId = 1L,
                    performanceTitle = "title",
                    genre = "BAND",
                    runningTime = 60,
                    performanceDescription = "description",
                    performanceAttentionNote = "note",
                    bankName = "KAKAOBANK",
                    accountNumber = "123",
                    accountHolder = "holder",
                    posterImage = "poster.png",
                    performanceTeamName = "team",
                    performanceVenue = "venue",
                    roadAddressName = "road",
                    placeDetailAddress = "detail",
                    latitude = "0",
                    longitude = "0",
                    performanceContact = "010",
                    performancePeriod = "period",
                    ticketPrice = 1000,
                    totalScheduleCount = 1,
                    schedules = emptyList(),
                    casts = emptyList(),
                    staffs = emptyList(),
                    images = emptyList(),
                ),
                isBookerExist = true,
            ),
        )

        assertBooleanField(objectMapper.valueToTree(availability), "isAvailable", true)
        assertFalse(objectMapper.valueToTree<JsonNode>(availability).has("available"))
        assertBooleanField(objectMapper.valueToTree(detailSchedule), "isBooking", true)
        assertFalse(objectMapper.valueToTree<JsonNode>(detailSchedule).has("booking"))
        assertBooleanField(objectMapper.valueToTree(bookingSchedule), "isBooking", true)
        assertFalse(objectMapper.valueToTree<JsonNode>(bookingSchedule).has("booking"))
        assertBooleanField(objectMapper.valueToTree(modifyDetail), "isBookerExist", true)
        assertFalse(objectMapper.valueToTree<JsonNode>(modifyDetail).has("bookerExist"))
    }

    private fun enumNames(values: Array<out Enum<*>>): List<String> = values.map { it.name }

    private fun assertSameEnumNames(apiValues: Array<out Enum<*>>, domainValues: Array<out Enum<*>>) {
        assertEquals(enumNames(domainValues), enumNames(apiValues))
    }

    private fun assertTextField(json: JsonNode, fieldName: String, expectedValue: String) {
        assertTrue(json.has(fieldName)) { "Missing JSON field: $fieldName" }
        assertFalse(json.get(fieldName).isObject) { "$fieldName must stay a JSON string" }
        assertEquals(expectedValue, json.get(fieldName).asText())
    }

    private fun assertBooleanField(json: JsonNode, fieldName: String, expectedValue: Boolean) {
        assertTrue(json.has(fieldName)) { "Missing JSON field: $fieldName" }
        assertEquals(expectedValue, json.get(fieldName).booleanValue())
    }
}
