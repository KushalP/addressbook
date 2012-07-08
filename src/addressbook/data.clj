(ns addressbook.data
  (:use [addressbook.validations]
        [validateur.validation]
        [monger.conversion :only [from-db-object]])
  (:require [clojure.data.json :as json]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [monger.core :as mg]
            [monger.collection :as mc])
  (:import [org.bson.types ObjectId]))

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

(mg/connect!)
(mg/set-db! (mg/get-db "contacts"))

(defn contains-valid-keys?
  "Checks whether a given contact map contains all of the required keys"
  [contact-map]
  (reduce 'and (map #(contains? base-record %)
                    (keys (walk/keywordize-keys contact-map)))))

(defn flatten-vector-of-maps
  "Given a vector of maps, strips out the values from the map producing just a vector of values"
  [vector]
  (when-not (or (nil? vector)
                (not (vector? vector)))
    (when-not (or (empty? vector)
                  (not (every? map? vector)))
      (-> (map vals vector)
          (flatten)
          (vec)))))

(defn flatten-contact-values
  "Given a contact map, strips out all values (even nested) and returns a set of the values"
  [contact-map]
  (when-not (or (nil? contact-map)
                (not (map? contact-map)))
    (when-not (empty? contact-map)
      (let [name      (:name contact-map)
            telephone (:tel contact-map)
            address   (:address contact-map)]
        (set (map str/lower-case
                  (map str/trim
                       (flatten
                        (map #(str/split % #"\n|,")
                             (-> (filter #(not (empty? %))
                                         (-> contact-map
                                             (assoc :name (vals name))
                                             (assoc :tel (flatten-vector-of-maps telephone))
                                             (assoc :address (flatten-vector-of-maps address))
                                             (vals)
                                             (flatten)))
                                 (flatten)
                                 (set)))))))))))

(defn get-contact
  "Given an ObjectId hash string, finds the corresponding contact map (if any) stored in the database"
  [id]
  (let [error-msg {:error {:message (str "contact with id: '" id "' not found")}}]
    (try
      (let [result (from-db-object (mc/find-by-id "contacts"
                                                  (ObjectId. id))
                                   true)]
        (if (nil? result)
          error-msg
          (dissoc result :_keywords)))
      (catch IllegalArgumentException e error-msg))))

(defn add-contact!
  "Adds a given contact map to the database"
  [raw-data]
  (let [data (walk/keywordize-keys raw-data)]
    (if (valid? record-validations data)
      (let [id (ObjectId.)
            formed-data (-> data
                            (assoc :_keywords (flatten-contact-values data))
                            (assoc :_id id))
            result (mc/insert "contacts" formed-data)]
        (if (nil? result)
          {:message "something went wrong"
           :errors ["internal server error"]}
          {:message "contact created"
           :id (.toStringMongod id)}))
      {:message "You have provided badly formatted data"
       :errors (vec (record-validations data))})))

(defn search-contacts
  "Given a set of strings to search with, find the set of contacts it best matches"
  [search-terms]
  (let [error-msg {:error {:message "You must provide a set of search terms"}}]
    (if (or (nil? search-terms)
            (not (set? search-terms))
            (not (every? string? search-terms)))
      error-msg
      (let []
        nil))))

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
            (let [merged-value (merge original values)
                  new-form (assoc merged-value :_keywords
                                  (flatten-contact-values
                                   (dissoc merged-value :_id)))]
              (mc/update-by-id "contacts" (ObjectId. id) new-form)
              {:success true})))
        error-params-not-allowed))))
