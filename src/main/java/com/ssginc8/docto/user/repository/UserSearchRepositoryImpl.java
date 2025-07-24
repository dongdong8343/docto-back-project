package com.ssginc8.docto.user.repository;

import static com.ssginc8.docto.file.entity.QFile.*;
import static com.ssginc8.docto.user.entity.QUser.*;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.entity.User;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserSearchRepositoryImpl implements UserSearchRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<User> findByRoleAndDeletedAtIsNull(Role role, Pageable pageable) {
		return queryFactory.selectFrom(user)
			.leftJoin(user.profileImage, file).fetchJoin()
			.where(user.deletedAt.isNull(), userRoleEq(role))
			.limit(pageable.getPageSize())
			.offset(pageable.getOffset() * pageable.getPageSize())
			.fetch();
	}

	@Override
	public Long countByRoleAndDeleteAtIsNull(Role role) {
		return queryFactory.select(user.count())
			.from(user)
			.where(user.deletedAt.isNull(), userRoleEq(role))
			.fetchOne();
	}

	private BooleanExpression userRoleEq(Role role) {
		return Objects.nonNull(role) ? user.role.eq(role) : null;
	}
}
