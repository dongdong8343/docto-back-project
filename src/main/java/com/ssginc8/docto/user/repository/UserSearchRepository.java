package com.ssginc8.docto.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.entity.User;

public interface UserSearchRepository {
	Page<User> findByRoleAndDeletedAtIsNull(Role role, Pageable pageable);
}
