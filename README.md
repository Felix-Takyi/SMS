# School Management System

A Java-based school management application built with Spring Boot, PostgreSQL, and Flyway. The project is designed for a local school environment and follows a modular backend architecture for managing users, admissions, academics, attendance, exams, finance, and more.

## Overview

This project provides a foundation for a school administration system with:

- User authentication and role-based access control
- Student and admissions workflows
- Academic year, classes, subjects, and enrollments
- Attendance tracking
- Assessment and grading support
- Finance and payment records
- Audit and security logging
- Local PostgreSQL persistence with database migrations

## Tech Stack

- Java 21
- Spring Boot 3.5.3
- Spring Web
- Spring Data JPA
- Spring Security
- Validation
- PostgreSQL
- Flyway
- Maven

## Project Structure

```text
.
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   │       └── java/
├── docs/
│   └── architecture.md
├── compose.yaml
├── pom.xml
├── README.md
└── target/
```

## Prerequisites

Before running the application, make sure you have:

- Java 21 or newer
- Maven 3.9+
- Docker Desktop or Docker Engine (for PostgreSQL container)
- PostgreSQL client tools (optional, for manual database checks)

## Database Setup

This project includes a Docker Compose file for PostgreSQL.

Start the database:

```bash
docker compose up -d postgres
```

The compose configuration uses:

- Database: `school_management`
- Username: `school_app`
- Port: `5432`

Set the `DB_PASSWORD` environment variable before running the app if needed:

```bash
export DB_PASSWORD=your_secure_password
```

On Windows PowerShell:

```powershell
$env:DB_PASSWORD="your_secure_password"
```

## Run the Application

From the project root:

```bash
mvn spring-boot:run
```

The application entry point is:

```text
backend/src/main/java/com/school/management/SchoolManagementApplication.java
```

## Build the Project

```bash
mvn clean package
```

## Run Tests

```bash
mvn test
```

## Configuration Notes

The application is configured via Spring Boot properties in the backend resources folder. The exact database connection settings and app configuration should be reviewed in the project’s `application.yml` file before running in a local or production environment.

## Notes

- The project is intentionally structured as a modular monolith.
- Flyway manages database schema changes.
- The documentation in the `docs/architecture.md` file describes the architecture and product scope in more detail.

## License

This project does not currently include a license file. Add one if you plan to distribute or publish the project.
