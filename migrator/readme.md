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