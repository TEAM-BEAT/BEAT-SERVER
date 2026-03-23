package com.beat.contracts.notification;

public interface BookingNotificationPort {

	void send(BookingNotification notification);
}
