Path Manager
============

Service that manages paths for a domain. Ensures they are unique etc.

Rationale
=========

In a microservice architecture there may be many services that produce and render content. If all such content appears
a single domain (such as on theguardian.com) then there is potential for url collisions. This service manages a domain's
url space and allows content production services to claim urls to ensure that they are unique.

Operations
==========

The path manager exposes the following operations:

Register a new path
-------------------

To register a new path issue a POST request with ```path``` and ```system``` parameters. This operation will create a new path entry for the path
requested iff the the path is not currently in use. An id is also generated for to identify the object that the path links to,
this id should be stored in the calling system for future operations (this is stored as the pageId in R2 and composer,
replacing the previous pageId sequence in oracle).

If successful this operation will return an argo JSON response with the paths registered, These are indexed by path type.

example:

```
    curl --data "path=foo/bar/baz&system=test" https://pathmanager.local.dev-gutools.co.uk/paths
```

returns

```
    {data: 
        {"canonical":
            [{
            "path":"foo/bar/baz",
            "identifier":2000051,
            "type":"canonical",
            "system":"test"
            }],
        "short":
            [{
            "path":"simulatedShort/foo/bar/baz",
            "identifier":2000051,
            "type":"short",
            "system":"test"
            }]
        }
    }
```


Add (or update) an existing path
--------------------------------

This endpoint is used to migrate paths from existing systems, like the register new operation it registers a path but 
this version uses the identifier provided by the client rather than generating a new id.

To register an existing path issue a PUT request to ```/paths/<id>``` with the path record data as json in the body:

```
    {
        "path":"<path>",
        "identifier":<id>,
        "type":"canonical", // canonical paths can be registered currently
        "system":"<system>"
    }
```

If successful this operation will return a JSON response with the paths registered, These are indexed by path type.

example:

```
    curl -i -XPUT -H"Content-Type: application/json" -d '{"path":"foo/bar/baz1","identifier":345,"type":"canonical","system":"test"}' https://pathmanager.local.dev-gutools.co.uk/paths/345
```

returns

```
    {
    "canonical": {"path":"foo/bar/baz1","identifier":345,"type":"canonical","system":"test"},
    "short":{"path":"simulatedShort/foo/bar/baz1","identifier":345,"type":"short","system":"test"}
    }
```


Update a canonical path
----------------------

To update a canonical path for an item issue a POST request to ```/paths/<id>``` with ```newPath``` parameter.
If the new path is available then the old path entry is removed and the new record with the new path is inserted.

If successful this operation will return a argo json response with the updated canonical path record

example:

```
    curl --data "newPath=foo/bar/hux" https://pathmanager.local.dev-gutools.co.uk/paths/345
```

returns

```
    {"data":{
            "canonical":[{"path":"foo/bar/hux","identifier":345,"type":"canonical","system":"test"}]
    }}
```


Looking up paths
----------------

Paths can be looked up by id or searched by path. To lookup by id issue a GET request to ```/paths/<id>``` this will return and argo json response
with all the paths registered for that id 

example:

```
    curl https://pathmanager.local.dev-gutools.co.uk/paths/345
```

returns

```
    {"data":{
        "canonical":[{"path":"foo/bar/hux","identifier":345,"type":"canonical","system":"test"}],
        "short":[{"path":"/simulatedShort/345","identifier":345,"type":"short","system":"test"}]    
    }}
```

To find what is registered on a given path issue a get request to ```/paths``` with a ```path=``` query string parameter. This will respond
with an argo json response in the same format as the id lookup, however only one path record will be included (matching the requested path, obviously)

example:

```
    curl https://pathmanager.local.dev-gutools.co.uk/paths?path=foo/bar/hux
```

returns

```
    {"data":{
        "canonical":[{"path":"foo/bar/hux","identifier":345,"type":"canonical","system":"test"}]   
    }}
```

If a path is not found then the endpoint will respond with a 404 response. The lookup endpoints also support HEAD requests which can be used to 
check if a path is in use (by checking if the response is a 404 or 200).

Not supported yet
=================

The path manager does not currently support:

* deleting paths - This will be implemented alongside the clients.
* bulk import of paths - This will be implented once the system is running and trickle migration is active
* short url generation - There is currently a nod towards this but it's all smoke and mirrors
* redirects - These may be added at a later date once the work on canonical paths is completed.

Running locally
===============

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
