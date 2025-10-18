import json
from datetime import datetime
from pathlib import Path

K6_RESULTS_DIR = Path("performance/k6/results")
ALLURE_ENV_FILE = Path("target/allure-results/environment.properties")


def ensure_allure_env_file():
    ALLURE_ENV_FILE.parent.mkdir(parents=True, exist_ok=True)
    if not ALLURE_ENV_FILE.exists():
        ALLURE_ENV_FILE.touch()


def summarize_k6_json(file_path: Path) -> dict:
    with file_path.open("r") as handle:
        data = json.load(handle)

    metrics = data.get("metrics", {})
    summary = {}

    for name, metric in metrics.items():
        values = metric.get("values") or {}
        metric_type = metric.get("type")

        if metric_type == "trend":
            if "p(95)" in values:
                summary[f"{name}_p95_ms"] = values["p(95)"]
            if "avg" in values:
                summary[f"{name}_avg_ms"] = values["avg"]
        elif metric_type == "rate" and "rate" in values:
            summary[f"{name}_rate_pct"] = values["rate"] * 100.0

    return summary


def append_to_allure_env(summary: dict, scenario: str):
    ensure_allure_env_file()
    timestamp = datetime.now()
    lines = [f"# Appended by merge_k6_to_allure.py at {timestamp}"]
    for key, value in summary.items():
        lines.append(f"{scenario}_{key}={value}")
    lines.append("")
    with ALLURE_ENV_FILE.open("a") as handle:
        handle.write("\n".join(lines))


def main():
    if not K6_RESULTS_DIR.exists():
        print(f"No k6 results folder found at {K6_RESULTS_DIR}")
        return

    summaries_found = False
    for summary_file in sorted(K6_RESULTS_DIR.rglob("*-summary.json")):
        summaries_found = True
        scenario = summary_file.stem.replace("-summary", "")
        print(f"Processing {summary_file.name} for scenario '{scenario}' ...")
        summary = summarize_k6_json(summary_file)
        if summary:
            append_to_allure_env(summary, scenario)
        else:
            print(f"⚠️ No summarizable metrics found in {summary_file.name}")

    if summaries_found:
        print(f"K6 summaries merged into {ALLURE_ENV_FILE}")
    else:
        print(f"No summary JSON files detected in {K6_RESULTS_DIR}")


if __name__ == "__main__":
    main()
