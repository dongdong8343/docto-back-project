package com.ssginc8.docto.user.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.DynamicUpdate;

import com.ssginc8.docto.file.entity.File;
import com.ssginc8.docto.global.base.BaseTimeEntity;
import com.ssginc8.docto.user.model.Password;

import io.micrometer.common.util.StringUtils;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@DynamicUpdate
@Table(name = "tbl_user")
@Entity
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;

	@Column(nullable = false, unique = true, length = 340)
	private String uuid;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(unique = true)
	private String providerId;

	@Embedded
	private Password password;

	@Column(length = 100)
	private String name;

	@Column(length = 100)
	private String phone;

	@Column(length = 200)
	private String address;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LoginType loginType;

	@Enumerated(EnumType.STRING)
	private Role role;

	private Long penalty;

	@Column(nullable = false)
	private Boolean isSuspended;

	private LocalDateTime suspendedAt;

	private LocalDateTime suspensionExpiresAt;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "fileId")
	private File profileImage;

	private User(String uuid, String email, Password password, String name, String phone,
		String address, LoginType loginType, Role role, File profileImage, Boolean isSuspended) {
		this.uuid = uuid;
		this.email = email;
		this.password = password;
		this.name = name;
		this.phone = phone;
		this.address = address;
		this.loginType = loginType;
		this.role = role;
		this.profileImage = profileImage;
		this.isSuspended = isSuspended;
	}

	private User(String uuid, String email, Password password, String name, String phone,
		LoginType loginType, Role role, Boolean isSuspended) {
		this.uuid = uuid;
		this.email = email;
		this.password = password;
		this.name = name;
		this.phone = phone;
		this.loginType = loginType;
		this.role = role;
		this.isSuspended = isSuspended;
	}

	private User(String uuid, String email, String providerId, String name, LoginType loginType, Boolean isSuspended) {
		this.uuid = uuid;
		this.email = email;
		this.providerId = providerId;
		this.name = name;
		this.loginType = loginType;
		this.isSuspended = isSuspended;
	}

	public String getPasswordValue() {
		return this.password.getPassword();  // 내부적으로 String 리턴
	}

	public static User createUserByEmail(String email, Password password, String name, String phone, String address,
		Role role, File profileImage) {
		String uuid = UUID.randomUUID().toString();

		return new User(uuid, email, password, name, phone, address, LoginType.EMAIL, role, profileImage, false);
	}

	public static User createDoctorByEmail(String email, Password password, String name, String phone,
		Role role) {
		String uuid = UUID.randomUUID().toString();

		return new User(uuid, email, password, name, phone, LoginType.EMAIL, role, false);
	}

	public static User createUserBySocial(String providerId, String name, String email, LoginType loginType) {
		String uuid = UUID.randomUUID().toString();

		return new User(uuid, email, providerId, name, loginType, false);
	}

	// 이미 가입 된 사용자가 소셜 로그인으로 로그인 한 경우 호출
	public User updateOauthInfo(String providerId, String name, String email, LoginType loginType) {
		this.providerId = providerId;
		this.name = name;
		this.email = email;
		this.loginType = loginType;

		return this;
	}

	public void updateSocialInfo(String phone, File profileImage, String address, Role role) {
		this.phone = phone;
		this.profileImage = profileImage;
		this.address = address;
		this.role = role;

	}

	public void updatePassword(Password password) {
		this.password = password;
	}

	public String getProfileUrl() {
		if (Objects.isNull(this.profileImage)) {
			return null;
		}
		return this.profileImage.getUrl();
	}

	public String getProfileName() {
		if (Objects.isNull(this.profileImage)) {
			return null;
		}
		return this.profileImage.getFileName();
	}

	public void updateUser(String name, String email, String phone, String address, File profileImage) {
		if (StringUtils.isNotBlank(name)) {
			this.name = name;
		}

		if (StringUtils.isNotBlank(email)) {
			this.email = email;
		}

		if (StringUtils.isNotBlank(phone)) {
			this.phone = phone;
		}

		if (StringUtils.isNotBlank(address)) {
			this.address = address;
		}

		if (Objects.nonNull(profileImage)) {
			if (Objects.nonNull(this.profileImage)) {
				this.profileImage.updateFile(profileImage.getFileName(), profileImage.getOriginalName(),
					profileImage.getUrl(),
					profileImage.getBucketName(), profileImage.getFileSize(), profileImage.getFileType());
			} else {
				this.profileImage = profileImage;
			}
		}
	}

	public static User createUser(String username, Password password, String email, String loginType, String role,
		Boolean suspended, String uuid) {
		User user = new User();
		user.name = username;
		user.password = password;
		user.email = email;
		user.loginType = LoginType.valueOf(loginType);
		user.role = Role.valueOf(role);
		user.isSuspended = suspended;
		user.uuid = uuid;
		// 필요한 초기화 작업 추가
		return user;
	}

	public void updatePhoneAndProfileImage(String phone, File profileImage) {
		if (StringUtils.isNotBlank(phone)) {
			this.phone = phone;
		}
		if (profileImage != null) {
			this.profileImage = profileImage;
		}
	}

	/**
	 * 패널티 부여
	 * @param value
	 */
	public void addPenalty(Long value) {
		if (this.penalty == null) {
			this.penalty = 0L;
		}
		this.penalty += value;
	}

	/**
	 * 예약 정지
	 */
	public void suspendFordDays(int days) {
		this.isSuspended = true;
		this.suspendedAt = LocalDateTime.now();
		this.suspensionExpiresAt = LocalDateTime.now().plusDays(days);

	}
}
