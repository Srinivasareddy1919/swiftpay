// k6 load test: 250 TPS sustained, 1,000,000 total transactions
// Run:  k6 run --out json=results.json load-test/swiftpay-load.js
//
// PCAP capture (run in parallel on the host):
//   Linux:   sudo tcpdump -i any -w swiftpay-load.pcap \
//              'port 8081 or port 8082 or port 9092 or port 5432 or port 6379'
//   Windows: use Wireshark and capture on the loopback adapter filtering the same ports.

import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  scenarios: {
    steady_250_tps: {
      executor: 'constant-arrival-rate',
      rate: 250,                // 250 iterations per second
      timeUnit: '1s',
      duration: '4000s',        // 250 * 4000 = 1,000,000 transactions
      preAllocatedVUs: 200,
      maxVUs: 1000,
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const BASE = __ENV.SWIFTPAY_URL || 'http://localhost:8081';

export default function () {
  const payload = JSON.stringify({
    transactionId: uuidv4(),
    senderId:   'user-' + Math.floor(Math.random() * 1000),
    receiverId: 'user-' + Math.floor(Math.random() * 1000),
    amount: (Math.random() * 100 + 1).toFixed(2),
    currency: 'USD',
  });
  const res = http.post(`${BASE}/v1/payments`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(res, { 'accepted': (r) => r.status === 202 || r.status === 201 });
}
