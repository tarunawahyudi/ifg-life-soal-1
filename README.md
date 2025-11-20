# Insurance Claim Processing System

A Quarkus-based microservice that processes insurance claims using Apache Kafka and PostgreSQL. This project was created specifically for the technical test requirements of IFG Life.

## Architecture Overview

The system processes insurance claims through the following components:

- **Kafka Topics**: Real-time message streaming for claim submissions and processed events
- **PostgreSQL Database**: Persistent storage for claims, policies, and assessments
- **Quarkus Application**: Reactive processing engine with REST API
- **Fraud Detection**: Automated risk assessment and fraud detection
- **Real-time Processing**: Event-driven claim processing with Kafka

## Tech Stack

- **Java 17**: Programming language
- **Quarkus 3.x**: Reactive Java framework
- **Apache Kafka**: Message streaming platform
- **PostgreSQL**: Relational database
- **Docker/Podman**: Containerization
- **SmallRye Reactive Messaging**: Kafka integration
- **Hibernate ORM**: Database access layer

## Features

### Core Functionality
- **Claim Submission**: REST API endpoint for submitting insurance claims
- **Real-time Processing**: Kafka-based claim processing pipeline
- **Fraud Detection**: Automated risk scoring and fraud detection
- **Priority Processing**: High-priority claim processing with dedicated Kafka topic
- **Audit Trail**: Complete claim assessment history and audit logs

### Data Models
- **Policyholders**: Customer information and management
- **Insurance Policies**: Policy details and coverage information
- **Claims**: Claim submissions with processing status
- **Claim Assessments**: Automated and manual assessment results
- **Fraud Alerts**: High-risk claim notifications

### API Endpoints
- `POST /api/claims/submit` - Submit a new insurance claim
- `GET /api/claims/{claimNumber}` - Retrieve claim details
- `GET /api/claims/pending` - List pending claims
- `GET /api/claims/high-priority` - List high-priority claims
- `GET /api/claims/health` - Health check endpoint

### Kafka Topics
- `claim-submissions` - Incoming claim submissions
- `high-priority-claims` - High-priority claim processing
- `processed-claims` - Processed claim results
- `fraud-alerts` - Fraud detection alerts
- `claim-events` - General claim events stream

## Quick Start

### Prerequisites
- Java 17+ installed
- Docker or Podman installed
- Gradle installed (or use provided wrapper)

### 1. Start Infrastructure Services

```bash
# Start Kafka, PostgreSQL, and management tools
docker-compose up -d

# Verify services are running
docker-compose ps
```

This will start:
- **Kafka**: localhost:9092
- **PostgreSQL**: localhost:5432
- **Kafka UI**: http://localhost:8080
- **pgAdmin**: http://localhost:5050

### 2. Initialize Database

The database will be automatically initialized with the schema and sample data when PostgreSQL starts.

### 3. Build and Run Application

```bash
# Build the application
./gradlew build

# Run in development mode
./gradlew quarkusDev

# Or run the built application
java -jar build/quarkus-app/quarkus-run.jar
```

### 4. Verify Installation

- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/q/health
- **OpenAPI/Swagger**: http://localhost:8080/swagger-ui
- **Metrics**: http://localhost:8080/q/metrics

## Usage Examples

### Submit a Claim via REST API

```bash
curl -X POST http://localhost:8080/api/claims/submit \
  -H "Content-Type: application/json" \
  -d '{
    "claimNumber": "CLM-12345678",
    "policyNumber": "POL001",
    "claimType": "ACCIDENT",
    "incidentDate": "2024-01-15",
    "claimedAmount": 5000.00,
    "description": "Car accident on highway",
    "priority": "NORMAL",
    "policyholderId": "PH001",
    "policyholderName": "John Doe",
    "policyholderEmail": "john.doe@email.com"
  }'
```

### Submit Claims via Kafka

```bash
# Create Kafka topic
docker exec kafka kafka-topics --create --topic claim-submissions --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# Send claim submission to Kafka
docker exec kafka kafka-console-producer --topic claim-submissions --bootstrap-server localhost:9092
```

Then paste a JSON claim submission message.

### Monitor Processing

```bash
# Consume processed claims
docker exec kafka kafka-console-consumer --topic processed-claims --bootstrap-server localhost:9092 --from-beginning

# Monitor fraud alerts
docker exec kafka kafka-console-consumer --topic fraud-alerts --bootstrap-server localhost:9092 --from-beginning
```

## Development

### Running Tests

```bash
# Run unit tests
./gradlew test

# Run integration tests
./gradlew testIntegration

# Run with coverage
./gradlew test jacocoTestReport
```

### Development Mode

```bash
# Start in development mode with hot reload
./gradlew quarkusDev

# Access development tools
# - http://localhost:8080/q/dev
# - http://localhost:8080/swagger-ui
```

### Building for Production

```bash
# Build native executable (requires GraalVM)
./gradlew build -Dquarkus.package.type=native

# Build standard JAR
./gradlew build

# Build container image
./gradlew build -Dquarkus.container-image.build=true
```

## Packaging and Running the Application

The application can be packaged using:

```shell script
./gradlew build
```

It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it's not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./gradlew build -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a Native Executable

You can create a native executable using:

```shell script
./gradlew build -Dquarkus.native.enabled=true
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/ifg-life-soal-1-1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/gradle-tooling>.

Developed by Taruna Wahyudi