package com.beat.apps.api

import com.beat.application.frontoffice.booking.booker.result.BookingCreationResult
import com.beat.application.frontoffice.home.booker.query.HomeFindAllResult
import com.beat.application.frontoffice.home.booker.query.HomePerformanceResult
import com.beat.application.frontoffice.home.booker.query.HomePromotionResult
import com.beat.application.frontoffice.member.command.SocialLoginType
import com.beat.application.frontoffice.performance.booker.query.BookingPerformanceScheduleResult
import com.beat.application.frontoffice.performance.booker.query.PerformanceDetailScheduleResult
import com.beat.application.frontoffice.performance.maker.PerformanceMutationResult
import com.beat.application.frontoffice.performance.maker.command.PerformanceBankName
import com.beat.application.frontoffice.performance.maker.command.PerformanceGenre
import com.beat.application.frontoffice.performance.maker.command.PerformanceScheduleNumber
import com.beat.application.frontoffice.performance.maker.query.PerformanceEditResult
import com.beat.application.frontoffice.schedule.booker.query.TicketAvailabilityResult
import com.beat.application.frontoffice.ticket.maker.command.TicketBookingStatus
import com.beat.apps.api.booking.api.request.BookingRefundRequest
import com.beat.apps.api.booking.api.request.GuestBookingRequest
import com.beat.apps.api.booking.api.request.MemberBookingRequest
import com.beat.apps.api.booking.api.response.GuestBookingResponse
import com.beat.apps.api.booking.api.response.MemberBookingResponse
import com.beat.apps.api.booking.api.type.BookingStatusType
import com.beat.apps.api.home.api.response.HomeFindAllResponse
import com.beat.apps.api.home.api.type.HomeGenreType
import com.beat.apps.api.member.api.request.MemberLoginRequest
import com.beat.apps.api.member.api.type.SocialTypeRequest
import com.beat.apps.api.performance.api.response.BookingPerformanceDetailScheduleResponse
import com.beat.apps.api.performance.api.response.PerformanceDetailScheduleResponse
import com.beat.apps.api.performance.api.response.PerformanceModifyDetailResponse
import com.beat.apps.api.performance.api.type.BankNameType
import com.beat.apps.api.performance.api.type.GenreType
import com.beat.apps.api.schedule.api.response.TicketAvailabilityResponse
import com.beat.apps.api.schedule.api.type.ScheduleNumberType
import com.beat.apps.api.ticket.api.request.TicketRefundRequest
import com.beat.domain.booking.model.BookingStatus
import com.beat.domain.member.model.SocialType
import com.beat.domain.performance.model.Genre
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.sharedkernel.vo.BankName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class ApisJsonCompatibilitySpec :
    FunSpec({

        // 프로덕션 Boot ObjectMapper와 동일한 계약으로 고정: unknown field 허용 + JSR310 지원.
        val objectMapper =
            jacksonObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .findAndRegisterModules()

        test("필수 request 컬렉션은 역직렬화 단계에서 누락과 null을 거부한다") {
            // Kotlin non-null 프로퍼티는 타입 시스템이 필수성을 보증한다(Bean Validation 불필요).
            shouldThrow<Exception> {
                objectMapper.readValue("""{"performanceId":1}""", TicketRefundRequest::class.java)
            }
            shouldThrow<Exception> {
                objectMapper.readValue(
                    """{"performanceId":1,"bookingList":null}""",
                    TicketRefundRequest::class.java,
                )
            }
        }

        test("refund request의 환불 계좌 필드는 JSON 누락과 null을 거부한다") {
            listOf(
                    """{"bookingId":1,"accountNumber":"123","accountHolder":"holder"}""",
                    """{"bookingId":1,"bankName":"KAKAOBANK","accountHolder":"holder"}""",
                    """{"bookingId":1,"bankName":"KAKAOBANK","accountNumber":"123"}""",
                    """{"bookingId":1,"bankName":null,"accountNumber":"123","accountHolder":"holder"}""",
                    """{"bookingId":1,"bankName":"KAKAOBANK","accountNumber":null,"accountHolder":"holder"}""",
                    """{"bookingId":1,"bankName":"KAKAOBANK","accountNumber":"123","accountHolder":null}""",
                )
                .forEach { json ->
                    shouldThrow<Exception> {
                        objectMapper.readValue(json, BookingRefundRequest::class.java)
                    }
                }
        }

        test("api 경계 마이그레이션 전후로 api enum 이름이 호환된다") {
            withClue("API enum names") {
                enumNames(SocialTypeRequest.entries.toTypedArray()) shouldBe listOf("KAKAO")
                enumNames(GenreType.entries.toTypedArray()) shouldBe
                    listOf("BAND", "PLAY", "DANCE", "ETC")
                enumNames(HomeGenreType.entries.toTypedArray()) shouldBe
                    listOf("BAND", "PLAY", "DANCE", "ETC")
                enumNames(ScheduleNumberType.entries.toTypedArray()) shouldBe
                    listOf(
                        "FIRST",
                        "SECOND",
                        "THIRD",
                        "FOURTH",
                        "FIFTH",
                        "SIXTH",
                        "SEVENTH",
                        "EIGHTH",
                        "NINTH",
                        "TENTH",
                    )
                enumNames(BankNameType.entries.toTypedArray()) shouldBe
                    listOf(
                        "NH_NONGHYUP",
                        "KAKAOBANK",
                        "KB_KOOKMIN",
                        "TOSSBANK",
                        "SHINHAN",
                        "WOORI",
                        "IBK_GIUP",
                        "HANA",
                        "SAEMAUL",
                        "BUSAN",
                        "IMBANK_DAEGU",
                        "SINHYEOP",
                        "WOOCHAEGUK",
                        "SCJEIL",
                        "SUHYEOP",
                        "NONE",
                    )
                enumNames(BookingStatusType.entries.toTypedArray()) shouldBe
                    listOf(
                        "CHECKING_PAYMENT",
                        "BOOKING_CONFIRMED",
                        "BOOKING_CANCELLED",
                        "REFUND_REQUESTED",
                        "BOOKING_DELETED",
                    )
            }

            withClue("API and domain/application enum parity") {
                assertSameEnumNames(
                    SocialTypeRequest.entries.toTypedArray(),
                    SocialType.entries.toTypedArray(),
                )
                assertSameEnumNames(GenreType.entries.toTypedArray(), Genre.entries.toTypedArray())
                assertSameEnumNames(
                    HomeGenreType.entries.toTypedArray(),
                    Genre.entries.toTypedArray(),
                )
                assertSameEnumNames(
                    ScheduleNumberType.entries.toTypedArray(),
                    ScheduleNumber.entries.toTypedArray(),
                )
                assertSameEnumNames(
                    BankNameType.entries.toTypedArray(),
                    BankName.entries.toTypedArray(),
                )
                assertSameEnumNames(
                    BookingStatusType.entries.toTypedArray(),
                    BookingStatus.entries.toTypedArray(),
                )
                assertSameEnumNames(
                    SocialTypeRequest.entries.toTypedArray(),
                    SocialLoginType.entries.toTypedArray(),
                )
                assertSameEnumNames(
                    GenreType.entries.toTypedArray(),
                    PerformanceGenre.entries.toTypedArray(),
                )
                assertSameEnumNames(
                    BankNameType.entries.toTypedArray(),
                    PerformanceBankName.entries.toTypedArray(),
                )
                assertSameEnumNames(
                    ScheduleNumberType.entries.toTypedArray(),
                    PerformanceScheduleNumber.entries.toTypedArray(),
                )
                assertSameEnumNames(
                    BookingStatusType.entries.toTypedArray(),
                    TicketBookingStatus.entries.toTypedArray(),
                )
            }
        }

        test("api 경계 마이그레이션 전후로 domain enum request JSON 문자열 값이 호환된다") {
            val memberLoginRequest = MemberLoginRequest(SocialTypeRequest.KAKAO)
            val bookingRefundRequest =
                BookingRefundRequest(1L, BankNameType.KAKAOBANK, "123", "holder")
            val guestBookingRequest =
                GuestBookingRequest(
                    1L,
                    2,
                    "booker",
                    "010-0000-0000",
                    "990101",
                    "password",
                )
            val memberBookingRequest =
                MemberBookingRequest(
                    1L,
                    2,
                    "booker",
                    "010-0000-0000",
                )

            withClue("request enum serialization") {
                assertTextField(objectMapper.valueToTree(memberLoginRequest), "socialType", "KAKAO")
                assertTextField(
                    objectMapper.valueToTree(bookingRefundRequest),
                    "bankName",
                    "KAKAOBANK",
                )
            }

            withClue("request enum deserialization") {
                objectMapper
                    .readValue("""{"socialType":"KAKAO"}""", MemberLoginRequest::class.java)
                    .socialType shouldBe SocialTypeRequest.KAKAO
                objectMapper
                    .readValue(
                        """{"bookingId":1,"bankName":"KAKAOBANK","accountNumber":"123","accountHolder":"holder"}""",
                        BookingRefundRequest::class.java,
                    )
                    .bankName shouldBe BankNameType.KAKAOBANK

                // Legacy clients still send retired fields
                // (scheduleNumber/totalPaymentAmount/bookingStatus);
                // unknown properties must stay tolerated on the request boundary.
                val parsedGuestBookingRequest =
                    objectMapper.readValue(
                        """{"scheduleId":1,"purchaseTicketCount":2,"scheduleNumber":"FIRST","bookerName":"booker",""" +
                            """"bookerPhoneNumber":"010-0000-0000","birthDate":"990101","password":"password",""" +
                            """"totalPaymentAmount":20000,"bookingStatus":"CHECKING_PAYMENT"}""",
                        GuestBookingRequest::class.java,
                    )
                parsedGuestBookingRequest.purchaseTicketCount shouldBe 2

                val parsedMemberBookingRequest =
                    objectMapper.readValue(
                        """{"scheduleId":1,"scheduleNumber":"FIRST","purchaseTicketCount":2,"bookerName":"booker",""" +
                            """"bookerPhoneNumber":"010-0000-0000","bookingStatus":"CHECKING_PAYMENT","totalPaymentAmount":20000}""",
                        MemberBookingRequest::class.java,
                    )
                parsedMemberBookingRequest.purchaseTicketCount shouldBe 2
            }
        }

        test("api 경계 마이그레이션 전후로 booking response JSON 문자열 값이 호환된다") {
            val creationResult =
                BookingCreationResult(
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
                    createdAt = LocalDateTime.parse("2026-04-01T12:00:00"),
                )
            val guestBookingResponse = GuestBookingResponse.from(creationResult)
            val memberBookingResponse = MemberBookingResponse.from(creationResult)

            withClue("guest booking response") {
                val guestJson = objectMapper.valueToTree<JsonNode>(guestBookingResponse)
                assertTextField(guestJson, "scheduleNumber", "FIRST")
                assertTextField(guestJson, "bookingStatus", "CHECKING_PAYMENT")
                assertTextField(guestJson, "bankName", "KAKAOBANK")
            }
            withClue("member booking response") {
                val memberJson = objectMapper.valueToTree<JsonNode>(memberBookingResponse)
                assertTextField(memberJson, "scheduleNumber", "FIRST")
                assertTextField(memberJson, "bookingStatus", "CHECKING_PAYMENT")
                assertTextField(memberJson, "bankName", "KAKAOBANK")
            }
        }

        test("home response의 JSON 필드명과 enum 값이 호환된다") {
            val response =
                HomeFindAllResponse.from(
                    HomeFindAllResult(
                        promotionList =
                            listOf(
                                HomePromotionResult(
                                    1L,
                                    "promotion.png",
                                    11L,
                                    "redirect",
                                    true,
                                    "ONE",
                                )
                            ),
                        performanceList =
                            listOf(
                                HomePerformanceResult(
                                    11L,
                                    "title",
                                    "period",
                                    30000,
                                    3,
                                    "BAND",
                                    "poster.png",
                                    "venue",
                                )
                            ),
                    )
                )

            val json = objectMapper.valueToTree<JsonNode>(response)
            val promotion = json.get("promotionList").get(0)
            val performance = json.get("performanceList").get(0)

            withClue("home response field names") {
                json.has("promotionList") shouldBe true
                json.has("performanceList") shouldBe true
                promotion.has("carouselNumber") shouldBe true
                assertBooleanField(promotion, "isExternal", true)
                promotion.has("external") shouldBe false
                performance.has("genre") shouldBe true
            }
            withClue("home response enum strings") {
                promotion.get("carouselNumber").isObject shouldBe false
                performance.get("genre").isObject shouldBe false
                promotion.get("carouselNumber").asText() shouldBe "ONE"
                performance.get("genre").asText() shouldBe "BAND"
            }
        }

        test("boolean response의 JSON 필드명이 호환된다") {
            val availability =
                TicketAvailabilityResponse.from(
                    TicketAvailabilityResult(1L, "FIRST", 10, 2, 8, 1, true)
                )
            val detailSchedule =
                PerformanceDetailScheduleResponse.from(
                    PerformanceDetailScheduleResult(1L, null, "FIRST", 3, true)
                )
            val bookingSchedule =
                BookingPerformanceDetailScheduleResponse.from(
                    BookingPerformanceScheduleResult(1L, null, "FIRST", 8, true, 3)
                )
            val modifyDetail =
                PerformanceModifyDetailResponse.from(
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
                    )
                )

            withClue("availability response") {
                val json = objectMapper.valueToTree<JsonNode>(availability)
                assertBooleanField(json, "isAvailable", true)
                json.has("available") shouldBe false
            }
            withClue("performance detail schedule response") {
                val json = objectMapper.valueToTree<JsonNode>(detailSchedule)
                assertBooleanField(json, "isBooking", true)
                json.has("booking") shouldBe false
            }
            withClue("booking performance schedule response") {
                val json = objectMapper.valueToTree<JsonNode>(bookingSchedule)
                assertBooleanField(json, "isBooking", true)
                json.has("booking") shouldBe false
            }
            withClue("performance modify detail response") {
                val json = objectMapper.valueToTree<JsonNode>(modifyDetail)
                assertBooleanField(json, "isBookerExist", true)
                json.has("bookerExist") shouldBe false
            }
        }
    })

private fun enumNames(values: Array<out Enum<*>>): List<String> = values.map { it.name }

private fun assertSameEnumNames(apiValues: Array<out Enum<*>>, domainValues: Array<out Enum<*>>) {
    enumNames(domainValues) shouldBe enumNames(apiValues)
}

private fun assertTextField(json: JsonNode, fieldName: String, expectedValue: String) {
    withClue("Missing JSON field: $fieldName") {
        json.has(fieldName) shouldBe true
    }
    withClue("$fieldName must stay a JSON string") {
        json.get(fieldName).isObject shouldBe false
    }
    json.get(fieldName).asText() shouldBe expectedValue
}

private fun assertBooleanField(json: JsonNode, fieldName: String, expectedValue: Boolean) {
    withClue("Missing JSON field: $fieldName") {
        json.has(fieldName) shouldBe true
    }
    json.get(fieldName).booleanValue() shouldBe expectedValue
}
