# 🎓 Student Information & Certificate Management System (SICMS) — Backend

Enterprise RESTful Web API built with **Java 21** and **Spring Boot 3.3.5** for managing student records, faculty section assignments, document verification workflows, and Supabase integration.

---

## 🚀 Features

- **🔐 Stateless Authentication & Authorization**:
  - JWT Authentication (Access Tokens & Refresh Tokens).
  - Google OAuth2 Single Sign-On ID Token validation.
  - Email 2FA & OTP security services via Spring Mail SMTP.
  - Role-based endpoint authorization (`ROLE_ADMIN` vs `ROLE_FACULTY`).
- **👨‍🎓 Student Management API**:
  - Server-side multi-field filtering, pagination, and full-text searching.
  - Digital ID card payload & QR verification code generation.
  - Student photo management.
- **👨‍🏫 Faculty & Academic Scope API**:
  - Academic Groups (`CSE-AIML`, `ECE`) and Section mappings.
  - Scoped data enforcement (Faculty members access only assigned sections).
- **📜 Certificate & Document Management API**:
  - PDF Upload, Versioning, Approval (`VERIFIED`), and Rejection (`REJECTED`) workflows.
  - Automated missing document audit engines.
  - Direct PDF binary file streaming for secure inline browser previews.
- **🗑️ True Cascade Deletion Engine**:
  - Atomic database transactions (`@Transactional`).
  - Purges linked PDF certificates & profile photos from **Supabase Storage** before removing database records.

---

## 🛠️ Technology Stack

- **Language & Runtime**: Java 21 LTS
- **Framework**: Spring Boot 3.3.5
- **Security**: Spring Security, JJWT (`io.jsonwebtoken` 0.12.6), Google API Client
- **Database**: Supabase PostgreSQL (`jdbc:postgresql://`)
- **Connection Pool**: HikariCP tuned for Supavisor session pooler
- **Storage**: Supabase Object Storage Client
- **Build System**: Maven

---

## 💻 Local Development Setup

### 1. Prerequisites
- Java 21 Development Kit (JDK 21) installed
- Apache Maven 3.9+

### 2. Environment Configuration
Copy `.env.example` to `.env` or set environment variables:
```bash
export DB_URL="jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require"
export DB_USERNAME="postgres.ookzjdmkoaunbrufvmvq"
export DB_PASSWORD="YourPassword"
export JWT_SECRET="c2ljbXNfc3VwZXJfc2VjcmV0X2tleV9mb3Jfand0X2F1dGhlbnRpY2F0aW9uXzIwMjZfc2VjdXJlX2tleQ=="
```

### 3. Compile & Test
```bash
cd Backend
mvn test-compile
```

### 4. Run Application
```bash
mvn spring-boot:run
```
The backend API server will start on `http://localhost:8080/api`.

---

## 📦 Containerization & Oracle Cloud VM Deployment

### Option A: Deploy via Docker
```bash
# Build Docker Image
docker build -t sicms-backend .

# Run Docker Container
docker run -d -p 8080:8080 --name sicms-api sicms-backend
```

### Option B: Deploy to Oracle Cloud Always Free VM
Refer to the complete step-by-step production setup guide in [`deployment/ORACLE_CLOUD_SETUP.md`](deployment/ORACLE_CLOUD_SETUP.md) covering:
- Systemd Service unit setup (`sicms-backend.service`)
- Automated deployment bash script (`deployment/deploy.sh`)
- Nginx Reverse Proxy & Let's Encrypt SSL configuration (`deployment/nginx.conf`)

---

## 📂 Backend Package Structure

```
src/main/java/com/sicms/
├── config/       # Spring Security, CORS, and DataInitializer
├── controller/   # REST Controllers (Auth, Student, Faculty, Document, Academic)
├── dto/          # Data Transfer Objects & Request/Response validators
├── entity/       # JPA Relational Data Entities (User, Student, Faculty, Document)
├── exception/    # Global Exception Handlers & custom exceptions
├── repository/   # Spring Data JPA Repositories
├── security/     # JwtAuthenticationFilter, JwtService, CustomUserDetails
└── service/      # Core Business Logic & Supabase Storage integration
```

---

## 📄 License
Distributed under the MIT License. See `LICENSE` for details.
