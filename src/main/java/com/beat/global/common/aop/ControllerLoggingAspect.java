package com.beat.global.common.aop;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import net.minidev.json.JSONObject;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Order(2)
@Component
@Profile("!test")
public class ControllerLoggingAspect {

	private static final String REQUEST_URI = "requestURI";
	private static final String CONTROLLER = "controller";
	private static final String METHOD = "method";
	private static final String HTTP_METHOD = "httpMethod";
	private static final String LOG_TIME = "logTime";
	private static final String PARAMS = "params";

	/** Controller 요청 로깅 */
	@Before("com.beat.global.common.aop.Pointcuts.allController()")
	public void logControllerRequest(JoinPoint joinPoint) {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attributes == null) return;

		HttpServletRequest request = attributes.getRequest();
		Map<String, Object> logInfo = new HashMap<>();

		logInfo.put(CONTROLLER, joinPoint.getSignature().getDeclaringType().getSimpleName());
		logInfo.put(METHOD, joinPoint.getSignature().getName());
		logInfo.put(PARAMS, getParams(request));
		logInfo.put(LOG_TIME, System.currentTimeMillis());
		logInfo.put(HTTP_METHOD, request.getMethod());

		try {
			logInfo.put(REQUEST_URI, URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8));
		} catch (Exception e) {
			logInfo.put(REQUEST_URI, request.getRequestURI());
			log.error("[로깅 에러] URL 디코딩 실패", e);
		}

		log.info("[HTTP {}] {} | {}.{}() | Params: {}",
			logInfo.get(HTTP_METHOD), logInfo.get(REQUEST_URI),
			logInfo.get(CONTROLLER), logInfo.get(METHOD),
			logInfo.get(PARAMS));
	}

	/** Controller 정상 반환 로깅 */
	@AfterReturning(value = "com.beat.global.common.aop.Pointcuts.allController()", returning = "result")
	public void logControllerResponse(JoinPoint joinPoint, Object result) {
		log.debug("[Controller 정상 반환] {}.{}() | 반환 값: {}",
			joinPoint.getSignature().getDeclaringType().getSimpleName(),
			joinPoint.getSignature().getName(),
			result);
	}

	/** Controller 예외 발생 시 로깅 */
	@AfterThrowing(value = "com.beat.global.common.aop.Pointcuts.allController()", throwing = "ex")
	public void logControllerException(JoinPoint joinPoint, Exception ex) {
		log.error("[Controller 예외 발생] {}.{}() | 예외 메시지: {}",
			joinPoint.getSignature().getDeclaringType().getSimpleName(),
			joinPoint.getSignature().getName(),
			ex.getMessage(), ex);
	}

	/** HTTP 요청 파라미터를 JSON 형태로 변환 */
	private static JSONObject getParams(HttpServletRequest request) {
		JSONObject jsonObject = new JSONObject();
		Enumeration<String> params = request.getParameterNames();

		while (params.hasMoreElements()) {
			String param = params.nextElement();
			String replacedParam = param.replace(".", "-");
			String[] values = request.getParameterValues(param);

			if (values == null || values.length == 0) {
				jsonObject.put(replacedParam, ""); // 값이 없을 경우 빈 문자열 저장
			} else if (values.length > 1) {
				jsonObject.put(replacedParam, values); // 여러 값이 있는 경우 배열로 저장
			} else {
				jsonObject.put(replacedParam, values[0]); // 단일 값이면 문자열로 저장
			}
		}
		return jsonObject;
	}
}
