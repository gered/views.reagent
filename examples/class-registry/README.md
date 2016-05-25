# Reagent Data Views Example - Class Registry

This is a "Class Registry" application that has a lot of CRUD operations
in it which allow users to manage students and professors, as well as
classes and then assign the students/professors to those classes. The
idea is _very_ loosely based off one of the Om tutorial applications
(the data used is almost identical).

Note that this example is somewhat complicated as there are several
lists of data shown in the UI, all of which are completely editable
to the user. While the code is longer as a result, this still serves
as a more interesting example with several views being used (some
using parameters).

Definitely take a look at the [TodoMVC][1] example app before diving into
this and also be sure you're familiar with Reagent.

[1]: https://github.com/gered/reagent-data-views/tree/master/examples/todomvc

## Running

### A quick note on the dependencies used

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
