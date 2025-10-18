#!/usr/bin/env python3
import json, os, uuid, datetime

K6_SUMMARY = "performance/k6/results/summary.json"
ALLURE_RESULTS = "target/allure-results"

os.makedirs(ALLURE_RESULTS, exist_ok=True)

def allure_test(uuid_str, name, full_name, status, duration, metric_name, value):
    """Generate a minimal Allure result JSON for a performance metric"""
    return {
        "uuid": uuid_str,
        "name": name,
        "fullName": full_name,
        "status": status,
        "stage": "finished",
        "start": int(datetime.datetime.now().timestamp() * 1000),
        "stop": int(datetime.datetime.now().timestamp() * 1000) + duration,
        "labels": [
            {"name": "epic", "value": "Performance"},
            {"name": "feature", "value": metric_name.split('_')[0].capitalize()},
            {"name": "story", "value": metric_name},
        ],
        "parameters": [{"name": "metric", "value": metric_name}, {"name": "value", "value": str(value)}],
    }

def main():
    if not os.path.exists(K6_SUMMARY):
        print(f"⚠️ No summary file found at {K6_SUMMARY}")
        return

    with open(K6_SUMMARY, "r") as f:
        summary = json.load(f)

    metrics = summary.get("metrics", {})
    count = 0

    for metric_name, data in metrics.items():
        # Determine p95 or rate values where available
        val = None
        if "p(95)" in data:
            val = data["p(95)"]
        elif "rate" in data:
            val = data["rate"]
        elif "avg" in data:
            val = data["avg"]

        if val is None:
            continue

        # Determine status vs. thresholds
        if "auth" in metric_name:
            threshold = 800 if "duration" in metric_name else 0.01
        elif "members" in metric_name:
            threshold = 700 if "duration" in metric_name else 0.01
        elif "user" in metric_name:
            threshold = 600 if "duration" in metric_name else 0.01
        else:
            threshold = 800

        passed = val < threshold
        status = "passed" if passed else "failed"
        duration = 50

        test_uuid = str(uuid.uuid4())
        test_name = f"{metric_name}: {val:.2f} ({'OK' if passed else 'FAIL'})"
        full_name = f"Performance::{metric_name}"

        with open(os.path.join(ALLURE_RESULTS, f"{test_uuid}-result.json"), "w") as out:
            json.dump(allure_test(test_uuid, test_name, full_name, status, duration, metric_name, val), out, indent=2)
        count += 1

    print(f"✅ Generated {count} Allure performance results in {ALLURE_RESULTS}")

if __name__ == "__main__":
    main()
