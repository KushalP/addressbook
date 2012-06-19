(ns addressbook.validations
  (:use validateur.validation))

(def email-regex
  #"(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}\b")

(defn is-vector
  "A valid validateur function to check if a given key in our map holds a vector"
  [attribute]
  (let [f (if (vector? attribute) get-in get)]
    (fn [m]
      (let [value  (f m attribute)
            errors (if (vector? value)
                     {}
                     {attribute #{"must be a vector"}})]
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
   (is-vector :address)
   (is-vector :tel)))
