-- BEAT jOOQ codegen DDL source — deterministic, no live DB required
-- MySQL 8.0 compatible, parsed by jOOQ DDLDatabase (H2 interpreter)
-- This file is the single source for jOOQ generated Tables. Keep in sync with JPA entities and ops DDL.
-- Last sync: 2026-08-25 — CONTRACT 이후 최신 DDL (performance_start_date/end_date NOT NULL, booking.total_payment_amount NOT NULL, CHECK/fulltext는 운영에만)

CREATE TABLE `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `role` VARCHAR(10) NOT NULL DEFAULT 'USER'
);

CREATE TABLE `member` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `nickname` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255),
  `deleted_at` DATETIME(6),
  `user_id` BIGINT NOT NULL,
  `social_id` BIGINT NOT NULL,
  `social_type` VARCHAR(255) NOT NULL,
  `created_at` DATETIME(6),
  `updated_at` DATETIME(6),
  CONSTRAINT `uk_member_user_id` UNIQUE (`user_id`),
  CONSTRAINT `uk_member_social_identity` UNIQUE (`social_type`, `social_id`)
);

CREATE TABLE `performance` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `performance_title` VARCHAR(255) NOT NULL,
  `genre` VARCHAR(255) NOT NULL,
  `running_time` INT NOT NULL,
  `performance_description` VARCHAR(1500) NOT NULL,
  `performance_attention_note` VARCHAR(1500) NOT NULL,
  `bank_name` VARCHAR(255),
  `account_number` VARCHAR(255),
  `account_holder` VARCHAR(255),
  `poster_image` VARCHAR(255) NOT NULL,
  `performance_team_name` VARCHAR(255) NOT NULL,
  `performance_venue` TEXT NOT NULL,
  `road_address_name` VARCHAR(255) NOT NULL,
  `place_detail_address` VARCHAR(255) NOT NULL,
  `latitude` VARCHAR(255) NOT NULL,
  `longitude` VARCHAR(255) NOT NULL,
  `performance_contact` VARCHAR(255) NOT NULL,
  `performance_start_date` DATE NOT NULL,
  `performance_end_date` DATE NOT NULL,
  `ticket_price` INT NOT NULL,
  `total_schedule_count` INT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `created_at` DATETIME(6),
  `updated_at` DATETIME(6)
  -- CHECK chk_performance_payment_account_complete, chk_performance_period_complete는 운영 DDL에만 존재 (H2 파싱 회피)
);

CREATE TABLE `schedule` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `performance_date` DATETIME(6) NOT NULL,
  `booking_close_at` DATETIME(6) NOT NULL,
  `total_ticket_count` INT NOT NULL,
  `sold_ticket_count` INT NOT NULL,
  `schedule_number` VARCHAR(255) NOT NULL,
  `performance_id` BIGINT NOT NULL
);

CREATE TABLE `booking` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `purchase_ticket_count` INT NOT NULL,
  `booker_name` VARCHAR(255) NOT NULL,
  `booker_phone_number` VARCHAR(255) NOT NULL,
  `booking_status` VARCHAR(255) NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `cancellation_date` DATETIME(6),
  `birth_date` VARCHAR(255),
  `password` VARCHAR(255),
  `bank_name` VARCHAR(255),
  `account_number` VARCHAR(255),
  `account_holder` VARCHAR(255),
  `total_payment_amount` INT NOT NULL,
  `schedule_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `updated_at` DATETIME(6)
  -- CHECK chk_booking_refund_account_complete_v2는 운영 DDL에만 존재
);

CREATE TABLE `cast` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `cast_name` VARCHAR(255) NOT NULL,
  `cast_role` VARCHAR(255) NOT NULL,
  `cast_photo` VARCHAR(255) NOT NULL,
  `performance_id` BIGINT NOT NULL
);

CREATE TABLE `staff` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `staff_name` VARCHAR(255) NOT NULL,
  `staff_role` VARCHAR(255) NOT NULL,
  `staff_photo` VARCHAR(255) NOT NULL,
  `performance_id` BIGINT NOT NULL
);

CREATE TABLE `performance_image` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `performance_image_url` VARCHAR(255) NOT NULL,
  `performance_id` BIGINT NOT NULL
);

CREATE TABLE `promotion` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `promotion_photo` VARCHAR(255) NOT NULL,
  `performance_id` BIGINT,
  `redirect_url` VARCHAR(255) NOT NULL,
  `is_external` BOOLEAN NOT NULL,
  `carousel_number` VARCHAR(255) NOT NULL
);

-- 운영 DDL에만 존재: create fulltext index ft_booker_name on booking (booker_name);
-- jOOQ DDLDatabase(H2)는 fulltext index 파싱을 지원하지 않아 주석 처리
