# views.reagent Example - Todo MVC

This is a modification of the Todo MVC app for Reagent [demonstrated here][1].
This version of the app has been modified to use a PostgreSQL database
to store the Todos and to provide realtime synchronization of changes 
to that data to any number of users currently viewing the app.

[1]: http://reagent-project.github.io/

## Running

### Creating the Database

This example app uses a PostgreSQL database. The SQL script to create
it is in `create_db.sql`. You can easily pipe it into `psql` at a 
command line to create it quickly, for example:

    $ psql < create_db.sql

(Of course, add any username/host parameters you might need)

### Starting It Up

To build everything and run in one step:

    $ lein rundemo
    
Then open up a web browser or two and head to http://localhost:8080/
to see the web app in action.

If you want to run this application in a REPL, just be sure to build
the ClojureScript:

    $ lein cljsbuild once

And then in the REPL you can just run:

    (-main)

to start the web app (you should be put in the correct namespace 
immediately).
