(ns addressbook.data
  (:use somnium.congomongo)
  (:require [clojure.data.json :as json]))

;; These two blocks are adding the ability to convert ObjectId
;; objects into their equivalent string form. This means they
;; can be passed around easier.
(defn- write-json-mongodb-objectid [x out escape-unicode?]
  (json/write-json (str x) out escape-unicode?))

(extend org.bson.types.ObjectId json/Write-JSON
        {:write-json write-json-mongodb-objectid})

(def base-record
  "The base map structure we'll conj against for key consistency"
  {:name {:family ""
          :given ""
          :additional ""
          :prefix ""
          :suffix ""}
   :formatted-name ""
   :org ""
   :title ""
   :photo ""
   :tel []
   :address []
   :email ""})

(def conn
  (make-connection "contacts"
                   :host "127.0.0.1"
                   :port 27017))

(defn get-contact
  [id]
  (fetch-one :contacts
             :where {:_id (object-id id)}))

(defn add-contact
  [data]
  (insert! :contacts data))
