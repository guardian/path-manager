Url Manager
===========

Service that manages urls for a domain. Ensures they are unique etc.

Rationale
---------

In a microservice architecture there may be many services that produce and render content. If all such content appears
a single domain (such as on theguardian.com) then there is potential for url collisions. This service manages a domain's
url space and allows content production services to claim urls to ensure that they are unique.

