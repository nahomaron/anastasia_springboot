import json
import os
from datetime import datetime
from pathlib import Path

K6_RESULTS_DIR = Path("performance/results")
ALLURE_ENV_FILE = Path("target/allure-results/environment.properties")

def summarize_k6_json(file_path):
    with open(file_path, "r") as f:
        data = json.load(f)

    metrics = data.get("metrics", {})
    summary = {}

    for name, m in metrics.items():
        if name.startswith("http_req_duration"):
            summary["p95_duration_ms"] = m["percentiles"]["p(95)"]
            summary["avg_duration_ms"] = m["avg"]
        elif name == "http_req_failed":
            summary["failure_rate"] = m["rate"] * 100  # percent

    return summary

def append_to_allure_env(summary, test_name):
    lines = [f"# Appended by merge_k6_to_allure.py at {datetime.now()}"]
    for key, value in summary.items():
        lines.append(f"{test_name}_{key}={value}")
    lines.append("")
    with open(ALLURE_ENV_FILE, "a") as f:
        f.write("\n".join(lines))

def main():
    if not K6_RESULTS_DIR.exists():
        print("No k6 results folder found.")
        return

    for file in K6_RESULTS_DIR.glob("*.json"):
        print(f"Processing {file.name} ...")
        summary = summarize_k6_json(file)
        test_name = file.stem.replace("-summary", "")
        append_to_allure_env(summary, test_name)

    print(f"K6 summaries merged into {ALLURE_ENV_FILE}")

if __name__ == "__main__":
    main()
