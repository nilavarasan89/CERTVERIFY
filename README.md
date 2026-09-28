# CertVerify – Digital Certificate Issuance and Public Verification System

CertVerify is a production-grade, academic backend REST API built using **Java 21** and **Spring Boot 3.2.5**. It solves the problem of certificate forgery and alteration by providing institutions with a secure platform to issue, publicly verify, audit, and revoke completion certificates.

---

## 📋 Table of Contents
1. [Project Overview](#-project-overview)
2. [Key Features & Business Rules](#-key-features--business-rules)
3. [Technology Stack](#-technology-stack)
4. [Package Structure](#-package-structure)
5. [Database Setup](#-database-setup)
6. [API Reference & Usage](#-api-reference--usage)
7. [PDF & QR Code Generation](#-pdf--qr-code-generation)
8. [Testing & Postman Collection](#-testing--postman-collection)
9. [How to Build & Run](#-how-to-build--run)
10. [Student Exam / Viva Q&A Guide](#-student-exam--viva-qa-guide)

---

## 🎯 Project Overview

Institutions issue course/event completion certificates as PDFs or paper documents. However, these documents can easily be altered, forged, or fake-generated.

**CertVerify** establishes a centralized verification platform:
- Every certificate gets a unique, human-readable Certificate ID (e.g., `CERT-100001`).
- Every certificate gets a globally unique, cryptographically secure random Verification Code (e.g., `CV-X8K29PLQ`).
- Anyone can verify authenticity using the public API without authentication.
- Administrators can revoke certificates (e.g., for academic dishonesty or administrative errors). Revoked certificates permanently report `INVALID` / `REVOKED`.
- Every verification query is logged in `VerificationRecord` for audit trails and analytics.

---

## 🔒 Key Features & Business Rules

1. **Globally Unique Verification Code**: Generated server-side using strong randomness (`SecureRandom`) and checked against the database before persistence.
2. **Sequential & Unique Certificate ID**: Server-assigned format (`CERT-100001`, `CERT-100002`...).
3. **Revocation Enforcement**: Revoking a certificate sets `revoked = true` in MySQL. Revoked certificates are never deleted to preserve historical data.
4. **Verification Logic**:
   - **Valid Code**: Returns `valid: true`, `status: "VALID"`.
   - **Revoked Code**: Returns `valid: false`, `status: "REVOKED"`.
   - **Unknown Code**: Returns `valid: false`, `status: "INVALID"`.
5. **Audit Logging**: Every verification attempt (valid or invalid) logs timestamp, searched code, target certificate reference, and result.
6. **Analytics & Statistics**: Provides real-time metrics per course and overall across the system.
7. **Clean Validation & Error Responses**: Input constraints are enforced with `Jakarta Validation`. Exceptions return clean JSON without exposing stack traces.

---

## 🛠️ Technology Stack

- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.2.5
- **Modules**: Spring Web, Spring Data JPA, Spring Validation
- **ORM & Persistence**: Hibernate 6.4, MySQL 8.x (H2 supported for in-memory testing)
- **Build Tool**: Apache Maven
- **PDF Generation**: Apache PDFBox 3.0.2
- **QR Code Generation**: ZXing 3.5.3
- **Utilities**: Lombok

---

## 📁 Package Structure

```
com.certverify
│
├── CertverifyApplication.java              # Main Spring Boot Entry Point
│
├── config
│   └── CorsConfig.java                     # Global CORS configuration
│
├── controller
│   ├── ParticipantController.java          # Participant management APIs
│   ├── CourseController.java               # Course/Event management APIs
│   ├── CertificateController.java          # Issuance, Verification & Revocation APIs
│   └── StatisticsController.java           # Issuance & Verification analytics APIs
│
├── entity
│   ├── Participant.java                    # Participant entity (@Table participants)
│   ├── Course.java                         # Course entity (@Table courses)
│   ├── Certificate.java                    # Certificate entity (@Table certificates)
│   └── VerificationRecord.java             # Verification log entity (@Table verification_records)
│
├── repository
│   ├── ParticipantRepository.java          # JPA repository for Participants
│   ├── CourseRepository.java               # JPA repository for Courses
│   ├── CertificateRepository.java          # JPA repository for Certificates
│   └── VerificationRecordRepository.java   # JPA repository for Verification Records
│
├── service
│   ├── ParticipantService.java             # Participant business logic
│   ├── CourseService.java                  # Course business logic
│   ├── CertificateService.java             # Core certificate issuance & verification logic
│   ├── StatisticsService.java              # System & Course statistics calculator
│   ├── PdfGeneratorService.java            # PDF certificate generator using PDFBox
│   └── QrCodeGeneratorService.java         # QR Code image generator using ZXing
│
├── dto
│   ├── ParticipantRequest.java             # Participant creation DTO
│   ├── ParticipantResponse.java            # Participant response DTO
│   ├── CourseRequest.java                  # Course creation DTO
│   ├── CourseResponse.java                 # Course response DTO
│   ├── CertificateRequest.java             # Certificate issuance DTO
│   ├── CertificateResponse.java            # Certificate response DTO
│   ├── VerificationResponse.java           # Public verification result DTO
│   ├── RevokeResponse.java                 # Revocation result DTO
│   ├── VerificationHistoryResponse.java    # Audit history item DTO
│   └── StatisticsResponse.java             # Course/System statistics DTO
│
└── exception
    ├── ResourceNotFoundException.java       # HTTP 404 handler
    ├── DuplicateResourceException.java      # HTTP 409 handler
    ├── BusinessException.java               # HTTP 400 handler
    ├── ErrorResponse.java                  # Structured error output DTO
    └── GlobalExceptionHandler.java          # @RestControllerAdvice
```

---

## 🗄️ Database Setup

### 1. Create MySQL Database
Run this command in MySQL Workbench, phpMyAdmin, or MySQL CLI:
```sql
CREATE DATABASE certverify;
```

### 2. Configure `application.properties`
The application connects to MySQL automatically:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/certverify?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:root}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

> **Note**: An in-memory H2 database configuration is also included in `application-h2.properties` for instant offline testing without installing MySQL.

---

## 🚀 API Reference & Usage

### 1. Participant APIs
- **Create Participant**: `POST /api/participants`
  ```json
  {
    "name": "Rahul Kumar",
    "email": "rahul@gmail.com",
    "phone": "9876543210"
  }
  ```
- **Get All Participants**: `GET /api/participants`
- **Get Participant by ID**: `GET /api/participants/{id}`

### 2. Course APIs
- **Create Course**: `POST /api/courses`
  ```json
  {
    "courseName": "Java Programming",
    "description": "Spring Boot Training",
    "eventDate": "2026-09-28"
  }
  ```
- **Get All Courses**: `GET /api/courses`
- **Get Course by ID**: `GET /api/courses/{id}`

### 3. Certificate APIs
- **Issue Certificate**: `POST /api/certificates`
  ```json
  {
    "participantId": 1,
    "courseId": 1
  }
  ```
  *Response:*
  ```json
  {
    "id": 1,
    "certificateId": "CERT-100001",
    "verificationCode": "CV-X8K29PLQ",
    "participantId": 1,
    "participantName": "Rahul Kumar",
    "participantEmail": "rahul@gmail.com",
    "courseId": 1,
    "courseName": "Java Programming",
    "issuedDate": "2026-09-28",
    "revoked": false,
    "status": "VALID"
  }
  ```

- **Get Certificate Details**: `GET /api/certificates/{id}`

- **Public Verification**: `GET /api/certificates/verify/{verificationCode}`
  *Valid Response:*
  ```json
  {
    "valid": true,
    "certificateId": "CERT-100001",
    "verificationCode": "CV-X8K29PLQ",
    "participantName": "Rahul Kumar",
    "courseName": "Java Programming",
    "issuedDate": "2026-09-28",
    "status": "VALID",
    "message": "Certificate is valid and authentic"
  }
  ```
  *Revoked Response:*
  ```json
  {
    "valid": false,
    "certificateId": "CERT-100001",
    "verificationCode": "CV-X8K29PLQ",
    "participantName": "Rahul Kumar",
    "courseName": "Java Programming",
    "issuedDate": "2026-09-28",
    "status": "REVOKED",
    "message": "Certificate has been revoked"
  }
  ```
  *Invalid Response:*
  ```json
  {
    "valid": false,
    "status": "INVALID",
    "message": "Invalid verification code"
  }
  ```

- **Revoke Certificate**: `PUT /api/certificates/{id}/revoke`
  ```json
  {
    "message": "Certificate revoked successfully",
    "certificateId": "CERT-100001",
    "status": "REVOKED"
  }
  ```

- **Verification History**: `GET /api/certificates/{id}/verification-history`
  ```json
  [
    {
      "verifiedAt": "2026-09-28T10:30:00",
      "result": "VALID"
    },
    {
      "verifiedAt": "2026-09-28T11:10:00",
      "result": "INVALID"
    }
  ]
  ```

### 4. Statistics APIs
- **Course Statistics**: `GET /api/statistics/course/{courseId}`
  ```json
  {
    "courseId": 1,
    "courseName": "Java Programming",
    "totalCertificates": 100,
    "validCertificates": 95,
    "revokedCertificates": 5
  }
  ```
- **Overall Statistics**: `GET /api/statistics`
  ```json
  {
    "totalCertificates": 150,
    "validCertificates": 142,
    "revokedCertificates": 8,
    "totalVerificationAttempts": 325
  }
  ```

---

## 📄 PDF & QR Code Generation

- **Download PDF Certificate**: `GET /api/certificates/{id}/pdf`
  Renders an A4 PDF certificate complete with institution headers, participant details, issue date, verification code, and an embedded QR Code.
- **Get QR Code Image**: `GET /api/certificates/{id}/qr`
  Returns a high-resolution PNG QR Code image pointing to the public verification endpoint.

---

## 📮 Postman Collection

A ready-to-import Postman collection is included in the project root:
- `CertVerify.postman_collection.json`

Import this file directly into Postman to test all endpoints out-of-the-box.

---

## 🔨 How to Build & Run

### 1. Compile & Run Tests
```bash
mvn clean test
```

### 2. Run the Application
```bash
mvn spring-boot:run
```
Or run with H2 profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

---

## 🎓 Student Exam / Viva Q&A Guide

**Q1: Why do we use DTOs instead of exposing JPA Entities directly?**
*Answer:* DTOs isolate API responses from database schema changes, prevent circular JSON serialization infinite loops in `@ManyToOne`/`@OneToMany` relationships, and hide internal sensitive database fields.

**Q2: How does the system handle certificate revocation?**
*Answer:* Instead of deleting records from MySQL (which destroys historical records), we update `revoked = true`. Future verification requests check this flag and return `REVOKED` / `valid: false`.

**Q3: How is verification code collision prevented?**
*Answer:* Verification codes use `SecureRandom` characters prefixed with `CV-`. Before saving, `CertificateRepository.existsByVerificationCode()` checks the database. If a collision occurs, a loop generates a new code until a unique one is produced. Additionally, a DB unique constraint enforces uniqueness at the schema level.

**Q4: How are verification attempts tracked?**
*Answer:* Every call to `/api/certificates/verify/{code}` creates a `VerificationRecord` entry in MySQL storing timestamp, searched code, target certificate reference, and result (`VALID`, `REVOKED`, or `INVALID`).
