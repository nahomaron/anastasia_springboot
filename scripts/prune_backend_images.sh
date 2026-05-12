#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <image-repository>" >&2
  exit 1
fi

IMAGE_REPO="$1"

declare -A KEEP_REFS=()
RELEASE_FILES=(
  "/opt/anastasis-staging/releases/current.env"
  "/opt/anastasis-staging/releases/previous.env"
  "/opt/anastasis-production/releases/current.env"
  "/opt/anastasis-production/releases/previous.env"
)

for release_file in "${RELEASE_FILES[@]}"; do
  if [[ -f "${release_file}" ]]; then
    image_ref="$(grep '^BACKEND_IMAGE=' "${release_file}" | cut -d= -f2- || true)"
    if [[ -n "${image_ref}" ]]; then
      KEEP_REFS["${image_ref}"]=1
    fi
  fi
done

mapfile -t CANDIDATES < <(
  docker image ls --digests --format '{{.Repository}}@{{.Digest}}' "${IMAGE_REPO}" \
    | grep -v '<none>' \
    | sort -u
)

for image_ref in "${CANDIDATES[@]}"; do
  if [[ -n "${KEEP_REFS[${image_ref}]:-}" ]]; then
    echo "Keeping ${image_ref}"
    continue
  fi

  echo "Removing stale image ${image_ref}"
  docker image rm "${image_ref}" >/dev/null 2>&1 || true
done

# Remove stopped containers and unused networks/build cache after each deploy.
docker container prune -f >/dev/null 2>&1 || true
docker network prune -f >/dev/null 2>&1 || true
docker builder prune -af >/dev/null 2>&1 || true

# Clean up untagged layers that are no longer referenced by any image.
docker image prune -f >/dev/null 2>&1 || true
