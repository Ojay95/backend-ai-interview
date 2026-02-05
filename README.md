# 🧠 MockInterview.ai - Backend API

The robust, scalable **Spring Boot** application that powers the MockInterview.ai platform. It handles real-time AI integration, secure authentication, document processing, and interview session management using **Domain-Driven Design (DDD)**.

---

## 🛠️ Tech Stack

* **Core Framework**: Spring Boot 3.4+ (Java 21)
* **AI Engine**: Spring AI (Google Gemini 1.5 Pro/Flash Integration)
* **Database**: PostgreSQL 15 (Primary Data Store)
* **Caching**: Redis 7 (Session State, Rate Limiting)
* **Security**: Spring Security 6 + JWT (Stateless Authentication)
* **PDF Processing**: Apache PDFBox 3.0 (CV Parsing)
* **Build Tool**: Maven
* **Testing**: JUnit 5, Testcontainers

---

## 📂 Project Structure

The project follows a modular **Domain-Driven Design** architecture to ensure separation of concerns:

```text
src/main/java/com/ai_interview/
├── common/                 # Global configurations, exceptions, utils
├── config/                 # Security, CORS, Swagger, & App Config
├── domain/
│   ├── auth/               # User Registration, Login, JWT
│   ├── cv/                 # Resume Parsing & Gemini Analysis
│   ├── interview/          # Session Logic, Transcripts, Scoring
│   ├── analytics/          # Dashboard Stats & Aggregation
│   ├── payment/            # Subscription & Mock Payments
│   ├── support/            # Contact Form Handling
│   └── user/               # User Settings & Preferences
└── MockInterviewApplication.java

```

---

## 🚀 Getting Started

### 1. Prerequisites

Ensure you have the following installed:

* **Java 21 JDK**
* **Docker Desktop** (for PostgreSQL & Redis)
* **Maven** (or use the included `mvnw` wrapper)
* **Google AI Studio API Key**

### 2. Infrastructure Setup (Docker)

Start the required databases using Docker:

```bash
# Start PostgreSQL
docker run --name mock-postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password -e POSTGRES_DB=mock_interview -p 5432:5432 -d postgres:15

# Start Redis
docker run --name mock-redis -p 6379:6379 -d redis:7

```

### 3. Configuration

Locate `src/main/resources/application.properties` and configure your environment variables:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/mock_interview
spring.datasource.username=postgres
spring.datasource.password=password

# Google Gemini AI Key
spring.ai.google.genai.api-key=YOUR_GOOGLE_API_KEY_HERE

# JWT Security
app.jwt.secret=YOUR_SUPER_SECRET_256_BIT_KEY_HERE
app.jwt.expiration-ms=86400000

# File Upload Limits
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB

```

### 4. Run the Application

You can start the server using the Maven wrapper:

```bash
./mvnw spring-boot:run

```

The server will start on `http://localhost:8080`.

---

## 🔌 API Endpoints Overview

| Module | Method | Path | Description |
| --- | --- | --- | --- |
| **Auth** | `POST` | `/api/v1/auth/register` | Register new user |
|  | `POST` | `/api/v1/auth/login` | Login and receive JWT |
|  | `GET` | `/api/v1/users/profile` | Get current user details |
| **CV** | `POST` | `/api/v1/cv/analyze` | Upload PDF & get AI critique |
|  | `GET` | `/api/v1/cv/history` | Get past analysis reports |
| **Interview** | `POST` | `/api/v1/interviews/analyze` | Submit session for grading |
|  | `GET` | `/api/v1/interviews/history` | Get user session history |
| **Analytics** | `GET` | `/api/v1/analytics/dashboard` | aggregated performance stats |
| **Settings** | `PUT` | `/api/v1/user/preferences` | Update AI persona settings |
| **Support** | `POST` | `/api/v1/public/support/contact` | Submit help request (Public) |

---

## 🧪 Running Tests

The project includes **Integration Tests** using `Testcontainers` to spin up ephemeral database instances for testing.

```bash
# Run all tests
./mvnw test

```

---

## 🔒 Security Notes

* **JWT Auth**: All endpoints (except `/auth/**` and `/public/**`) require a valid Bearer Token.
* **CORS**: Configured to allow requests from `http://localhost:3000` (React Frontend).

---

*Backend maintained by the MockInterview.ai Team.*