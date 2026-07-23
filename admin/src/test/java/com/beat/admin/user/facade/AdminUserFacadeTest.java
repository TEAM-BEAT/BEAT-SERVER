package com.beat.admin.user.facade;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.beat.admin.user.application.query.AdminUserQueryService;
import com.beat.admin.user.application.result.AdminUserResults;

class AdminUserFacadeTest {

	@Test
	void facadeDelegatesUserScenarioToQueryService() {
		AdminUserQueryService queryService = mock(AdminUserQueryService.class);
		AdminUserFacade adminUserFacade = new AdminUserFacade(queryService);
		when(queryService.findAllUsers(1L)).thenReturn(new AdminUserResults(List.of()));

		adminUserFacade.checkMemberAndFindAllUsers(1L);

		verify(queryService).findAllUsers(1L);
	}
}
