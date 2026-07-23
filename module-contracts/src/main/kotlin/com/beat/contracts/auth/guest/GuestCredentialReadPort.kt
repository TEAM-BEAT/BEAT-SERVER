package com.beat.contracts.auth.guest

import com.beat.contracts.auth.guest.readmodel.GuestCredentialReadModel

fun interface GuestCredentialReadPort {

    fun findCandidates(
        bookerName: String,
        phoneNumber: String,
        birthDate: String,
    ): List<GuestCredentialReadModel>
}
