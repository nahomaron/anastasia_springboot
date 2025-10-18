#!/usr/bin/env python3
import datetime
import json
import uuid
from pathlib import Path
from typing import Optional

SUMMARY_DIR = Path("performance/k6/results")
ALLURE_RESULTS_DIR = Path("target/allure-results")

METRIC_THRESHOLDS = {
    "auth": {
        "auth_login_duration": 800,
        "http_req_duration": 800,
        "http_req_failed": 1.0,  # percent
    },
    "member": {
        "members_list_duration": 700,
        "members_error_rate": 1.0,
        "http_req_duration": 750,
        "http_req_failed": 1.0,
    },
    "user": {
        "user_fetch_duration": 600,
        "http_req_duration": 650,
        "http_req_failed": 1.0,
    },
    "default": {
        "http_req_duration": 800,
        "http_req_failed": 1.0,
    },
}


def allure_test(uuid_str: str, name: str, full_name: str, status: str, duration_ms: int, labels: dict, metric_name: str, value):
    """Generate a minimal Allure result JSON for a performance metric."""
    now_ms = int(datetime.datetime.now().timestamp() * 1000)
    allure_labels = [
        {"name": "epic", "value": "Performance"},
        {"name": "feature", "value": labels.get("feature", metric_name.split("_")[0].capitalize())},
        {"name": "story", "value": labels.get("story", metric_name)},
        {"name": "suite", "value": labels.get("suite", "k6-performance")},
    ]
    return {
        "uuid": uuid_str,
        "name": name,
        "fullName": full_name,
        "status": status,
        "stage": "finished",
        "start": now_ms,
        "stop": now_ms + duration_ms,
        "labels": allure_labels,
        "parameters": [
            {"name": "metric", "value": metric_name},
            {"name": "value", "value": f"{value:.3f}"},
        ],
    }


def load_summary(summary_path: Path) -> Optional[dict]:
    if not summary_path.exists():
        return None
    with summary_path.open("r") as handle:
        return json.load(handle)


def extract_metric_value(metric_name: str, metric_data: dict):
    metric_type = metric_data.get("type")
    values = metric_data.get("values", {})

    if metric_type == "trend":
        return values.get("p(95)")
    if metric_type == "rate":
        rate = values.get("rate")
        return rate * 100 if rate is not None else None
    return None


def determine_threshold(scenario: str, metric_name: str):
    scenario_thresholds = {**METRIC_THRESHOLDS.get("default", {}), **METRIC_THRESHOLDS.get(scenario, {})}
    return scenario_thresholds.get(metric_name)


def should_track_metric(metric_name: str) -> bool:
    return metric_name in {
        "http_req_duration",
        "http_req_failed",
        "auth_login_duration",
        "members_list_duration",
        "members_error_rate",
        "user_fetch_duration",
    }


def generate_allure_results():
    if not SUMMARY_DIR.exists():
        print(f"⚠️ No k6 summary directory found at {SUMMARY_DIR}")
        return

    ALLURE_RESULTS_DIR.mkdir(parents=True, exist_ok=True)

    total_tests = 0
    summary_files = sorted(SUMMARY_DIR.rglob("*-summary.json"))

    if not summary_files:
        print(f"⚠️ No summary JSON files found in {SUMMARY_DIR}")
        return

    for summary_file in summary_files:
        scenario = summary_file.stem.replace("-summary", "")
        summary = load_summary(summary_file)
        if not summary:
            print(f"⚠️ Unable to parse {summary_file}")
            continue

        metrics = summary.get("metrics", {})
        for metric_name, metric_data in metrics.items():
            if not should_track_metric(metric_name):
                continue

            value = extract_metric_value(metric_name, metric_data)
            if value is None:
                continue

            threshold = determine_threshold(scenario, metric_name)
            if threshold is None:
                continue

            passed = value <= threshold
            status = "passed" if passed else "failed"
            suffix = "ms" if metric_data.get("type") == "trend" else "%"
            test_uuid = str(uuid.uuid4())
            name = f"[{scenario}] {metric_name} = {value:.2f}{suffix}"
            full_name = f"Performance::{scenario}::{metric_name}"
            labels = {"feature": scenario.capitalize(), "story": metric_name, "suite": "k6-performance"}
            result_payload = allure_test(test_uuid, name, full_name, status, 50, labels, metric_name, value)

            output_path = ALLURE_RESULTS_DIR / f"{test_uuid}-result.json"
            with output_path.open("w") as out_handle:
                json.dump(result_payload, out_handle, indent=2)
            total_tests += 1

    print(f"✅ Generated {total_tests} Allure performance results in {ALLURE_RESULTS_DIR}")


def main():
    generate_allure_results()


if __name__ == "__main__":
    main()
