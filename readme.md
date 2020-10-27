Path Manager
============

Service that manages paths for a domain. Ensures they are unique etc.

## Rationale

In a microservice architecture there may be many services that produce and render content. If all such content appears
a single domain (such as on theguardian.com) then there is potential for url collisions. This service manages a domain's
url space and allows content production services to claim urls to ensure that they are unique.

## Operations

The path manager exposes the following operations:

### Register a new path

To register a new path issue a POST request with ```path``` and ```system``` parameters. This operation will create a new path entry for the path
requested if the the path is not currently in use. An id is also generated for to identify the object that the path links to,
this id should be stored in the calling system for future operations (this is stored as the internalPageCode in composer and CAPI).

If successful this operation will return an argo JSON response with the paths registered, These are indexed by path type.

example:

```sh
    curl --data "path=foo/bar/baz&system=test" https://pathmanager.local.dev-gutools.co.uk/paths
```

returns

```json
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


### Update (or add) an existing path

This endpoint is used to migrate paths from existing systems, like the register new operation it registers a path but 
this version uses the identifier provided by the client rather than generating a new id.

To register an existing path issue a PUT request to ```/paths/<id>``` with the path record data as json in the body:

```json
    {
        "path":"<path>",
        "identifier":<id>,
        "type":"canonical", // canonical paths can be registered currently
        "system":"<system>"
    }
```

If successful this operation will return an argo JSON response with the paths registered, These are indexed by path type.

example:

```sh
    curl -i -XPUT -H"Content-Type: application/json" -d '{"path":"foo/bar/baz1","identifier":345,"type":"canonical","system":"test"}' https://pathmanager.local.dev-gutools.co.uk/paths/345
```

returns

```json
    {"data": 
        {"canonical":
            [{
            "path":"foo/bar/baz1",
            "identifier":345,
            "type":"canonical",
            "system":"test"
            }],
        "short":
            [{
            "path":"simulatedShort/foo/bar/baz1",
            "identifier":345,
            "type":"short",
            "system":"test"
            }]
        }
    }
```


### Update a canonical path

To update a canonical path for an item issue a POST request to ```/paths/<id>``` with ```path``` parameter.
If the new path is available then the old path entry is removed and the new record with the new path is inserted.

If successful this operation will return a json response with the updated canonical path record

example:

```sh
    curl --data "path=foo/bar/hux" https://pathmanager.local.dev-gutools.co.uk/paths/345
```

returns

```json
    {"data":{
            "canonical":[{"path":"foo/bar/hux","identifier":345,"type":"canonical","system":"test"}]
    }}
```


### Looking up paths

Paths can be looked up by id or searched by path. To lookup by id issue a GET request to ```/paths/<id>``` this will return a json response
with all the paths registered for that id 

example:

```sh
    curl https://pathmanager.local.dev-gutools.co.uk/paths/345
```

returns

```json
    {"data":{
        "canonical":[{"path":"foo/bar/hux","identifier":345,"type":"canonical","system":"test"}],
        "short":[{"path":"/simulatedShort/345","identifier":345,"type":"short","system":"test"}]    
    }}
```

To find what is registered on a given path issue a get request to ```/paths``` with a ```path=``` query string parameter. This will respond
with a json response in the same format as the id lookup, however only one path record will be included (matching the requested path, obviously)

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


### Deleting all paths for an item

To delete all path records for an item issue a DELETE request to ```/paths/<id>```. This will result is a 204, no content, response if successful.

This endpoint will delete the canonical path, the short path and any future path records associated with the is.

example:

```sh
    curl -i -XDELETE https://pathmanager.local.dev-gutools.co.uk/paths/345
```

will respond with a 204



## Not supported yet

The path manager does not currently support:

* redirects - These may be added at a later date once the work on canonical paths is completed.

## Running locally

Run `setup.sh` to install dependencies, this will also setup dev-nginx mappings.

To launch `path-manager` locally, either...
- `start.sh` to run the docker container for local DynamoDB and start the project in `sbt`
- OR, within `sbt` you can simply run `start`  (which starts the docker container for local DynamoDB and calls `run` on the `pathManager` sub-project, which starts the Play app). 
...the path manager should then be accessible on: [https://pathmanager.local.dev-gutools.co.uk/](pathmanager.local.dev-gutools.co.uk/).

Note, the app is configured to run on port 10000.

The local dynamo instance can be deleted with `docker-compose down -v`.

You can access [https://pathmanager-db.local.dev-gutools.co.uk/shell/](pathmanager-db.local.dev-gutools.co.uk/shell/) to query tables etc.


## Running a migration

Please see [/migrator/readme.md](migrator/readme.md).

## Refreshing from PROD

Occassionally there will be a need to refresh the path manager instance in a pre-prod stage with data from production.

The pathmanager is backed by a DynamoDB database. To import/export data from it, use AWS Data Pipeline. See [here](http://docs.aws.amazon.com/datapipeline/latest/DeveloperGuide/dp-importexport-ddb.html) for instructions on how to do this. Some notes:
 - Before starting, increase the read/write throughput of the table you are exporting/importing from. e.g. if exporting then increase the number of read capacity units to 500 for both the table and index.
 - When creating the pipeline, set DynamoDB write throughput ratio to 0.95 (this is why we increase the throughput)
 - For success/failure alerts, there's an sns topic called 'pipelinestatus' which you can link to your email address
 - Exporting from the PROD table with 2 m3.xlarge instances took less than 10 minutes when I did it. Importing the PROD data set into the CODE with 2 m1.large instances took 3.5 hours.
 - In the resources menu of 'edit in architect'
   - Increase the timeout to something larger than 2 hours (12 was plenty for me)
   - Increase the number of instances to 2 (this is what I did, I'm assuming it makes things a bit faster, but it's probably not worth putting it any higher)
   - If exporting from PROD, I recommend using m3.xlarge instances - this solved some weird errors I was getting (see [here](http://ijin.github.io/blog/2015/07/02/dynamodb-export-with-datapipeline/) (with google translate!) for more details)


## Argo JSON 

[https://github.com/argo-rest/spec](Argo JSON) is a subset of JSON developed at the Guardian which delivers a hypermedia response. Most of the clients for this microservice can consume it. It is meant in every location where JSON is used in the documentation. 