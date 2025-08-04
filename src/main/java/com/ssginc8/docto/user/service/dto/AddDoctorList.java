package com.ssginc8.docto.user.service.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AddDoctorList {
	@Getter
	public static class DoctorInfo {
		@Email(message = "이메일 형식에 맞지 않습니다.")
		@NotBlank(message = "이메일은 비어있을 수 없습니다.")
		private String email;

		@NotBlank(message = "패스워드는 비어있을 수 없습니다.")
		private String password;

		@NotBlank(message = "이름은 비어있을 수 없습니다.")
		private String name;

		@NotBlank(message = "휴대폰 번호는 비어있을 수 없습니다.")
		private String phone;

		private Long hospitalId;

		private String specialization;
	}

	@Getter
	@Builder
	public static class RegisteredDoctor {
		private String email;
		private Long userId;
		private String specialization;
		private Long hospitalId;
	}

	public static RegisteredDoctor toRegisteredDoctor(Long userId, DoctorInfo doctor) {
		return AddDoctorList.RegisteredDoctor.builder()
			.email(doctor.getEmail())
			.userId(userId)
			.specialization(doctor.getSpecialization())
			.hospitalId(doctor.getHospitalId())
			.build();
	}

	@Getter
	@NoArgsConstructor
	public static class Request {
		@Valid
		List<DoctorInfo> doctorInfos;
		Long hospitalId;
	}

	@Getter
	@NoArgsConstructor
	public static class Response {
		private List<RegisteredDoctor> registeredDoctors;

		@Builder
		public Response(List<RegisteredDoctor> registeredDoctors) {
			this.registeredDoctors = registeredDoctors;
		}
	}

	public static Response toResponse(List<RegisteredDoctor> registeredDoctors) {
		return AddDoctorList.Response.builder()
			.registeredDoctors(registeredDoctors)
			.build();
	}
}
