package com.ssginc8.docto.user.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.entity.User;

public interface UserQueryRepository {
	List<User> findByRoleAndDeletedAtIsNull(Role role, Pageable pageable);

	Long countByRoleAndDeleteAtIsNull(Role role);
}
