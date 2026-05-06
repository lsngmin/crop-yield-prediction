# 농작물 수확량 예측 API

> ML 모델을 Spring Boot 단일 컨테이너에서 직접 추론하는 농업 도메인 API

환경/관리 조건 9개 피처를 받아 RandomForest 회귀 모델로 작물 수확량을 예측한다. 
ML 모델은 ONNX로 export하고 Spring Boot가 ONNX Runtime Java로 직접 추론

<img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Jupyter-F37626?style=for-the-badge&logo=Jupyter&logoColor=white">
  
  <img src="https://img.shields.io/badge/scikit learn-F7931E?style=for-the-badge&logo=scikitlearn&logoColor=white"> <img src="https://img.shields.io/badge/onnx-005CED?style=for-the-badge&logo=onnx&logoColor=white"> <img src="https://img.shields.io/badge/gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white"> <img src="https://img.shields.io/badge/docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=Swagger&logoColor=white"> <img src="https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=JUnit5&logoColor=white">

<img width="1024" height="559" alt="image" src="https://github.com/user-attachments/assets/6a43449f-b318-46fa-9949-2d2509a85d62" />

<br>

- **단일 컨테이너 ML 추론**: Python 사이드카 없이 JVM에서 직접 ONNX 추론
- **모델 크기 최적화**: 142MB → 24.6MB, MAE 0.4031 → 0.4015 개선
- **트러블슈팅**: ONNX TreeEnsembleRegressor float32 제약 대응
  
## Quick Start
### 실행

```bash
# Docker
docker build -t crop-yield-api .
docker run --rm -p 8080:8080 crop-yield-api

# 또는 로컬 (개발 시)
./gradlew bootRun
```
> 서버 시작 후 Swagger: http://localhost:8080/swagger-ui.html

### 요청
```bash
curl -X POST http://localhost:8080/predict \
  -H "Content-Type: application/json" \
  -d '{
    "rainfallMm": 500.0,
    "temperatureCelsius": 25.0,
    "fertilizerUsed": true,
    "irrigationUsed": true,
    "daysToHarvest": 120,
    "crop": "Wheat",
    "region": "South",
    "soilType": "Loam",
    "weatherCondition": "Rainy"
  }'
```

### 응답
```json
{
  "predictedYield": 5.732,
  "modelVersion": "v1.0"
}
```

## RandomForest Model

### 데이터

[Agriculture Crop Yield (Kaggle)](https://www.kaggle.com/datasets/samuelotiattakorah/agriculture-crop-yield) — 합성 100만 rows, 9 피처 + 타겟

```bash
kaggle datasets download -d samuelotiattakorah/agriculture-crop-yield -p data/ --unzip
```

### 학습 결과

[`notebooks/baseline.ipynb`](notebooks/baseline.ipynb) — EDA, 학습, 평가, ONNX export 전 과정.

| 지표 | 값 |
|---|---|
| MAE | 0.4015 ton/ha |
| RMSE | 0.5032 ton/ha |

상위 3개 피처(Rainfall, Fertilizer_Used, Irrigation_Used)가 모델 결정의 **92.7% 차지**.

<img width="692" height="468" alt="Feature Importance" src="https://github.com/user-attachments/assets/5988108a-5a89-4790-8fc0-56f240e46a41" />

### 데이터의 한계

합성 데이터셋이라서 실제 농업 데이터의 노이즈/결측치/계절성을 반영하지 않는다. 
베이스라인 모델 검증 목적이며, 실 운영엔 도메인 데이터 추가학습이 필요하다.

## 백엔드 구현 세부사항

### Spring Boot 패턴

- **`@ConfigurationProperties` (record)**: yaml의 `app.model.*` 값을 record로 
  타입 안전 매핑 — [`ModelProperties.java`](...)
  
- **모델 startup 로딩**: `OrtEnvironment`/`OrtSession`을 빈으로 등록해 startup 시 
  1회 초기화. 첫 요청 latency 제거 + 싱글톤 보장 — [`ModelLoader.java`](...)
  
- **`@RestControllerAdvice` 3단계 매핑**: Validation 예외(400) / 비즈니스 예외(400) / 
  일반 예외(500, 메시지 마스킹) — [`GlobalExceptionHandler.java`](...)
  
- **JNI 자원 관리**: `OnnxTensor`/`OrtSession.Result`는 GC 대상이 아닌 네이티브 자원 
  → 중첩 try-with-resources로 누수 방지

### 입력 → 추론 흐름

1. 카테고리 값 검증 (메타데이터 화이트리스트)
2. 9 필드 → 24 float 배열 변환 (불리언 → int, 카테고리 → one-hot)
3. `featureOrder` 순서대로 정렬 (학습 컬럼 순서 일치)
4. ONNX 추론 → 단일 float 결과 추출

[`PredictService.java`](src/main/java/com/sngmin/cropyieldapi/service/PredictService.java)

## 테스트 전략

JUnit 5 + Mockito 단위 테스트 3개:

| # | 대상 | 방법 |
|---|---|---|
| 1 | Service 정상 케이스 | ONNX 의존성 mock. `OnnxTensor.createTensor` 정적 메서드는 `MockedStatic`으로 모킹 |
| 2 | Service 예외 케이스 | 잘못된 카테고리 → `IllegalArgumentException` (인코딩 도달 전 차단 확인) |
| 3 | Controller validation | `@WebMvcTest` + `MockMvc`로 웹 레이어 격리. 필드 누락 → 400 응답 검증 |

→  ONNX 파일 없이 동작하는 순수 단위 테스트다. 모델 변경/재학습 시에도 테스트 영향 X

### 의도적으로 테스트하지 않은 것
- **통합 테스트**: v1 스펙 외 (스코프 제어). v2로 미룸
- **모델 정확도**: ML 영역, 노트북에서 별도 검증 (MAE 0.40)
- **부하 테스트**: 베이스라인 단계엔 미적용

## 왜 이렇게 만들었나

### 1. 왜 RandomForest?

농업 데이터 특성상 강수량/온도 같은 환경 변수와 수확량 사이엔 비선형/임계 패턴이 흔하다 (예: 강수량 X mm 이상에서 효과). RandomForest는 트리 분기점이 곧 임계값이라 이런 패턴을 자연스럽게 학습한다. 또한 합성 데이터의 노이즈에 강건하고, 9개 피처 스케일이 제각각(rainfall mm, temperature ℃, days)이라 스케일링 없이 바로 학습 가능한 점도 베이스라인으로 적합했다.

### 2. 왜 ONNX (pickle 아님)?

스펙상 **단일 컨테이너 제약** 때문. pickle을 쓰면 Python 사이드카가 별도로 필요해 docker-compose 멀티 컨테이너로 가게 된다. ONNX 포맷은 ONNX Runtime Java를 통해 JVM 내에서 직접 추론할 수 있어 단일 컨테이너 구성 유지가 가능하다.

float64 입력으로 더 높은 정밀도를 시도했으나, ONNX의 `TreeEnsembleRegressor` 연산자가 float32 출력을 강제하는 스펙 한계로 type mismatch가 발생했다. 트리 앙상블 모델 공통의 ONNX ML opset 한계라 판단하고 float32를 채택했다. sklearn 대비 검증 결과 최대 오차가 약 1e-6로 운영상 무시 가능한 수준으로 확인했다.

### 3. ONNX 변환 시 모델 크기 제약 해결

베이스라인 학습 후 ONNX 변환 시 모델 크기 문제로 두 번 재학습하며 배포 가능한 모델로 변환하였다.

| 설정 | MAE | 파일 크기 | 비고 |
|---|---|---|---|
| default (max_depth=None) | 0.4115 | GB 추정 | 변환 미완료 |
| 1차 제약 (depth=20, leaf=20) | 0.4031 | 142 MB | GitHub 100MB 초과 |
| **최종 (n=50, depth=15, leaf=50)** | **0.4015** | **24.6 MB** | ✅ |

정규화를 강하게 적용할수록 모델 크기뿐 아니라 MAE도 함께 개선되는 결과가 나왔다. 
합성 데이터의 강한 신호 특성상 기본값(max_depth=None)으로 학습한 트리는 
노이즈까지 외워 과적합한 것으로 추정한다.

## 향후 계획 (v2)

v1은 단일 컨테이너 ML 서빙 검증이 목표. 운영 기능과 ML 시스템 심화는 v2로 분리.

### 모델 서빙 심화
- **부하 테스트 + 성능 측정**: k6 기반 p50/p95/p99 latency 측정, 모델 로드 방식 비교
- **모델 버전 관리**: 응답에 모델 버전 메타데이터, 다른 버전 hot-reload
- **배치 추론 API**: `POST /predict/batch`로 다건 예측, 단건 vs 배치 성능 비교
- **추론 메트릭**: Micrometer로 추론 시간/호출 횟수 수집, `/actuator/metrics` 노출

### ML 시스템
- **전처리 파이프라인 통합**: sklearn Pipeline으로 학습/추론 전처리 일관성 보장
- **모델 비교**: XGBoost, LightGBM 벤치마크 (정확도 + 추론 속도 + 모델 크기)
- **모델 재학습 파이프라인**: 신규 데이터 누적 시 자동 재학습

### 운영
- **DB 연동**: 추론 요청/결과 영속화로 모델 모니터링 기반 마련
- **CI/CD**: GitHub Actions 기반 자동 빌드/테스트
- **통합 테스트**: Testcontainers로 실제 ONNX 환경 검증

