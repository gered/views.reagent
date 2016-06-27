# views.reagent Example - Class Registry

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

Definitely take a look at the [Todo MVC][1] example app before diving into
this and also be sure you're familiar with Reagent.

[1]: https://github.com/gered/views.reagent/tree/master/examples/todomvc

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
