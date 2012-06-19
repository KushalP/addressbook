(ns addressbook.validations
  (:use validateur.validation))

(def email-regex
  #"(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}\b")

(defn is-vector-of-maps
  "A valid validateur function to check if a given key in our map holds a vector of maps"
  [attribute]
  (let [f (if (vector? attribute) get-in get)]
    (fn [m]
      (let [value  (f m attribute)
            errors (if (and (vector? value)
                            (every? map? value))
                     {}
                     {attribute #{"must be a vector of maps"}})]
        [(empty? errors) errors]))))

(def record-validations
  "Validation set for the map form of a vCard data structure"
  (validation-set
   ;; check keys exist.
   (presence-of :name)
   (presence-of [:name :family])
   (presence-of [:name :given])
   (presence-of [:name :additional])
   (presence-of [:name :prefix])
   (presence-of [:name :suffix])
   (presence-of :formatted-name)
   (presence-of :org)
   (presence-of :title)
   (presence-of :photo)
   (presence-of :tel)
   (presence-of :address)
   (presence-of :email)

   ;; check values are formatted correctly.
   (format-of :email :format email-regex :allow-blank true)
   (is-vector-of-maps :address)
   (is-vector-of-maps :tel)))
