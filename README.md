# views.reagent

[Reagent][1] plugin for the [views][2] library, providing real-time component updates to 
server-side changes to data.

[1]: https://github.com/reagent-project/reagent
[2]: https://github.com/gered/views


## Usage

This library is made up of two core parts:

* The actual library, views.reagent, providing core functionality.
* A client/server communications plugin library which provides the glue code between whatever 
  underlying client/server library you're using (most likely [Sente][3]) and views.reagent.

[3]: https://github.com/ptaoussanis/sente

To use views.reagent in your application, you need to add both the main library and one 
client/server communications plugin library as dependencies. See their respective pages linked 
to below for more information on doing this.


### Main Library Documentation

[See here for full documentation.][4]

[4]: https://github.com/gered/views.reagent/tree/master/views.reagent


### Client/Server Plugin Documentation

* **[views.reagent.sente][5]** provides fairly low-level integration with Sente.

[5]: https://github.com/gered/views.reagent/tree/master/views.reagent.sente

If you're intent on using something else, you'll need to write your own client/server plugin 
library. Previously I provided a BrowserChannel plugin in addition to the Sente plugin library, 
but BrowserChannel is now pretty ancient and unnecessary since modern browsers universally support
Websockets, so it was removed in favour of using Sente.


### Examples

There are two example applications for you to look at to see a fully working web application with
working views system configured and working.

* [Todo MVC][6]. This is a copy of the original Reagent "Todo MVC" example app, but re-worked to
   use a SQL database and the views system.
* [Class Registry][7]. This is a somewhat more complex application with a busy UI showing a bunch
  of data at once, but it does serve to show how a UI can be built from multiple different views 
  at once.

[6]: https://github.com/gered/views.reagent/tree/master/examples/todomvc
[7]: https://github.com/gered/views.reagent/tree/master/examples/class-registry


### Notes

views.reagent uses a separate plugin architecture for client/server communication mainly
because I've observed that there are at times a variety of different ways in which people
like to integrate client/server communications in their applications. I wanted to avoid
(as much as possible) doing anything that would require any specific way of doing this
kind of integration.

The client/server glue code provided by these libraries is incredibly light so if they
do not meet your needs for whatever reason you should find it easy to create one yourself.


## License

Copyright © 2022 Gered King

Distributed under the the MIT License. See LICENSE for more details.
