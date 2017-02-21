# TeamEdit
TeamEdit is a microservices-based sample application that allows teams to collaborate on the same documents in real-time (think collabedit).  It consists of a number of services that are built as independent Docker images and deployed to Kubernetes.

# Architecture
The application consists (currently) of 2 services:
* [UI Gateway](ui-gateway/README.md) - Application front-end that provides the browser-based editing experience.  This is an AngularJS app that communicates with the Backend Service via WebSockets to send updates made by the current user and receive updates made by other users. 
* [Backend Service](backend-service/README.md) - Merges document updates made by many clients, and alerts active clients of changes via WebSockets.  This is a Java app based on the Grizzly framework, and uses RabbitMQ messaging to communicate document changes between Backend Service nodes.

**TODO** - Add an architecture diagram

# Current State
* Currently a dead simple implementation:
    * Single document
    * Single user
    * No authentication
    * Only handles added text, not deleted text.