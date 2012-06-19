# Address Book

A Restful web app which holds and retrieves contact information. It
can provide output formatted as either JSON or vCard.

## Development

To start developing with address book, you'll need to have MongoDB
installed and setup with the correct permissions. Then run the
following within the project directory:

    lein deps
    lein test

### Development Server

To run a development server, run the following within the project
directory:

    lein ring server

## License

Copyright (C) 2012 Kushal Pisavadia

Distributed under the Eclipse Public License, the same as Clojure.
