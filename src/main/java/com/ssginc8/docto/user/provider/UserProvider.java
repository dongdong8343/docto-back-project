package com.ssginc8.docto.user.provider;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.global.error.exception.userException.EmailNotFoundException;
import com.ssginc8.docto.global.error.exception.userException.UserNotFoundException;
import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.repository.UserRepository;
import com.ssginc8.docto.user.repository.UserSearchRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserProvider {
	private final UserRepository userRepository;
	private final UserSearchRepository userSearchRepository;

	@Transactional(readOnly = true)
	public User getUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);
	}

	public User loadUserByEmailOrException(String email) {
		return userRepository.findByEmail(email)
			.orElseThrow(UserNotFoundException::new);
	}

	public Optional<User> loadUserByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	public User loadUserByUuid(String uuid) {
		return userRepository.findByUuid(uuid)
			.orElseThrow(UserNotFoundException::new);
	}

	public User loadUserByProviderId(String providerId) {
		return userRepository.findByProviderId(providerId)
			.orElseThrow(UserNotFoundException::new);
	}

	public User loadEmailByNameAndPhone(String name, String phone) {
		return userRepository.findByNameAndPhone(name, phone)
			.orElseThrow(EmailNotFoundException::new);
	}

	public Page<User> loadUsersByRole(Role role, Pageable pageable) {
		return userSearchRepository.findByRoleAndDeletedAtIsNull(role, pageable);
	}

	public User createUser(User user) {
		return userRepository.save(user);
	}

	public User save(User user) {
		return userRepository.save(user);
	}
}
