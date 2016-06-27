# views.reagent

This is the main library for the [views.reagent project][1] which 
provides the core functionality that most of your application code will
make use of.

Familiarity with the [views][2] library is *absolutely crucial* to 
understanding and usage of views.reagent.

[1]: https://github.com/gered/views.reagent
[2]: https://github.com/gered/views

## Leiningen

```clj
[gered/views.reagent "0.1"]
```

## Usage

Much of this documentation will be referring to the 
[Todo MVC example project][3] which uses 
[Sente client/server messaging][4] and [SQL views][5].

[3]: https://github.com/gered/views.reagent/tree/master/examples/todomvc
[4]: https://github.com/gered/views.reagent/tree/master/views.reagent.sente
[5]: https://github.com/gered/views.sql

Usage of this library is incredibly simple once you have a working 
views system up and running.

Initialization of the views system is typically either done directly 
via `views.core/init!` with some special configuration for whatever 
client/server messaging plugin you're using, or you may be required to
call a special "init" function provided by the plugin library to use 
instead of `views.core/init!`.

The Todo MVC example uses the following (server-side) view system:

```clj
(require '[views.sql.view :refer [view]])

(defonce view-system ... )

(defn todos-list []
  ["SELECT id, title, done FROM todos ORDER BY title"])

(def views
  [(view :todos db #'todos-list)])
```

A single view named `:todos` which simply returns a list of all Todos
in the database. The view takes no parameters.

Over on the ClojureScript side of things, once a connection has been
established to the server by the client/server messaging library, you
are ready to start using **view cursors** in your Reagent components.

### View Cursors

A **view cursor** is simply a Reagent cursor that represents the 
underlying view data received from the views library when the view is 
subscribed to. We can create a subscription by simply creating a view 
cursor for the desired view and giving it any appropriate parameters. 
views.reagent will automatically determine if it's the first usage of 
the view cursor and if a subscription request needs to be sent to the 
server. When a view refresh is performed on the server for the view, 
views.reagent sends it to the client and the data is put into a 
location where it's available to the views cursor. Since view cursors 
are Reagent cursors, updating the data like this instantly causes 
components dereferencing the cursor to rerender themselves. When 
components are unmounted views.reagent will automatically unsubscribe 
from views as appropriate. As well, when parameters passed in to view 
cursors change, view re-subscriptions are automatically handled to make
sure the view cursor is always up to date with the current client 
state.

So, how do we create a view cursor?

```clj
(ns webapp.client
  (:require
    [views.reagent.client.component :as vc :refer [view-cursor] :refer-macros [defvc]]))

(defvc my-todos-list []
  [:ul
    (map
      (fn [{:keys [id title done]}]
        ^{:key id}
        [:li {:class (if done "completed")} title])
      @(view-cursor :todos))])
```

Given the previously set up view system on the server, this is all the
UI code necessary to subscribe to the `:todos` view and retrieve and 
render the Todos list on the client. Whenever the `:todos` view is
refreshed on the server, the client will receive the data automatically
and the component will rerender since it is dereferencing the views
cursor. Finally, the client will automatically unsubscribe from the 
`:todos` view when the `my-todos-list` component is unmounted.

At first glance, `my-todos-list` looks just like any other normal 
Reagent component. However a very important difference is the use of 
`defvc` instead of `defn`.

`defvc` creates a Reagent **view component** which hooks into some 
React component lifecycle events to automatically handle view 
subscriptions/unsubscriptions for us based on how we use `view-cursor`
inside the component. You ***must*** use `defvc` for all Reagent 
components within which you want to use `view-cursor`.

`view-cursor` returns a Reagent cursor containing the actual view data
(in this case, a simple list of Todos). It's important to note that at
first the view data returned will be `nil` since obviously the client 
must first wait for the subscription to be processed by the server and
then for the server to send back the initial view data. Once this 
happens the component will automatically rerender as you would expect 
(showing the list of todos).

You can check if the view cursor is still waiting on the initial set of
data through the use of the `loading?` function. This can be used to 
render some kind of "loading" message or something similar if you'd 
prefer not to render empty data when components first load.

```clj
(defvc my-todos-list []
  (let [todos (view-cursor :todos)]
    (if (vc/loading? todos)
      [:div "Please wait ..."]
      [:ul
        (map
          (fn [{:keys [id title done]}]
            ^{:key id}
            [:li {:class (if done "completed")} title])
          @todos)])))
```

Note that `loading?` should be passed the actual Reagent cursor that
`view-cursor` returns, not the dereferenced result.

Also remember that `loading?` only checks if the view cursor is waiting
on the **initial** view data. Once that first set of data is received, 
`loading?` will always return false. There is no current method in 
views.reagent for determining if a view refresh is pending, although 
this is typically somewhat of a less drastic UI change to the user so 
in practice it may be less of a concern.

#### View Parameters

Some of your views may take parameters. This is easily supported by 
views.reagent.

As an example, if our `:todos` view was updated to include a filter:

```clj
(defn todos-list [done?]
  ["SELECT id, title, done FROM todos WHERE done = ? ORDER BY title" done?])
```

We can include this parameter in our call to `view-cursor`:

```clj
(view-cursor :todos true)       ; only return completed todos
```

We could get a little bit more fancy and make it an optional parameter:

```clj
(defn todos-list [& [done?]]
  (if-not (nil? done?)
    ["SELECT id, title, done FROM todos WHERE done = ? ORDER BY title" done?]
    ["SELECT id, title, done FROM todos ORDER BY title"]))
```

Letting us do any of these on the client:

```clj
(view-cursor :todos)            ; return all todos
(view-cursor :todos true)       ; only completed
(view-cursor :todos false)      ; only un-completed
```

#### View Cursors Are Intended To Be Read-only

Even though a Reagent cursor allows you to update them as well as read
from them, updating a view cursor doesn't do anything. Nothing stops 
you from doing this, but updating a view cursor does not propagate 
changes to the server or anything like that. In addition, you will lose
any changes you make every time a view refresh is received as the data
gets blindly replaced.

It is recommended that you do not write code that updates a view cursor.

## Advanced Topics

Most people just looking to use views.reagent in their applications 
probably won't need to read anything in this section.

### Manually Managing View Subscriptions

For those applications with very specific/complex requirements, you can
manually subscribe and unsubscribe to views from your ClojureScript 
code using views.reagent as well as make use of view cursors outside of
Reagent components. I do not recommend this though.

```clj
(use 'views.reagent.client.core)

; subscribe to the :todos view (no parameters)
(subscribe! [{:view-id :todos :parameters []}])

; returns a Reagent cursor to the view's data. this is a "dumb" function and if no
; subscription exists it will simply return a cursor pointing at nil data forever
(->view-sig-cursor {:view-id :todos :parameters []})

; unsubscribe from the :todos view
(unsubscribe! [{:view-id :todos :parameters []}])
```

When using these low-level functions, you need to specify view 
signature maps (a.k.a. "view sigs") to refer to the views you want to 
use. Also note that unlike the server-side view signatures that you may
be familiar with from the views library, these client-side view
signatures never have a `:namespace` in them. Even if you include one,
it is disregarded by the server.

Also note that `subscribe!` and `unsubscribe!` both take a list of view
signatures, so you can subscribe and unsubscribe from multiple views at
once.

I do not recommend mixing use of these low-level functions and using 
`defvc` components. You should probably pick one or the other and stick
to it unless you really know what you're doing.

### Integration Points for Writing a Client/Server Messaging Plugin

If you would like to write your own client/server messaging plugin 
library to fit your own needs you can easily do so. There are a couple
integration points within the main views.reagent library, as well as 
some special configuration you will need to provide to the views system
that you need to be aware of.

#### View Subscriber Key

In the views library, a bunch of functions take a "subscriber key." 
This is an arbitrary value that uniquely identifies a subscriber. There
can of course be multiple views for each subscriber key. Said another 
way: someone (uniquely identified by the subscriber key) can be 
subscribed to multiple different views at the same time.

You will typically want to use the underlying client/server library's
"client/user connection ID" as the subscriber key. For Sente this is 
the "user id" or the "client id" (depending on your application), and 
for clj-browserchannel this is the BrowserChannel "session id."

#### View System Configuration

You need to provide a `:send-fn` function that can be provided in the 
options given to `views.core/init!`. This function is used by the views
library to send view refreshes to subscribers. For views.reagent, your
send-fn function should send a vector with 3 things in it: the keyword
`:views/refresh` followed by the `view-sig` and then `view-data`. For 
example:

```clj
(defn send-fn
  [subscriber-key [view-sig view-data]]
  (your-messaging-lib/send! subscriber-key [:views/refresh view-sig view-data]))
```

#### Server-side Handling

`views.reagent.server.core` has two main event handling functions that 
provide proper handling for client connection events:

* `on-close!` should be called when a client's connection is closed for
whatever reason. views.reagent will remove all of the client's 
subscriptions.
* `on-receive!` should be called and passed in the raw data from every
message received from the client. It will return `true` if 
views.reagent recognized and handled the message as a subscription or
unsubscription request. See `views.reagent.utils/relevant-event?` for 
how it recognizes relevant messages.

For both of these functions, `client-id` should be the subscriber key
from the views library, and `context` can be anything you wish. 
Typically you will want to use something like a Ring request map and/or
user profile data as the context. This context argument is what gets 
passed to `views.core/subscribe!` and `views.core/unsubscribe!`.

#### Client-side Handling

`views.reagent.client.core` also has two main event handling functions 
that provide proper handling for server connection events:

* `on-open!` should be called when a connection to the server is 
established (and re-established, if applicable). views.reagent will 
re-send subscription requests for any subscriptions that should exist 
(e.g. if the connection was lost and the application reconnected, 
resubscribe to the views that all current mounted components need).
* `on-receive!` should be called and passed in the raw data from every 
message received from the server. It will return `true` if 
views.reagent recognized and handled the message as a view refresh 
event. See `views.reagent.utils/relevant-event?` for how it recognizes
relevant messages.

You also need to provide a function to send messages to the server.
You can set this function by directly using `reset!` on the atom
`views.reagent.client/send-fn`. The function should take a single
argument which is the data to be sent.

You should take care to hook up all of these integration points before
the first Reagent component is rendered at page load.

## License

Copyright © 2016 Gered King

Distributed under the the MIT License. See LICENSE for more details.
