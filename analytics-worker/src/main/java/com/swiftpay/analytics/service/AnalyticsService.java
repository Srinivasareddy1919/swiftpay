package com.swiftpay.analytics.service;

import com.swiftpay.analytics.event.PaymentCompletedEvent;

import java.util.Map;

/** Application service port for analytics aggregations (DIP). */
public interface AnalyticsService {
    void record(PaymentCompletedEvent event);
    Map<String, Long> summary();
}
