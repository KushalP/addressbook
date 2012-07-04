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
  "Given an ObjectId hash string, finds the corresponding contact map (if any) stored in the database"
  [id]
  (let [error-msg {:error {:message (str "contact with id: '" id "' not found")}}]
    (try
      (let [result (fetch-one :contacts :where {:_id (object-id id)})]
        (if (nil? result)
          error-msg
          result))
      (catch IllegalArgumentException e error-msg))))

(defn add-contact!
  "Adds a given contact map to the database"
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

(defn contains-valid-keys?
  "Checks whether a given contact map contains all of the required keys"
  [x]
  (reduce 'and (map #(contains? base-record %)
                    (keys (walk/keywordize-keys x)))))

(defn update-contact!
  "Given an ObjectId hash string, and a map of values, updates the contact map with the provided ObjectId with the provided values"
  [id values]
  (let [allowed-keys (keys base-record)
        error-values-needed {:error {:message "You must provide an id and the values to update"}}
        error-params-not-allowed {:error {:message "You cannot update those keys. See 'allowed-keys' for a list of updatable keys."
                                          :allowed-keys allowed-keys}}]
    (if-not (and (not (and (nil? id)
                           (nil? values)))
                 (not (empty? values)))
      error-values-needed
      (if (contains-valid-keys? values)
        (let [original (get-contact! id)]
          (if (contains? original :error)
            original
            (dosync
             (update! :contacts original (merge original values))
             {:success true})))
        error-params-not-allowed))))
