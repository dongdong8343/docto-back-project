package com.ssginc8.docto.user.repo;

import static com.ssginc8.docto.file.entity.QFile.file;
import static com.ssginc8.docto.user.entity.QUser.user;

import com.querydsl.core.types.dsl.BooleanExpression;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssginc8.docto.file.entity.QFile;
import com.ssginc8.docto.user.entity.QUser;
import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.entity.User;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserSearchRepoImpl implements UserSearchRepo {

	private final JPAQueryFactory queryFactory;


	@Override
	public List<User> findByRoleAndDeletedAtIsNull(Role role, Pageable pageable) {

		return queryFactory.selectFrom(user)
			.leftJoin(user.profileImage, file).fetchJoin()
			.where(
				eqUserRole(role),
				user.deletedAt.isNull()
			)
			.limit(pageable.getPageSize())
			.offset(pageable.getPageSize() * pageable.getPageNumber())
			.fetch();
	}

	@Override
	public Long countByRoleAndDeletedAtIsNull(Role role) {
		return queryFactory.select(user.count())
			.from(user)
			.leftJoin(user.profileImage, file).fetchJoin()
			.where(
				eqUserRole(role),
				user.deletedAt.isNull()
			) .fetchOne();
	}

	// 동적쿼리 할떄 사용
	private BooleanExpression eqUserRole(Role role) {
		if(Objects.isNull(role)) {
			return null;
		}

		return user.role.eq(role);
	}
}
