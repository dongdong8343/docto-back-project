package com.ssginc8.docto.user.service.dto;

import java.util.Objects;

import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.entity.User;
import org.apache.commons.lang3.StringUtils;

public class UserInfo {
	public static class Response {
		public Long userId;
		public String email;
		public String name;
		public String phone;
		public String address;
		public Role role;
		public Boolean isSuspended;
		public String profileImageUrl;

		private Response(Long userId, String email, String name, String phone, String address, Role role, Boolean isSuspended, String profileImageUrl) {
			this.userId = userId;
			this.email = email;
			this.name = name;
			this.phone = phone;
			this.address = address;
			this.role = role;
			this.isSuspended = isSuspended;
			this.profileImageUrl = profileImageUrl;
		}

		public static Response from(User user, String defaultProfileUrl) {

			String profileUrl = StringUtils.defaultIfBlank(user.getProfileUrl(), defaultProfileUrl);

			return new Response(user.getUserId(), user.getEmail(), user.getName(), user.getPhone(), user.getAddress(), user.getRole(),
				user.getIsSuspended(), profileUrl);
		}
	}
}