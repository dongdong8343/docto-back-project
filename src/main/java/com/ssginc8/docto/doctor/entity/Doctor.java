package com.ssginc8.docto.doctor.entity;

import org.hibernate.annotations.DynamicUpdate;

import com.ssginc8.docto.global.base.BaseTimeEntity;
import com.ssginc8.docto.global.error.exception.doctorException.NegativeCapacityException;
import com.ssginc8.docto.hospital.entity.Hospital;
import com.ssginc8.docto.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_doctor")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@DynamicUpdate
public class Doctor extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long doctorId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "hospitalId", nullable = false)
	private Hospital hospital;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "userId", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Specialization specialization;

	@Column(nullable = false)
	private Long capacityPerHalfHour;

	private Long fileId;

	public Doctor(Hospital hospital, Specialization specialization, User user) {
		this.hospital = hospital;
		this.specialization = specialization;
		this.user = user;
		this.capacityPerHalfHour = 0L;
	}

	public static Doctor create(Hospital hospital, Specialization specialization, User user) {
		return new Doctor(hospital, specialization, user);
	}

	public void changeCapacityPerHalfHour(Long capacityPerHalfHour) {
		if (capacityPerHalfHour < 0) {
			throw new NegativeCapacityException();
		}

		this.capacityPerHalfHour = capacityPerHalfHour;
	}
}
