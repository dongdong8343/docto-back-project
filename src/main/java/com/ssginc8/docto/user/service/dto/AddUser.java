package com.ssginc8.docto.user.service.dto;

import org.springframework.web.multipart.MultipartFile;

import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.entity.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public class AddUser {
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@ToString
	public static class Request {
		@Email(message = "이메일 형식에 맞지 않습니다.")
		@NotBlank(message = "이메일은 비어있을 수 없습니다.")
		private String email;

		@NotBlank(message = "패스워드는 비어있을 수 없습니다.")
		private String password;

		@NotBlank(message = "이름은 비어있을 수 없습니다.")
		private String name;

		private String phone;

		private String address;

		private MultipartFile profileImage;

		private Role role;
	}

	@Getter
	public static class Response {
		private Long userId;

		private Role role;

		@Builder
		public Response(Long userId, Role role) {
			this.userId = userId;
			this.role = role;
		}
	}

	public static Response toResponse(User user) {
		return AddUser.Response.builder()
			.userId(user.getUserId())
			.role(user.getRole())
			.build();
	}
}
