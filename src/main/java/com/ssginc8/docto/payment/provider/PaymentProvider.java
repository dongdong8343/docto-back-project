package com.ssginc8.docto.payment.provider;

import org.springframework.stereotype.Service;

import com.ssginc8.docto.global.error.exception.paymentException.PaymentRequestNotFoundException;
import com.ssginc8.docto.payment.entity.PaymentRequest;
import com.ssginc8.docto.payment.repository.PaymentRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentProvider {
	private final PaymentRequestRepository paymentRequestRepository;

	public PaymentRequest save(PaymentRequest paymentRequest) {
		return paymentRequestRepository.save(paymentRequest);
	}

	public PaymentRequest findPaymentRequestByPaymentRequestId(Long paymentRequestId) {
		return paymentRequestRepository.findPaymentRequestByPaymentRequestId(paymentRequestId)
			.orElseThrow(PaymentRequestNotFoundException::new);
	}

	public PaymentRequest findPaymentRequestByOrderId(String orderId) {
		return paymentRequestRepository.findPaymentRequestByOrderId(orderId)
			.orElseThrow(PaymentRequestNotFoundException::new);
	}
}
