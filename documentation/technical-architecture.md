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

The first AWS deployment should avoid Kubernetes. The initial development setup should also avoid fixed idle costs where possible. For development, prefer serverless or externally managed free-tier services over always-on AWS resources.

Recommended near-zero-cost development setup:

- Supabase free-tier PostgreSQL for the development database.
- AWS Lambda for the backend API, using the managed Java 25 runtime.
- Lambda Function URL for initial public HTTP access, avoiding API Gateway until its features are needed.
- Amazon S3 for generated media assets.
- AWS Systems Manager Parameter Store for configuration and secrets where practical.
- Reserved Lambda concurrency to protect the Supabase database from too many simultaneous connections.

The development setup should avoid these by default:

- Amazon RDS, because it has idle database and storage cost.
- Application Load Balancer, because it has fixed hourly cost.
- NAT Gateway, because it has fixed hourly cost.
- ECS Fargate services, unless the backend is explicitly moved away from Lambda.
- VPC-attached Lambda, unless private networking becomes necessary.

For a later staging or production environment, a practical managed AWS stack could be:

- Amazon ECR for backend container images.
- Amazon ECS Fargate for running the Spring Boot backend container.
- Application Load Balancer for public HTTP(S) traffic.
- Amazon RDS for PostgreSQL.
- Amazon S3 for generated media assets.
- Amazon CloudFront for efficient media delivery when needed.
- AWS Secrets Manager or SSM Parameter Store for secrets and API keys.
- Amazon CloudWatch for logs and metrics.

This is simpler than Kubernetes while still being production-oriented and compatible with Terraform, but it should not be the default development environment because several resources have non-zero idle cost.

### Lambda Backend Direction

The Spring Boot 4 backend can run on AWS Lambda using the managed Java 25 runtime. The preferred first Lambda approach is to keep normal Spring Web MVC controllers and run the application behind AWS Lambda Web Adapter. This avoids rewriting the backend into function-style handlers while preserving a near-zero idle-cost hosting model.

Expected tradeoffs:

- Cold starts are possible after idle periods because Lambda execution environments are not guaranteed to stay warm.
- Warm execution environments can reuse the initialized JVM and Spring application context.
- Provisioned Concurrency should be avoided for development because it adds idle cost.
- Lambda SnapStart should be considered after the basic deployment works to reduce Java and Spring Boot cold-start latency.

SnapStart initializes the Java Lambda function when a published function version is created, then stores a snapshot of the initialized execution environment. Later invocations can restore from that snapshot instead of starting the JVM and Spring application from zero. It is useful for Spring Boot cold starts, but requires care with database connections, generated secrets, random values, and other state captured during initialization.

For Supabase PostgreSQL, the Lambda backend should keep the JDBC pool small and use the pooled Supabase connection string where available. Initial settings should favor database safety over throughput, such as a low Hikari maximum pool size and a low reserved concurrency value.

## Terraform

Terraform should be introduced early, even for a minimal environment, so infrastructure changes stay reproducible.

Recommended initial Terraform scope for development:

- AWS provider and project/environment naming conventions.
- Local Terraform state at first, with optional migration to remote state later.
- S3 media bucket, enabled explicitly.
- IAM role and policies for the Lambda backend.
- Lambda function definition using Java 25, once a deployable artifact is available.
- Lambda Function URL for initial HTTP access.
- SSM parameters for database URL, credentials references, and external provider API keys.
- CloudWatch log group with retention configured.
- Optional budget alarm or cost guardrail.
- Feature flags such as `enable_backend`, `enable_media_bucket`, and `enable_snapstart` so a default development apply does not create unexpected paid resources.

Resources that should not be part of the default development apply:

- RDS PostgreSQL.
- Load balancer.
- NAT Gateway.
- Always-on ECS service.

Possible later production Terraform scope:

- VPC and networking.
- RDS PostgreSQL instance.
- S3 media bucket.
- ECR repository.
- ECS Fargate service.
- Load balancer and security groups.
- IAM roles for the backend service.
- Secrets or parameters for database and external AI provider credentials.

For the first version, prefer a single environment such as `dev`. Split into `dev`, `staging`, and `prod` only after the application shape is clearer and the cost model is accepted.

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
