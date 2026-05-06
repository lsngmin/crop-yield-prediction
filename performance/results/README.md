# Performance Test Results

## POST /predict (단건 추론)

### Environment
- Machine: MacBook Air (Apple Silicon)
- Java: 21 (Temurin)
- Spring Boot: 4.0.6
- Container: Docker (멀티스테이지 빌드, 단일 컨테이너)
- Model: RandomForest (n=50, depth=15, leaf=50), ONNX float32, 24.6 MB
- Cold start: 제외

### Scenario
- Executor: `constant-vus`
- VUs: 20
- Duration: 1m
- Sleep per iteration: 1s
- Total iterations: 1200

### Result

| 지표 | 값 |
|---|---|
| p50 | 3.55 ms |
| p95 | 7.30 ms |
| p99 | 25.95 ms |
| max | 26.72 ms |
| mean | 4.05 ms |
| error rate | 0.00% |
| throughput | 19.9 req/s |

### Thresholds (전부 통과)
- `http_req_failed`: rate < 1% → 0.00%
- `http_req_duration`: p50 < 100ms → 3.55ms
- `http_req_duration`: p95 < 300ms → 7.30ms
- `http_req_duration`: p99 < 500ms → 25.95ms

### Notes
- 추론 자체 latency (Micrometer 측정): ~3.8ms — k6의 p50과 일치 → HTTP/JSON 오버헤드 미미
- VU=20 + sleep=1s 조합으로 실제 throughput 한계 측정 X. 더 높은 부하 테스트는 v2.