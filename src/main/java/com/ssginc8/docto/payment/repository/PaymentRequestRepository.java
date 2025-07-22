package com.ssginc8.docto.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ssginc8.docto.payment.entity.PaymentRequest;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {
	Optional<PaymentRequest> findPaymentRequestByPaymentRequestId(Long paymentRequestId);

	Optional<PaymentRequest> findPaymentRequestByOrderId(String orderId);
}
