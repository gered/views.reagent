# views.reagent

[Reagent][1] plugin for the [views][2] library, providing real-time component updates to server-side changes to data.

[1]: https://github.com/reagent-project/reagent
[2]: https://github.com/gered/views


## Usage

This library is made up of two core parts:

* The actual library, views.reagent, providing core functionality.
* A client/server communications plugin library which provides the glue code between whatever underlying client/server library you're using (e.g. [Sente][3] or [clj-browserchannel][4]) and views.reagent.

[3]: https://github.com/ptaoussanis/sente
[4]: https://github.com/gered/clj-browserchannel


### Main Library Documentation

[See here for full documentation.][5]

[5]: https://github.com/gered/views.reagent/tree/master/views.reagent


### Client/Server Plugin Documentation

* **[views.reagent.sente][6]** provides fairly low-level integration with Sente.
* **[views.reagent.browserchannel][7]** for using BrowserChannel for client/server communication.

[6]: https://github.com/gered/views.reagent/tree/master/views.reagent.sente
[7]: https://github.com/gered/views.reagent/tree/master/views.reagent.browserchannel


### Examples

There are two example applications for you to look at to see a fully working web application with working views system configured and working.

* Todo MVC. There are two versions of this that are both largely identical except that [one uses Sente][8] and the [other uses BrowserChannel][9].
* [Class Registry][10]. This is a somewhat more complex application with a busy UI showing a bunch of data at once, but it does serve to show how a UI can be built from multiple different views at once. This example app uses Sente.

[8]: https://github.com/gered/views.reagent/tree/master/examples/todomvc
[9]: https://github.com/gered/views.reagent/tree/master/examples/todomvc-browserchannel
[10]: https://github.com/gered/views.reagent/tree/master/examples/class-registry


### Notes

views.reagent uses a separate plugin architecture for client/server communication mainly
because I've observed that there are at times a variety of different ways in which people
like to integrate client/server communications in their applications. I wanted to avoid
(as much as possible) doing anything that would require any specific way of doing this
kind of integration.

As well, speaking for myself, I use my own custom helper library that wraps over Sente which
I like but did not want to force anyone else to use.

The client/server glue code provided by these libraries is incredibly light so if they
do not meet your needs for whatever reason you should find it easy to create one yourself.


## License

Copyright © 2016 Gered King

Distributed under the the MIT License. See LICENSE for more details.
