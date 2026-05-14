# Technical Architecture

## Confirmed Decisions

- Backend: Java with Spring Boot 4.
- Backend build target: native build, likely using GraalVM native image support.
- Database: PostgreSQL.
- Cloud provider: AWS.
- Infrastructure management: Terraform.
- Initial deployment: no Kubernetes.
- Web frontend: React-based.
- Mobile frontend: React Native for iOS and Android.
- Preferred frontend reuse strategy: React Native with web support where practical.

## Backend

The backend should be a Spring Boot 4 service that exposes APIs for authentication-aware user progress, vocabulary metadata, spaced repetition state, and AI media generation orchestration.

The native build target is useful for reducing memory usage and improving startup time. The first version should still keep local JVM development simple, with native builds used for deployment or release validation.

Recommended backend responsibilities:

- User vocabulary progress and review scheduling.
- Vocabulary metadata management.
- AI content-generation workflow orchestration.
- Generated media asset metadata and cache lookup.
- Signed URL generation for protected media assets if needed.

## Database

PostgreSQL should store structured application data:

- Users and account references.
- Vocabulary entries.
- Example sentences and prompts.
- Generated media metadata.
- Spaced repetition state.
- Review history.

Generated binary assets should not be stored in PostgreSQL. Audio, video, and images should live in S3, with PostgreSQL storing references, statuses, checksums, model/provider metadata, and timestamps.

## AWS Infrastructure

The first AWS deployment should avoid Kubernetes. A practical starting point is:

- Amazon ECR for backend container images.
- Amazon ECS Fargate for running the Spring Boot backend container.
- Application Load Balancer for public HTTP(S) traffic.
- Amazon RDS for PostgreSQL.
- Amazon S3 for generated media assets.
- Amazon CloudFront for efficient media delivery when needed.
- AWS Secrets Manager or SSM Parameter Store for secrets and API keys.
- Amazon CloudWatch for logs and metrics.

This is simpler than Kubernetes while still being production-oriented and compatible with Terraform.

## Terraform

Terraform should be introduced early, even for a minimal environment, so infrastructure changes stay reproducible.

Recommended initial Terraform scope:

- VPC and networking.
- RDS PostgreSQL instance.
- S3 media bucket.
- ECR repository.
- ECS Fargate service.
- Load balancer and security groups.
- IAM roles for the backend service.
- Secrets or parameters for database and external AI provider credentials.

For the first version, prefer a single environment such as `dev`. Split into `dev` and `prod` only after the application shape is clearer.

## Frontend Strategy

React Native can be reused for web through React Native Web, especially when using Expo. This is a good fit if the app UI is mostly shared across platforms.

Recommended direction:

- Start with an Expo-based React Native app.
- Enable web support with React Native Web.
- Keep shared UI and domain logic in common code.
- Use platform-specific files only where media playback or native behavior differs.

Expected risk: Vocavista has demanding audiovisual playback requirements. If React Native Web makes seamless web media playback too awkward, the fallback should be a dedicated React web app while keeping business logic, API client code, types, and design tokens shared with the mobile app.

## Initial Repository Shape

A practical monorepo layout would be:

```text
backend/          Spring Boot service
frontend/         Expo React Native app with web support
infrastructure/   Terraform configuration
documentation/    Product and architecture documentation
```

This keeps the project simple while making the main technical areas explicit.
