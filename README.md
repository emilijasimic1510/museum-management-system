# Museum and Exhibition Management System

Diploma thesis project developed at the Faculty of Technical Sciences,
University of Novi Sad.

Student: Emilija Simić  
Index: RA 3/2022

## Project overview

The system supports museum and exhibition management, digital catalogues,
ticket sales and validation, visitor activities, guided tours, reports and
personalized exhibition routes.

## Architecture

- Angular frontend
- Spring Boot backend
- PostgreSQL relational database
- Neo4j graph database
- Docker Compose development environment

PostgreSQL is the source of truth for business, transactional and personal
data. Neo4j is used only for relationships between cultural content and
recommendation generation.

## Planned user roles

- Visitor
- Curator
- Cashier
- Guide
- Manager
- Administrator

## Main functional areas

- Museum and hall management
- Exhibition and exhibit management
- Digital exhibition catalogue
- Ticket purchase and QR code generation
- Ticket validation and visit tracking
- Digital audio guide
- Events and guided tours
- Visitor interests and ratings
- Personalized recommendations and routes
- Attendance and revenue reports

## Repository structure

- `backend/` – Spring Boot application
- `frontend/` – Angular application
- `database/` – migrations, seed data and database diagrams
- `docs/` – specification, EER and UML diagrams
- `legacy/` – cleaned original JDBC project

## Current status

- [x] Extended project specification
- [x] Initial EER model
- [ ] Relational model
- [ ] Spring Boot project setup
- [ ] Angular project setup
- [ ] PostgreSQL migrations
- [ ] Authentication and authorization
- [ ] Core functional verticals
- [ ] Neo4j recommendation module
- [ ] Testing and documentation

## Running the project

Setup instructions will be added after the initial backend, frontend and
Docker configuration are completed.