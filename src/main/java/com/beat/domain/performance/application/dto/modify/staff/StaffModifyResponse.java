package com.beat.domain.performance.application.dto.modify.staff;

public record StaffModifyResponse(Long staffId,
                                  String staffName,
                                  String staffRole,
                                  String staffPhoto) {

    public static StaffModifyResponse of(Long staffId, String staffName, String staffRole, String staffPhoto) {
        return new StaffModifyResponse(staffId, staffName, staffRole, staffPhoto);
    }
}