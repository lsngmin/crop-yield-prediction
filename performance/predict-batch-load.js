import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
    scenarios: {
        batch_predict_load: {
            executor: "constant-vus",
            vus: 10,
            duration: "1m",
        },
    },
    thresholds: {
        http_req_failed: ["rate<0.01"],
        http_req_duration: ["p(50)<200", "p(95)<600", "p(99)<1000"],
    },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

const item = {
    rainfallMm: 500.0,
    temperatureCelsius: 25.0,
    fertilizerUsed: true,
    irrigationUsed: true,
    daysToHarvest: 120,
    crop: "Wheat",
    region: "South",
    soilType: "Loam",
    weatherCondition: "Rainy",
};

const BATCH_SIZE = 50;

const payload = JSON.stringify({
    items: Array.from({ length: BATCH_SIZE }, () => item),
});

const params = {
    headers: { "Content-Type": "application/json" },
};

export default function () {
    const res = http.post(`${BASE_URL}/predict/batch`, payload, params);

    check(res, {
        "status is 200": (r) => r.status === 200,
        "count is 50": (r) => {
            try {
                return JSON.parse(r.body).count === BATCH_SIZE;
            } catch {
                return false;
            }
        },
    });

    sleep(1);
}