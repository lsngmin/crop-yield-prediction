import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
    scenarios: {
        predict_load: {
            executor: "constant-vus",
            vus: 20,
            duration: "1m",
        },
    },
    thresholds: {
        http_req_failed: ["rate<0.01"],
        http_req_duration: ["p(50)<100", "p(95)<300", "p(99)<500"],
    },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

const payload = JSON.stringify({
    rainfallMm: 500.0,
    temperatureCelsius: 25.0,
    fertilizerUsed: true,
    irrigationUsed: true,
    daysToHarvest: 120,
    crop: "Wheat",
    region: "South",
    soilType: "Loam",
    weatherCondition: "Rainy",
});

const params = {
    headers: { "Content-Type": "application/json" },
};

export default function () {
    const res = http.post(`${BASE_URL}/predict`, payload, params);

    check(res, {
        "status is 200": (r) => r.status === 200,
        "has predictedYield": (r) => {
            try {
                return JSON.parse(r.body).predictedYield !== undefined;
            } catch {
                return false;
            }
        },
        "has modelVersion": (r) => {
            try {
                return JSON.parse(r.body).modelVersion !== undefined;
            } catch {
                return false;
            }
        },
    });

    sleep(1);
}