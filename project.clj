(defproject addressbook "1.0.0-SNAPSHOT"
  :description "A Restful web app which holds and retrieves contact information"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.1.2"]
                 [compojure "1.1.3"]
                 [com.novemberain/monger "1.2.0"]
                 [com.novemberain/validateur "1.2.0"]
                 [de.ubercode.clostache/clostache "1.3.0"]
                 [ring/ring-jetty-adapter "1.1.5"]
                 [ring/ring-json "0.1.2"]]
  :profiles {:dev {:dependencies [[lein-ring "0.7.5"]
                                  [ring-mock "0.1.3"]]}}
  :main addressbook.core
  :ring {:handler addressbook.core/app}
  :test-selectors {:default (constantly true)})
