package com.decoraciones.common.decorators;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@Slf4j
public class CurrentUserIdResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentUserId.class) && Long.class.equals(parameter.getParameterType());
	}

	@Override
	public Long resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof Jwt jwt) {
			Object uidClaim = jwt.getClaim("uid");
			Long uid = null;
			if (uidClaim instanceof Number n) {
				uid = n.longValue();
			} else if (uidClaim instanceof String s) {
				try {
					uid = Long.valueOf(s);
				} catch (NumberFormatException ignored) {
					uid = null;
				}
			}
			if (uid == null) {
				log.warn("JWT sin claim 'uid' en @CurrentUserId");
			}
			return uid;
		}

		return null;
	}
}
