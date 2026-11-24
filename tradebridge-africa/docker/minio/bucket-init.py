import os
from urllib.parse import urlparse

from minio import Minio
from minio.commonconfig import ENABLED, Filter
from minio.lifecycleconfig import Expiration, LifecycleConfig, Rule
from minio.sse import SseS3


endpoint = os.environ.get("MINIO_ENDPOINT", "http://minio:9000")
parsed_endpoint = urlparse(endpoint)
if parsed_endpoint.scheme not in ("http", "https") or not parsed_endpoint.netloc:
    raise ValueError("MINIO_ENDPOINT must be an absolute HTTP or HTTPS URL")

client = Minio(
    parsed_endpoint.netloc,
    access_key=os.environ.get("MINIO_ROOT_USER", "tradebridge"),
    secret_key=os.environ.get("MINIO_ROOT_PASSWORD", "tradebridge-minio-secret"),
    secure=parsed_endpoint.scheme == "https",
)
bucket = os.environ.get("MINIO_BUCKET_KYC", "tb-kyc-documents")

if not client.bucket_exists(bucket):
    client.make_bucket(bucket)
    print(f"Created bucket {bucket}")
else:
    print(f"Bucket {bucket} already exists")

client.set_bucket_encryption(bucket, SseS3())
client.set_bucket_lifecycle(
    bucket,
    LifecycleConfig(
        [
            Rule(
                ENABLED,
                rule_filter=Filter(prefix=""),
                expiration=Expiration(days=2555),
            )
        ]
    ),
)
print(f"Configured SSE-S3 and seven-year lifecycle for {bucket}")
