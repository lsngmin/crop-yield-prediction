# Performance Test Results

## POST /predict (단건 추론)

### Environment
- MacBook Air (Apple Silicon), Java 21, Spring Boot 4.0.6
- Container: Docker (단일 컨테이너, 멀티스테이지 빌드)
- Model: RandomForest (n=50, depth=15, leaf=50), ONNX float32, 24.6 MB
- Cold start: 제외

### Scenario
- Executor: `constant-vus`, VUs=20, Duration=1m, Sleep=1s/iter

### Result
| 지표 | 값 |
|---|---|
| p50 | 3.55 ms |
| p95 | 7.30 ms |
| p99 | 25.95 ms |
| error rate | 0.00% |
| request throughput | 19.9 req/s |
| prediction throughput | 19.9 predictions/s |

---

## POST /predict/batch (배치 추론)

### Scenario
- Executor: `constant-vus`, VUs=10, Duration=1m, Sleep=1s/iter
- Batch size: 50 items per request

### Result
| 지표 | 값 |
|---|---|
| p50 | 13.17 ms |
| p95 | 36.66 ms |
| p99 | 184.67 ms |
| error rate | 0.00% |
| request throughput | 9.81 req/s |
| **prediction throughput** | **490.65 predictions/s** |

---

## 단건 vs Batch 비교

| 지표 | 단건 | Batch (50) | 비율 |
|---|---|---|---|
| p50 latency | 3.55 ms | 13.17 ms | 3.7× ↑ |
| p99 latency | 25.95 ms | 184.67 ms | 7.1× ↑ |
| 1 prediction당 시간 | 3.55 ms | 0.26 ms | **13.5× 빠름** |
| predictions/s | 19.9 | 490.65 | **24.7× 높음** |

### 인사이트
- Batch는 **predictions/s 기준 약 25배 throughput** 달성. ONNX Runtime이 `[N, 24]` 텐서를 단일 호출로 처리하는 효율성 + HTTP/JNI 경계 통과 비용 1회.
- 단건은 **latency-sensitive 워크로드**에, Batch는 **throughput-oriented 워크로드**에 적합.
- **트레이드오프**: Batch는 응답까지 더 오래 걸림 (50건 처리). 실시간 단건 응답 필요한 UX엔 부적합.

### Caveats
- 로컬 단일 머신 측정 (클라이언트/서버 동일). 절대값보다 **상대 비교**에 의미.
- VU+sleep 제약으로 **server capacity 한계는 미측정** — request/s는 클라이언트측 부하 한계.
- 프로덕션 측정 (별도 호스트 + 클라우드 환경)은 v2 계획.