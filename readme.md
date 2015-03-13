Path Manager
============

Service that manages path for a domain. Ensures they are unique etc.

Rationale
---------

In a microservice architecture there may be many services that produce and render content. If all such content appears
a single domain (such as on theguardian.com) then there is potential for url collisions. This service manages a domain's
url space and allows content production services to claim urls to ensure that they are unique.

Running locally
---------------

The path manager requires a local version of DynamoDB, to start this just run the ```setup.sh``` script in the project root,
this will download the latest dynamo local from amazon and start it on port 10005. You can access
[http://localhost:10005/shell/](http://localhost:10005/shell/) to query tables etc.

The path manager itself is a play app so can be started by the ```run``` command in ```sbt```, the app is configured to run
on port 10000.

To run correctly in standalone mode we run behind nginx, This can be installed as follows (you may have done
this already if you work with identity, r2 or similar):

1 Install nginx:

  * *Linux:*   ```sudo apt-get install nginx```
  * *Mac OSX:* ```brew install nginx```

2 Make sure you have a sites-enabled folder under your nginx home. This should be

  * *Linux:* ```/etc/nginx/sites-enabled```
  * *Mac OSX:* ```/usr/local/etc/nginx/sites-enabled```

3 Make sure your nginx.conf (found in your nginx home) contains the following line in the http{} block:
`include sites-enabled/*;`

  * you may also want to disable the default server on 8080

4 Get the [dev-nginx](https://github.com/guardian/dev-nginx) repo checked out on your machine

5 Set up certs if you've not already done so (see dev-nginx readme)

6 Configure the pathmanager route in nginx

    sudo <path_of_dev-nginx>/setup-app.rb <path_of_path_manager>/nginx/nginx-mapping.yml
    
    
The path manager should now be accessible on:

   [https://pathmanager.local.dev-gutools.co.uk/debug](https://pathmanager.local.dev-gutools.co.uk/debug)
