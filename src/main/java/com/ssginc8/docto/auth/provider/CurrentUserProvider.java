package com.ssginc8.docto.auth.provider;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.provider.UserProvider;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CurrentUserProvider {
	private UserProvider userProvider;

	public User getUserFromUuid() {
		String uuid = SecurityContextHolder.getContext().getAuthentication().getName();

		return userProvider.loadUserByUuid(uuid);
	}
}
