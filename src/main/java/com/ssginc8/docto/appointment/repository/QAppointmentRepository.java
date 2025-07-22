package com.ssginc8.docto.appointment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ssginc8.docto.appointment.dto.AppointmentSearchCondition;
import com.ssginc8.docto.appointment.entity.Appointment;

public interface QAppointmentRepository {

	Page<Appointment> findAllByCondition(AppointmentSearchCondition condition, Pageable pageable);
}
