package com.beat.infra.config;

import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

// 임시 진단용 — "Only one AsyncConfigurer may exist" 의 source 식별.
// ApplicationContext 계층 (main + management) 을 따라 올라가며 각 context 에 등록된
// AsyncConfigurer bean 의 name / impl / identityHashCode 를 startup 시 1회 출력.
// 원인 식별 후 본 파일은 삭제 예정.
@Configuration(proxyBeanMethods = false)
public class AsyncConfigurerDiagnostic {

	private static final Logger log = LoggerFactory.getLogger(AsyncConfigurerDiagnostic.class);

	@Autowired
	private ApplicationContext context;

	@PostConstruct
	void dumpAsyncConfigurers() {
		ApplicationContext ctx = context;
		int depth = 0;
		while (ctx != null) {
			log.warn("[ASYNC_DIAG] depth={} contextId={} class={}",
				depth, ctx.getId(), ctx.getClass().getName());
			Map<String, AsyncConfigurer> beans = ctx.getBeansOfType(AsyncConfigurer.class);
			log.warn("[ASYNC_DIAG]   beans count = {}", beans.size());
			beans.forEach((name, bean) -> log.warn(
				"[ASYNC_DIAG]   bean='{}' impl={} hash={}",
				name, bean.getClass().getName(), System.identityHashCode(bean)));
			ApplicationContext parent = ctx.getParent();
			log.warn("[ASYNC_DIAG]   parent={}", parent == null ? "null" : parent.getId());
			ctx = parent;
			depth++;
		}
	}
}
