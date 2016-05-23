# Reagent Data Views Example - Todo MVC

This is a modification of the Todo MVC app for Reagent [demonstrated here][1].
This version of the app has been modified to use a PostgreSQL database
to store the Todos and to provide realtime synchronization of changes 
to that data to any number of users currently viewing the app.

[1]: http://reagent-project.github.io/

## Running

Since Reagent Data Views and the Views library it depends on are all
currently in somewhat of an experimental / pre-beta state right now,
you will need to first clone the following repositories and manually
install the libraries via `lein install`:

* [views](https://github.com/gered/views)
* [views-sql](https://github.com/gered/views-sql)
* [reagent-data-views](https://github.com/gered/reagent-data-views)

As well, you can install [views-honeysql](https://github.com/gered/views-honeysql)
if you want to try out using HoneySQL instead of SQL with views. But
this example app does not use it so it's not required.

Once these libraries are installed, you can simply build the ClojureScript:

    $ lein cljsbuild once

And then start up a REPL and run:

    (-main)

Or more simply, just do:

    $ lein run

Done either way, a new browser window should open to the app.

Alternatively, to build the ClojureScript and run the app all in one go:

    $ lein rundemo

Open up a second browser and make changes by adding or deleting a Todo,
or marking them as completed, etc. and see that the changes are
instantly propagated to all clients.
