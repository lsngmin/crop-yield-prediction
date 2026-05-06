# 농작물 수확량 예측 API

[TODO: 한 줄 설명 — 예: "환경/관리 조건 9개 피처를 받아 RandomForest 회귀 모델로 작물 수확량을 예측하는 REST API. ML 모델은 ONNX로 export하고 Spring Boot가 ONNX Runtime Java로 직접 추론."]

## 기술 스택 
<img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Jupyter-F37626?style=for-the-badge&logo=Jupyter&logoColor=white">
  
  <img src="https://img.shields.io/badge/scikit learn-F7931E?style=for-the-badge&logo=scikitlearn&logoColor=white"> <img src="https://img.shields.io/badge/onnx-005CED?style=for-the-badge&logo=onnx&logoColor=white"> <img src="https://img.shields.io/badge/gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white"> <img src="https://img.shields.io/badge/docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=Swagger&logoColor=white"> <img src="https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=JUnit5&logoColor=white">




<!-- <img src="https://img.shields.io/badge/표시할이름-색상?style=for-the-badge&logo=기술스택아이콘&logoColor=white"> -->

## 아키텍처
<img width="1024" height="559" alt="image" src="https://github.com/user-attachments/assets/6a43449f-b318-46fa-9949-2d2509a85d62" />

```
[클라이언트]
    │
    ▼  HTTP POST /predict (JSON)
[Spring Boot Application]
    │
    ├── PredictController     ← @Valid 입력 검증
    │       │
    │       ▼
    ├── PredictService        ← One-hot 인코딩 + ONNX 추론
    │       │
    │       ▼
    └── ModelLoader (startup) ← ONNX 모델 + 메타데이터 1회 로드
            │
            ▼
       [models/crop_yield_rf.onnx + feature_order.json]
```

## 데이터

- 출처: [Agriculture Crop Yield (Kaggle)](https://www.kaggle.com/datasets/samuelotiattakorah/agriculture-crop-yield)
- 100만 행 합성 데이터, 10개 컬럼 (피처 9 + 타겟 1)
- 타겟: `Yield_tons_per_hectare`
- 레포 미포함 (`data/`는 gitignore). Kaggle에서 다운로드 → `data/crop_yield.csv`

## 실행 방법

### Docker
```bash
docker build -t crop-yield-api .
docker run --rm -p 8080:8080 crop-yield-api
```

### 로컬 (Gradle)
```bash
./gradlew bootRun
```

## API 사용 예시

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

### Swagger UI
실행 후 http://localhost:8080/swagger-ui.html 접속.

## 모델 학습 (`notebooks/baseline.ipynb`)

### EDA 주요 발견

- **타겟 분포**: 평균 ~5 ton/ha, 정규분포에 가까운 종 모양
- **수치형 상관**: Rainfall ↔ Yield = 0.76 (다른 수치 피처는 ~0)
- **범주형 박스플롯**: Crop/Region/Soil_Type/Weather_Condition 모두 그룹 차이 없음
- **불리언**: Fertilizer_Used (+1.5톤), Irrigation_Used (+1.2톤) 강한 신호

### 최종 모델 결과

| 지표 | 값 |
|---|---|
| MAE | 0.4015 ton/ha |
| RMSE | 0.5032 ton/ha |
| ONNX 파일 크기 | 24.6 MB (float32) |
| ONNX vs sklearn 검증 오차 | ~1e-6 (사실상 0) |

### Feature Importance

| 피처 | 중요도 |
|---|---|
| Rainfall_mm | 60.6% |
| Fertilizer_Used | 19.6% |
| Irrigation_Used | 12.5% |
| Temperature_Celsius | 2.8% |
| Days_to_Harvest | 1.6% |
| 범주형 (Crop/Region/Soil_Type/Weather) | 각 ~0.16% |

상위 3개 피처가 모델 결정의 92.7% 차지.

## 의사결정 기록 ⭐

### 1. 왜 RandomForest?

[TODO: 본인 말로 정리. 핵심 포인트:]
- 비선형/임계 패턴 자연스럽게 처리 (트리 분기 = 임계값)
- 가정 검증 불필요 (비모수 모델)
- 이상치/스케일에 강건
- 베이스라인 빠르게 만들기 적합

### 2. 왜 ONNX (pickle 아님)?

[TODO: 본인 말로 정리. 핵심:]
- 단일 컨테이너 제약 → pickle은 Python 사이드카 필요 (불가능)
- ONNX Runtime Java로 JVM에서 직접 추론
- float32 한계 (트리 임계값 정밀도)는 검증 결과 무시 가능 수준 (max diff 1e-6)
- float64 시도했으나 ONNX TreeEnsembleRegressor가 float32 출력 강제하는 스펙 한계로 type mismatch — 문서화하고 float32 채택

### 3. 모델 배포 의사결정 (3차 비교)

| 모델 | MAE | 파일 크기 | 비고 |
|---|---|---|---|
| default (max_depth=None) | 0.4115 | 변환 미완 (수 GB 추정) | 배포 불가능 |
| 1차 제약 (depth=20, leaf=20) | 0.4031 | 142 MB | GitHub 100MB 초과 |
| **2차 제약 (n=50, depth=15, leaf=50)** | **0.4015** | **24.6 MB** | ✅ **채택** |

**핵심 인사이트**: 정규화 강해질수록 MAE도 개선됨. 합성 데이터의 강한 신호 특성상 깊은 트리는 과적합. **인프라 제약(GitHub 100MB)이 더 좋은 모델을 강제한 사례.**

### 4. 평가 지표 — MAE + RMSE

[TODO: 본인 말로 정리. 핵심:]
- MAE: 타겟과 같은 단위(ton/ha) → 직관적 해석
- RMSE: 큰 오차에 페널티 → 이상치 영향 감지
- 같이 보면 오차 분포 균등성 판단 가능
- R² 대신 MAE/RMSE: 비전공자에게도 설명 가능한 단위 있는 지표

### 5. 모델 + 메타데이터 분리 설계

[TODO: 본인 말로 정리. 핵심:]
- ONNX 모델 옆에 `feature_order.json` 같이 둠
- Spring Boot가 두 파일 모두 startup에 로드
- 모델 변경 시 코드 재컴파일 없이 메타데이터만 갱신 가능
- 모델 라이프사이클 ↔ 코드 라이프사이클 분리

### 6. 왜 DB 안 썼나?

[TODO: 본인 말로. 키워드: stateless 추론 서비스, v1 스코프, 추가 가치 대비 복잡도]

### 7. 왜 단일 컨테이너?

[TODO: 본인 말로. 키워드: 배포 단순성, ONNX Runtime Java 덕에 사이드카 불필요]

## 향후 계획 (v2)

- [TODO: 본인이 진짜 v2에 넣고 싶은 거 — DB 연동, 프론트엔드, CI/CD, 모델 비교 등]

## 프로젝트 구조

```
crop-yield-prediction/
├── data/                              # 학습 데이터 (gitignore)
├── notebooks/
│   └── baseline.ipynb                 # EDA + 학습 + 평가 + ONNX export
├── models/
│   ├── crop_yield_rf.onnx             # 학습된 모델 (24.6MB)
│   └── feature_order.json             # 피처 순서 + 카테고리 메타
├── src/main/java/com/sngmin/cropyieldapi/
│   ├── CropYieldApiApplication.java
│   ├── config/
│   │   ├── ModelLoader.java           # ONNX + 메타 startup 로딩
│   │   └── ModelProperties.java       # @ConfigurationProperties
│   ├── controller/
│   │   └── PredictController.java     # POST /predict
│   ├── service/
│   │   └── PredictService.java        # 인코딩 + 추론
│   ├── model/                         # DTO
│   │   ├── PredictRequest.java
│   │   ├── PredictResponse.java
│   │   ├── FeatureMetadata.java
│   │   └── ErrorResponse.java
│   └── exception/
│       └── GlobalExceptionHandler.java # @ControllerAdvice
├── src/main/resources/
│   └── application.yaml
├── src/test/java/...                  # 단위 테스트 3개
├── Dockerfile
├── build.gradle.kts
└── requirements.txt                   # Python 학습 환경
```
