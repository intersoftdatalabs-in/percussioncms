# Amazon S3 publish on EC2 (IMDSv2)

Percussion CMS detects whether it is running on EC2 so Server Properties for Amazon S3 can rely on the **instance profile** (with or without **Assume Role**) instead of static Access Key / Secret.

## Why Amazon Linux 2023+ matters

AWS instance metadata defaults increasingly use **IMDSv2 only** (`HttpTokens=required` on Amazon Linux 2023 and many newer AMIs). A plain IMDSv1 GET to `http://169.254.169.254/latest/meta-data/` fails on those hosts.

CMS probes IMDS with an IMDSv2 session token (`PUT /latest/api/token`, then GETs with `X-aws-ec2-metadata-token`) and falls back to IMDSv1 only when the token endpoint is unavailable (older AMIs).

## Operator checklist

|           Setting           |                                                                                          Guidance                                                                                           |
|-----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **HttpTokens**              | Prefer `required` (IMDSv2). CMS no longer needs IMDSv1 optional for EC2 detection.                                                                                                          |
| **HttpPutResponseHopLimit** | On the **EC2 instance**, set to **at least 2** when CMS runs **inside a container** (Docker/Kubernetes). Hop limit `1` blocks the token PUT from the container network namespace.           |
| **S3 form fields**          | Access Key, Secret, and Role ARN are **not** hard-required on save. Empty values are allowed for instance-profile / Assume Role setups; the UI shows a non-modal warning if they are empty. |
| **Bucket / region**         | Bucket name remains required. Region can be selected or derived from IMDS when blank on EC2.                                                                                                |

## Temporary workarounds (if needed)

1. Set metadata to `HttpTokens=optional` (allows IMDSv1) — not preferred long-term.
2. Raise `HttpPutResponseHopLimit` to `2` for containerized CMS.
3. Configure static Access Key + Secret (base identity for Assume Role if used).

## Code entry points

- Shared helper: `com.percussion.rx.delivery.impl.PSEc2MetadataClient`
- Delivery / region: `PSAmazonS3DeliveryHandler.isEC2Instance()` / `getCurrentEc2Region()`
- UI REST: `PSPubServerService.isEC2Instance()` → same probe

See issue [#2284](https://github.com/intersoftdatalabs-in/percussioncms/issues/2284).
