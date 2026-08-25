package com.beat.infrastructure.jooq.generated

import org.jooq.Field
import org.jooq.Table
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import java.time.LocalDate
import java.time.LocalDateTime

object Users {
    val TABLE: Table<*> = DSL.table(DSL.name("users"))
    val ID: Field<Long> = DSL.field(DSL.name("users", "id"), SQLDataType.BIGINT)
    val ROLE: Field<String> = DSL.field(DSL.name("users", "role"), SQLDataType.VARCHAR)
}

object Member {
    val TABLE: Table<*> = DSL.table(DSL.name("member"))
    val ID: Field<Long> = DSL.field(DSL.name("member", "id"), SQLDataType.BIGINT)
    val CREATED_AT: Field<LocalDateTime?> = DSL.field(DSL.name("member", "created_at"), SQLDataType.LOCALDATETIME.nullable(true))
    val DELETED_AT: Field<LocalDateTime?> = DSL.field(DSL.name("member", "deleted_at"), SQLDataType.LOCALDATETIME.nullable(true))
    val UPDATED_AT: Field<LocalDateTime?> = DSL.field(DSL.name("member", "updated_at"), SQLDataType.LOCALDATETIME.nullable(true))
    val SOCIAL_ID: Field<Long> = DSL.field(DSL.name("member", "social_id"), SQLDataType.BIGINT)
    val USER_ID: Field<Long> = DSL.field(DSL.name("member", "user_id"), SQLDataType.BIGINT)
    val EMAIL: Field<String?> = DSL.field(DSL.name("member", "email"), SQLDataType.VARCHAR.nullable(true))
    val NICKNAME: Field<String> = DSL.field(DSL.name("member", "nickname"), SQLDataType.VARCHAR)
    val SOCIAL_TYPE: Field<String> = DSL.field(DSL.name("member", "social_type"), SQLDataType.VARCHAR)
}

object Booking {
    val TABLE: Table<*> = DSL.table(DSL.name("booking"))
    val ID: Field<Long> = DSL.field(DSL.name("booking", "id"), SQLDataType.BIGINT)
    val PURCHASE_TICKET_COUNT: Field<Int> = DSL.field(DSL.name("booking", "purchase_ticket_count"), SQLDataType.INTEGER)
    val BOOKER_NAME: Field<String> = DSL.field(DSL.name("booking", "booker_name"), SQLDataType.VARCHAR)
    val BOOKER_PHONE_NUMBER: Field<String> = DSL.field(DSL.name("booking", "booker_phone_number"), SQLDataType.VARCHAR)
    val BOOKING_STATUS: Field<String> = DSL.field(DSL.name("booking", "booking_status"), SQLDataType.VARCHAR)
    val CREATED_AT: Field<LocalDateTime> = DSL.field(DSL.name("booking", "created_at"), SQLDataType.LOCALDATETIME)
    val CANCELLATION_DATE: Field<LocalDateTime?> = DSL.field(DSL.name("booking", "cancellation_date"), SQLDataType.LOCALDATETIME.nullable(true))
    val BIRTH_DATE: Field<String?> = DSL.field(DSL.name("booking", "birth_date"), SQLDataType.VARCHAR.nullable(true))
    val PASSWORD: Field<String?> = DSL.field(DSL.name("booking", "password"), SQLDataType.VARCHAR.nullable(true))
    val TOTAL_PAYMENT_AMOUNT: Field<Int> = DSL.field(DSL.name("booking", "total_payment_amount"), SQLDataType.INTEGER)
    val BANK_NAME: Field<String?> = DSL.field(DSL.name("booking", "bank_name"), SQLDataType.VARCHAR.nullable(true))
    val ACCOUNT_NUMBER: Field<String?> = DSL.field(DSL.name("booking", "account_number"), SQLDataType.VARCHAR.nullable(true))
    val ACCOUNT_HOLDER: Field<String?> = DSL.field(DSL.name("booking", "account_holder"), SQLDataType.VARCHAR.nullable(true))
    val SCHEDULE_ID: Field<Long> = DSL.field(DSL.name("booking", "schedule_id"), SQLDataType.BIGINT)
    val USER_ID: Field<Long> = DSL.field(DSL.name("booking", "user_id"), SQLDataType.BIGINT)
}

object Schedule {
    val TABLE: Table<*> = DSL.table(DSL.name("schedule"))
    val ID: Field<Long> = DSL.field(DSL.name("schedule", "id"), SQLDataType.BIGINT)
    val PERFORMANCE_DATE: Field<LocalDateTime> = DSL.field(DSL.name("schedule", "performance_date"), SQLDataType.LOCALDATETIME)
    val BOOKING_CLOSE_AT: Field<LocalDateTime> = DSL.field(DSL.name("schedule", "booking_close_at"), SQLDataType.LOCALDATETIME)
    val TOTAL_TICKET_COUNT: Field<Int> = DSL.field(DSL.name("schedule", "total_ticket_count"), SQLDataType.INTEGER)
    val SOLD_TICKET_COUNT: Field<Int> = DSL.field(DSL.name("schedule", "sold_ticket_count"), SQLDataType.INTEGER)
    val SCHEDULE_NUMBER: Field<String> = DSL.field(DSL.name("schedule", "schedule_number"), SQLDataType.VARCHAR)
    val PERFORMANCE_ID: Field<Long> = DSL.field(DSL.name("schedule", "performance_id"), SQLDataType.BIGINT)
}

object Performance {
    val TABLE: Table<*> = DSL.table(DSL.name("performance"))
    val ID: Field<Long> = DSL.field(DSL.name("performance", "id"), SQLDataType.BIGINT)
    val PERFORMANCE_TITLE: Field<String> = DSL.field(DSL.name("performance", "performance_title"), SQLDataType.VARCHAR)
    val GENRE: Field<String> = DSL.field(DSL.name("performance", "genre"), SQLDataType.VARCHAR)
    val RUNNING_TIME: Field<Int> = DSL.field(DSL.name("performance", "running_time"), SQLDataType.INTEGER)
    val PERFORMANCE_DESCRIPTION: Field<String> = DSL.field(DSL.name("performance", "performance_description"), SQLDataType.VARCHAR)
    val PERFORMANCE_ATTENTION_NOTE: Field<String> = DSL.field(DSL.name("performance", "performance_attention_note"), SQLDataType.VARCHAR)
    val BANK_NAME: Field<String?> = DSL.field(DSL.name("performance", "bank_name"), SQLDataType.VARCHAR.nullable(true))
    val ACCOUNT_NUMBER: Field<String?> = DSL.field(DSL.name("performance", "account_number"), SQLDataType.VARCHAR.nullable(true))
    val ACCOUNT_HOLDER: Field<String?> = DSL.field(DSL.name("performance", "account_holder"), SQLDataType.VARCHAR.nullable(true))
    val POSTER_IMAGE: Field<String> = DSL.field(DSL.name("performance", "poster_image"), SQLDataType.VARCHAR)
    val PERFORMANCE_TEAM_NAME: Field<String> = DSL.field(DSL.name("performance", "performance_team_name"), SQLDataType.VARCHAR)
    val PERFORMANCE_VENUE: Field<String> = DSL.field(DSL.name("performance", "performance_venue"), SQLDataType.VARCHAR)
    val ROAD_ADDRESS_NAME: Field<String> = DSL.field(DSL.name("performance", "road_address_name"), SQLDataType.VARCHAR)
    val PLACE_DETAIL_ADDRESS: Field<String> = DSL.field(DSL.name("performance", "place_detail_address"), SQLDataType.VARCHAR)
    val LATITUDE: Field<String> = DSL.field(DSL.name("performance", "latitude"), SQLDataType.VARCHAR)
    val LONGITUDE: Field<String> = DSL.field(DSL.name("performance", "longitude"), SQLDataType.VARCHAR)
    val PERFORMANCE_CONTACT: Field<String> = DSL.field(DSL.name("performance", "performance_contact"), SQLDataType.VARCHAR)
    val PERFORMANCE_START_DATE: Field<LocalDate> = DSL.field(DSL.name("performance", "performance_start_date"), SQLDataType.LOCALDATE)
    val PERFORMANCE_END_DATE: Field<LocalDate> = DSL.field(DSL.name("performance", "performance_end_date"), SQLDataType.LOCALDATE)
    val TICKET_PRICE: Field<Int> = DSL.field(DSL.name("performance", "ticket_price"), SQLDataType.INTEGER)
    val TOTAL_SCHEDULE_COUNT: Field<Int> = DSL.field(DSL.name("performance", "total_schedule_count"), SQLDataType.INTEGER)
    val USER_ID: Field<Long> = DSL.field(DSL.name("performance", "user_id"), SQLDataType.BIGINT)
    val CREATED_AT: Field<LocalDateTime?> = DSL.field(DSL.name("performance", "created_at"), SQLDataType.LOCALDATETIME.nullable(true))
    val UPDATED_AT: Field<LocalDateTime?> = DSL.field(DSL.name("performance", "updated_at"), SQLDataType.LOCALDATETIME.nullable(true))
}

object Promotion {
    val TABLE: Table<*> = DSL.table(DSL.name("promotion"))
    val ID: Field<Long> = DSL.field(DSL.name("promotion", "id"), SQLDataType.BIGINT)
    val PROMOTION_PHOTO: Field<String> = DSL.field(DSL.name("promotion", "promotion_photo"), SQLDataType.VARCHAR)
    val PERFORMANCE_ID: Field<Long?> = DSL.field(DSL.name("promotion", "performance_id"), SQLDataType.BIGINT.nullable(true))
    val REDIRECT_URL: Field<String> = DSL.field(DSL.name("promotion", "redirect_url"), SQLDataType.VARCHAR)
    val IS_EXTERNAL: Field<Boolean> = DSL.field(DSL.name("promotion", "is_external"), SQLDataType.BOOLEAN)
    val CAROUSEL_NUMBER: Field<String> = DSL.field(DSL.name("promotion", "carousel_number"), SQLDataType.VARCHAR)
}

object CastTable {
    val TABLE: Table<*> = DSL.table(DSL.name("cast"))
    val ID: Field<Long> = DSL.field(DSL.name("cast", "id"), SQLDataType.BIGINT)
    val CAST_NAME: Field<String> = DSL.field(DSL.name("cast", "cast_name"), SQLDataType.VARCHAR)
    val CAST_ROLE: Field<String> = DSL.field(DSL.name("cast", "cast_role"), SQLDataType.VARCHAR)
    val CAST_PHOTO: Field<String> = DSL.field(DSL.name("cast", "cast_photo"), SQLDataType.VARCHAR)
    val PERFORMANCE_ID: Field<Long> = DSL.field(DSL.name("cast", "performance_id"), SQLDataType.BIGINT)
}

object StaffTable {
    val TABLE: Table<*> = DSL.table(DSL.name("staff"))
    val ID: Field<Long> = DSL.field(DSL.name("staff", "id"), SQLDataType.BIGINT)
    val STAFF_NAME: Field<String> = DSL.field(DSL.name("staff", "staff_name"), SQLDataType.VARCHAR)
    val STAFF_ROLE: Field<String> = DSL.field(DSL.name("staff", "staff_role"), SQLDataType.VARCHAR)
    val STAFF_PHOTO: Field<String> = DSL.field(DSL.name("staff", "staff_photo"), SQLDataType.VARCHAR)
    val PERFORMANCE_ID: Field<Long> = DSL.field(DSL.name("staff", "performance_id"), SQLDataType.BIGINT)
}

object PerformanceImage {
    val TABLE: Table<*> = DSL.table(DSL.name("performance_image"))
    val ID: Field<Long> = DSL.field(DSL.name("performance_image", "id"), SQLDataType.BIGINT)
    val PERFORMANCE_IMAGE_URL: Field<String> = DSL.field(DSL.name("performance_image", "performance_image_url"), SQLDataType.VARCHAR)
    val PERFORMANCE_ID: Field<Long> = DSL.field(DSL.name("performance_image", "performance_id"), SQLDataType.BIGINT)
}
