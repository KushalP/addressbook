# Address Book

A Restful web app which holds and retrieves contact information. It
can provide output formatted as either JSON or vCard.

[![Continuous Integration status](https://secure.travis-ci.org/KushalP/addressbook.png)](http://travis-ci.org/KushalP/addressbook)

## Development

To start developing with address book, you'll need to have the
following installed with the correct permissions:

1. MongoDB
2. Clojure
3. Leiningen

Then run the following within the project directory:

    lein deps
    lein test

### Development Server

To run a development server, run the following within the project
directory:

    lein ring server

### Deploying Address Book

You can deploy the address book app in one of two ways:

1. Create a war file and run it with a server container like Tomcat
2. Create a jar file and run it as a service

Here's how you would create the war file:

    lein ring uberwar

Here's how you would create the jar file:

    lein uberjar

## License

Copyright (C) 2012 Kushal Pisavadia

Distributed under the Eclipse Public License, the same as Clojure.
