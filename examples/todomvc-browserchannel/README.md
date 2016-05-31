# views.reagent Example - Todo MVC (BrowserChannel)

This is a modification of the Todo MVC app for Reagent [demonstrated here][1].
This version of the app has been modified to use a PostgreSQL database
to store the Todos and to provide realtime synchronization of changes 
to that data to any number of users currently viewing the app.

[1]: http://reagent-project.github.io/

> **NOTE:** This is a copy of the [other Todo MVC example][2] and is the same
> in every respect, except that this one is using [BrowserChannel][3] instead of
> Sente as the underlying client/server messaging implementation.

[2]: https://github.com/gered/views.reagent/tree/master/examples/todomvc
[3]: https://github.com/gered/views.reagent/tree/master/views.reagent.browserchannel

## Running

### A quick note on the dependencies used

Since views.reagent and the Views library it depends on are all
currently in somewhat of an experimental / pre-beta state right now,
you will need to first clone the following repositories and manually
install the libraries via `lein install`:

* [views](https://github.com/gered/views)
* [views.sql](https://github.com/gered/views.sql)
* [views.reagent](https://github.com/gered/views.reagent)

As well, you can install [views.honeysql](https://github.com/gered/views.honeysql)
if you want to try out using HoneySQL instead of SQL with views. But
this example app does not use it so it's not required.

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
