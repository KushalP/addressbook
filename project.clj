(defproject addressbook "1.0.0-SNAPSHOT"
  :description "A Restful web app which holds and retrieves contact information"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.json "0.1.2"]
                 [compojure "1.1.0"]
                 [congomongo "0.1.9"]
                 [de.ubercode.clostache/clostache "1.3.0"]
                 [mississippi "1.0.0"]]
  :dev-dependencies [[lein-ring "0.7.1"]
                     [ring-mock "0.1.2"]])