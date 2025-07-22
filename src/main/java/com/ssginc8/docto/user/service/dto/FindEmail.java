package com.ssginc8.docto.user.service.dto;

import com.ssginc8.docto.user.entity.User;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class FindEmail {
	@Getter
	@NoArgsConstructor
	public static class Request {
		private String name;
		private String phone;

		@Builder
		public Request(String name, String phone) {
			this.name = name;
			this.phone = phone;
		}
	}

	@Getter
	@NoArgsConstructor
	public static class Response {
		private String email;

		public Response(String email) {
			this.email = email;
		}
	}

	public static Response toResponse(User user) {
		return new Response(user.getEmail());
	}
}
