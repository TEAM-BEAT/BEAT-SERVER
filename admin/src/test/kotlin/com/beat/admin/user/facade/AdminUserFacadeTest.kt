package com.beat.admin.user.facade

import com.beat.admin.user.application.query.AdminUserQueryService
import com.beat.admin.user.application.result.AdminUserResults
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class AdminUserFacadeTest {

    @Test
    fun facadeDelegatesUserScenarioToQueryService() {
        val queryService = mock(AdminUserQueryService::class.java)
        val adminUserFacade = AdminUserFacade(queryService)
        `when`(queryService.findAllUsers(1L)).thenReturn(AdminUserResults(emptyList()))

        adminUserFacade.checkMemberAndFindAllUsers(1L)

        verify(queryService).findAllUsers(1L)
    }
}