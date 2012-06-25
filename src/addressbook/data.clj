(ns addressbook.data
  (:use [addressbook.validations]
        [somnium.congomongo]
        [validateur.validation])
  (:require [clojure.data.json :as json]
            [clojure.walk :as walk]))

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

(set-connection! conn)

(defn get-contact
  [id]
  (let [error-msg {:error {:message (str "contact with id: '" id "' not found")}}]
    (try
      (let [result (fetch-one :contacts :where {:_id (object-id id)})]
        (if (nil? result)
          error-msg
          result))
      (catch IllegalArgumentException e error-msg))))

(defn add-contact
  [raw-data]
  (let [data (walk/keywordize-keys raw-data)]
    (if (valid? record-validations data)
      (let [result (insert! :contacts data)]
        (if (nil? result)
          {:message "something went wrong"
           :errors ["internal server error"]}
          {:message "contact created"
           :id (.toStringMongod (result :_id))}))
      {:message "You have provided badly formatted data"
       :errors (vec (record-validations data))})))

(defn update-contact
  [id values]
  (if-not (and (not (and (nil? id)
                         (nil? values)))
               (not (empty? values)))
    {:error {:message "You must provide an id and the values to update"}}
    (let [original (get-contact id)]
      (if (contains? original :error)
        original
        (update! :contacts original (merge original values))))))
