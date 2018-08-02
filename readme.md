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


### Add (or update) an existing path

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

If successful this operation will return an argo response with the paths registered, These are indexed by path type.

example:

```
    curl -i -XPUT -H"Content-Type: application/json" -d '{"path":"foo/bar/baz1","identifier":345,"type":"canonical","system":"test"}' https://pathmanager.local.dev-gutools.co.uk/paths/345
```

returns

```
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

If successful this operation will return a argo json response with the updated canonical path record

example:

```
    curl --data "path=foo/bar/hux" https://pathmanager.local.dev-gutools.co.uk/paths/345
```

returns

```
    {"data":{
            "canonical":[{"path":"foo/bar/hux","identifier":345,"type":"canonical","system":"test"}]
    }}
```


### Looking up paths

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


### Deleting all paths for an item

To delete all path records for an item issue a DELETE request to ```/paths/<id>```. This will result is a 204, no content, response if successful.

This endpoint will delete the canonical path, the short path and any future path records associated with the is.

example:

```
    curl -i -XDELETE https://pathmanager.local.dev-gutools.co.uk/paths/345
```

will respond with a 204



## Not supported yet

The path manager does not currently support:

* redirects - These may be added at a later date once the work on canonical paths is completed.

## Running locally

The path manager requires a local version of DynamoDB, to start this just run the ```setup.sh``` script in the project root,
this will download the latest dynamo local from amazon and start it on port 10005. You can access
[http://localhost:10005/shell/](http://localhost:10005/shell/) to query tables etc.

The path manager itself is a play app so can be started by the ```run``` command in the `pathManager` sub project in ```sbt```, the app is configured to run
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
   

## Running a migration

The migrator sub project produces a executable jar which will migrate data from an R2 database into the path manager.

To run you will need a `migrator.properties` file in the same directory you are executing from. The format of this file is:

```
    databaseAddress=10.0.0.127
    databaseService=gudevelopersdb.gucode.gnl
    user=deliveryXX 
    password=XXXXXXXXXXXX 
    pathManagerUrl=http://pathmanager.local.dev-gutools.co.uk/

```

Note the address will likely be an ip address as we've not yet got our internal DNS accessible from our VPC. The service
is what oracle is calling the database, typically the bit at the end of a jdbc connectionString. Creating this file with
the correct values is left as a exercise for the reader.

To run the migrator put a copy of the jar on an r2 admin instance (or a GC2 instance with access to the database), set up the config and then execute

```
    java -jar migrator.jar
```

This will read all the paths in the R2 db and send them to the the environment's pathmanager to insert them into dynamo.
It will also bump the dynamo sequence to larger than the R2 one.

If you only want to migrate the sequence run


```
    java -jar migrator.jar seq
```

The recommended way to migrate the path data is to export the R2 data to a dynamo backup file


```
    java -jar migrator.jar exportDyn
```

This will create a paths.dyn file. Upload this to S3 (in a folder corresponding to the environment you are migrating). And then follow
amazon's instructions for [importing data into dynamo](http://docs.aws.amazon.com/datapipeline/latest/DeveloperGuide/dp-importexport-ddb-part1.html)
 
When you do the import you probably want to increase the write capacity on your dynamo table so that your job runs in a sensible timeframe (also set
the throughput %age for the import job higher than the 20% it defaults to). If your job is going to run for some time you may need to update timeout
set in the amazon job template, to do this open the job in the architect view and tinker with the settings (you can also increase the number of task
runners, box sizes etc here.)

Refreshing from PROD
====================

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