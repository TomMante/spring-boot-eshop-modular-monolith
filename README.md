# Project Objective

This project serves as a study/blueprint for a spring boot modular monolith. The core idea stems from a course I took on .Net: https://learning.oreilly.com/course/net-8-backend/9781837028757/
where it showed how to implement a modular monolith using DDD, CQRS and VSA.

I took that base and ported it over to spring boot/java world and added my 2 cents. The main concepts I added are:
1. Used TDD, trying to borrow from the BDD world, using the given when then convention. (Which doesn't always seem to port well to unit tests).
2. Tried to make more abstraction so that the domain is completely split from the infrastructure using ports. This should allow for a simpler switch to a different database model, for example switching to mongoDB, should that be beneficial
3. Used 2 different repositories for write and read, this is also, to allow more flexibility. Coming from an eventsourcing project, I saw the flexibility of having different databases for write and read. With CQRS the query side directly interacts with the read model.
4. Leveraged Spring Auditables to add Auditing to objects.


## Extending the project
Some of the next steps will be:
1. Leverage Spring AI to add some examples on how spring ai can be used.
2. Add Cucumber/Serenity tests (api only)
3. Add contract testing 
4. Make one module work with MongoDB instead of JPA 
5. Make one of the modules become a separate microservice and use RabbitMQ to communicate.
6. Add dockerfile for deployment on k8s
7. Add some k8s deployment files to demonstrate how it can be deployed.
8. use of kustomize to show samples of deployment
9. Try out Kafka as a communication layer
10. AxonFramework / KurrentDb 
11. Add performance testing to test application at scale.
12. Rag.