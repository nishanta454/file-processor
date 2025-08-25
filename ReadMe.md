# Spring Boot File Processing Service

## Overview

A Spring Boot application that provides a REST API for uploading CSV files, storing them in PostgreSQL, and processing them asynchronously. The service includes rate limiting, status tracking, and downstream API integration.

## Features

- 📤 **File Upload API** - Upload CSV files via multipart/form-data
- 🏡 **Rate Limiting** - Configurable rate limiting using Bucket4j (default: 1 request per 10 minutes)
- 🗄% **PostgreSQL Storage** - Files stored as BYTEA with UUID primary keys
- ⚡ **Async Processing** - Background processing of CSV files line by line
- 📊 #Ktatus Tracking** - Real-time status updates (queued → in-progress → processed/failed)
- 🔗 **Downstream Integration** - Sends processed records to external API
- 🏯 **Spring Data JPA~** - Database operations with Hibernate

## Technology Stack

- **Java 17**
- **Spring Boot 3.1.0**
- **Spring Data JPA**
- **PostgreSQL**
- **Bucket4j (Rate Limiting)**
- **Gradle**
- **Lombok**

## Prerequisites

- Java 17 or higher
- PostgreSQL 12+
- Gradle 7.0+

## Database Setup

1. Create PostgreSQL database:
```sql
CREATE DATABASE mcsdb;
```

2. Run the schema script:
```sql
CREATE TABLE mcs_etl_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name VARCHAR(255) NOT NULL,
    file_content BYTEA NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'queued',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mcs_etl_files_status ON mcs_etl_files(status);
```

## Configuration

Update `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/mcsdb
spring.datasource.username=your_username
spring.datasource.password=your_password

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

# Server
server.port=8080

# Rate Limiting
upload.api.rate-limit.minutes=10

# Downstream API
downstream.api.url=http://localhost:8080/api/print
```

## Installation & Running

1. **Clone the repository**
```bash
git clone <repository-url>
cd file-processor
```

2. **Build the project**
```bash
./gradlew build
```

3. **Run the application**
```bash
./gradlew bootRun
```

Or run with custom port:
```bash
./gradlew bootRun --args='--server.port=8090'
```

## API Endpoints

### Upload File
```http
POST /api/files/upload
Content-Type: multipart/form-data

Form Data:
- file: [CSV file]
```

**Response:** UUID content ID
```
a1b2c3d4-e5f6-7890-1234-567890abcdef
```

### Check Status
```http
GET /api/files/status/{contentId}
```

**Response:** Current processing status
```json
"IN_PROGRESS"
```

Possible statuses: `IN_QUEUE`, `IN_PROGRESS`, `PROCESSEDXand `FAILED`

## Usage Examples

1. **Upload a CSV file:**
```bash
curl -X POST \
  http://localhost:8080/api/files/upload \
  -H "Content-Type: multipart/form-data" \
  -F "file=@sample.csv"
```

2. **Check processing status:**
```bash
curl http://localhost:8080/api/files/status/your-uuid-here
```

3. **Create test file:**
```bash
echo -e "name,age,city\nJohn,25,PYC\nJane,30,LA" > test.csv
```

## Rate Limiting

- Default: 1 upload per 10 minutes per IP address
- Configurable via `upload.api.rate-limit.minutes` property
- Uses Bucket4j with in-memory token bucket algorithm

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   HTTP Client   │───▶│   HTTP Server    │───▶│ Request Handler │
│  (Spring Boot)  │    │  (Boost.Asio)    │    │   (JSON Parse)  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                         │
                                                         ▼
                                               ┌─────────────────┐
                                               │ Console Output  │
                                               └─────────────────┘
```