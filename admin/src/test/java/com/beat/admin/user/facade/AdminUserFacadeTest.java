package com.beat.admin.user.facade;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.beat.admin.user.application.service.query.AdminUserQueryService;

class AdminUserFacadeTest {

	@Test
	void facadeDelegatesUserScenarioToQueryService() {
		AdminUserQueryService queryService = mock(AdminUserQueryService.class);
		AdminUserFacade adminUserFacade = new AdminUserFacade(queryService);

		adminUserFacade.checkMemberAndFindAllUsers(1L);

		verify(queryService).findAllUsers(1L);
	}
}
