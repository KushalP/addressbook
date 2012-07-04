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

(defn contains-valid-keys?
  "Checks whether a given contact map contains all of the required keys"
  [contact-map]
  (reduce 'and (map #(contains? base-record %)
                    (keys (walk/keywordize-keys contact-map)))))

(defn flatten-vector-of-maps
  "Given a vector of maps, strips out the values from the map producing just a vector of values"
  [vector]
  (if (or (nil? vector)
          (not (vector? vector)))
    nil
    (if (or (empty? vector)
             (not (every? map? vector)))
      nil
      (-> (map vals vector)
          (flatten)
          (vec)))))

(defn flatten-contact-values
  "Given a contact map, strips out all values (even nested) and returns a set of the values"
  [contact-map]
  (if (or (nil? contact-map)
          (not (map? contact-map)))
    nil
    (if (empty? contact-map)
      nil
      (let [name      (:name contact-map)
            telephone (:tel contact-map)
            address   (:address contact-map)]
        (set (filter #(not (empty? %))
                     (-> contact-map
                         (assoc :name (vals name))
                         (assoc :tel (flatten-vector-of-maps telephone))
                         (assoc :address (flatten-vector-of-maps address))
                         (vals)
                         (flatten))))))))

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
        (let [original (get-contact id)]
          (if (contains? original :error)
            original
            (dosync
             (update! :contacts original (merge original values))
             {:success true})))
        error-params-not-allowed))))
