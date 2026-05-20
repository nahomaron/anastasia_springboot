#!/usr/bin/env sh
set -euo pipefail

BUCKET_NAME="${ANASTASIA_S3_BUCKET:-anastasia-app-bucket}"

awslocal s3api create-bucket --bucket "$BUCKET_NAME" >/dev/null 2>&1 || true

cat > /tmp/cors.json <<'CORS'
{
  "CORSRules": [
    {
      "AllowedOrigins": [
        "http://localhost:4200",
        "https://staging.anastasisapp.com"
      ],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag"],
      "MaxAgeSeconds": 3000
    }
  ]
}
CORS

awslocal s3api put-bucket-cors --bucket "$BUCKET_NAME" --cors-configuration file:///tmp/cors.json

echo "LocalStack S3 initialized with CORS for bucket: $BUCKET_NAME"
