package com.ssginc8.docto.user.repository;

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
public class UserSearchRepositoryImpl implements UserSearchRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<User> findByRoleAndDeletedAtIsNull(Role role, Pageable pageable) {
		QUser user = QUser.user;
		QFile file = QFile.file;

		JPQLQuery<User> query = queryFactory.selectFrom(user)
			.leftJoin(user.profileImage, file).fetchJoin()
			.where(user.deletedAt.isNull());

		if (Objects.nonNull(role)) {
			query.where(user.role.eq(role));
		}

		int size = pageable.getPageSize();
		int offset = pageable.getPageNumber() * size;

		query.limit(size);
		query.offset(offset);

		List<User> content = query.fetch();

		JPQLQuery<Long> countQuery = queryFactory.select(user.count())
			.from(user)
			.where(user.deletedAt.isNull());

		if (Objects.nonNull(role)) {
			countQuery.where(user.role.eq(role));
		}

		long total = Optional.ofNullable(countQuery.fetchOne()).orElse(0L);

		return new PageImpl<>(content, pageable, total);
	}
}
