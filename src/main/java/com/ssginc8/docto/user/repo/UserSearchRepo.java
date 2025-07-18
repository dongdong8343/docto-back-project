package com.ssginc8.docto.user.repo;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.entity.User;

public interface UserSearchRepo {
	List<User> findByRoleAndDeletedAtIsNull(Role role, Pageable pageable);

	Long countByRoleAndDeletedAtIsNull(Role role);
}
