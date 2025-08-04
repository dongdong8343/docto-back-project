package com.ssginc8.docto.auth.provider;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.provider.UserProvider;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CurrentUserProvider {
	private final UserProvider userProvider;

	public User getUserFromUserId() {
		return userProvider.getUserById(
			Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName()));
	}
}
