[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=TomMante_SpringBoot-EshopModularMonolith&metric=alert_status&token=f2ed3a05ab760065662b7d00d52fab4ebdc4e4d0)](https://sonarcloud.io/summary/new_code?id=TomMante_SpringBoot-EshopModularMonolith)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=TomMante_SpringBoot-EshopModularMonolith&metric=coverage&token=f2ed3a05ab760065662b7d00d52fab4ebdc4e4d0)](https://sonarcloud.io/summary/new_code?id=TomMante_SpringBoot-EshopModularMonolith)

# Project Objective

This project serves as a study/blueprint for a Spring Boot modular monolith. It also deliberately pushes microservice-readiness to an extreme, both to explore its complexity and to play around with some ideas.

The core idea stems from a course I took on .NET: https://learning.oreilly.com/course/net-8-backend/9781837028757/
where it showed how to implement a modular monolith using DDD, CQRS and VSA.

## 📖 Documentation Index

Detailed documentation is located in the `docs/` directory:
- **[Architecture (DDD, CQRS, Modules)](docs/ARCHITECTURE.md)**
- **[Development Guidelines & Notes](docs/DEVELOPMENT.md)**
- **[Known Issues (defects & testing gaps)](docs/KNOWN_ISSUES.md)**
- **[Setup Instructions](docs/SETUP.md)**

## Repository Structure

### SpringBoot-EshopModularMonolith
This contains a Spring Boot 4 application, which is the main adaptation of the .NET course.

### Changes from original .NET course

I took that base and ported it over to the Spring Boot/Java world and added my own ideas. The main concepts I added are:
1. Used TDD, trying to borrow from the BDD world, using the given-when-then convention (which doesn't always seem to port well to unit tests).
2. Tried to add more abstraction so that the domain is completely split from the infrastructure using ports. This should allow for a simpler switch to a different database model, for example switching to MongoDB, should that ever prove beneficial.
3. Used two different repositories for write and read, to allow more flexibility. Coming from an event-sourcing project, I saw the value of having different databases for write and read. With CQRS, the query side interacts directly with the read model.
4. Leveraged Spring Auditables to add auditing to objects.

### Extending the project
Some of the next steps will be:
- [ ] Leverage Spring AI to add some examples on how Spring AI can be used.
- [ ] Add Cucumber/Serenity tests (API only)
- [ ] Add contract testing
- [x] Make one module work with MongoDB instead of JPA
- [ ] Make one of the modules become a separate microservice and use RabbitMQ to communicate.
- [x] Add Dockerfile
- [ ] Try out Kafka as a communication layer
- [ ] AxonFramework / KurrentDB
- [ ] Add performance testing to test application at scale.
- [ ] RAG.
- [x] Secure application with Keycloak.
- [ ] Explore Keycloak policy enforcer
- [ ] Consider Roles in Keycloak vs Roles in application
- [ ] Investigate Mongock (https://mongock.io/) as an alternative to Flyway for Mongo (data migration instead of schema migration)
- [ ] Consider making all aggregates immutable, to ensure thread safety at the domain level.

## Kubernetes deployment files
This part provides a set of k8s manifests to deploy the various components of the application.
The basis is to deploy on a local Minikube.

### Extending the project
- [ ] Add some k8s deployment files to demonstrate how it can be deployed.
- [ ] Use of Kustomize to show samples of deployment.
- [ ] Add example on how to use ArgoCD to deploy using GitOps.