(ns addressbook.validations
  (:use validateur.validation))

(def email-regex
  #"(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}\b")

(defn is-vector-of
  "A valid validateur function to check if a given key in our map holds a vector, and each item in the vector adheres to the passed function"
  [check-fn attribute]
  (let [f (if (vector? attribute) get-in get)]
    (fn [m]
      (let [value  (f m attribute)
            errors (if (and (vector? value)
                            (every? check-fn value))
                     {}
                     {attribute #{"must be a valid vector"}})]
        [(empty? errors) errors]))))

(defn valid-tel?
  "Checks that a given map holds a valid telephone number structure"
  [m]
  (let [v (validation-set
           ;; check keys exist.
           (presence-of :type)
           (presence-of :value))]
    (valid? v m)))

(defn valid-address?
  "Checks that a given map holds a valid address structure"
  [m]
  (let [v (validation-set
           ;; check keys exist.
           (presence-of :type)
           (presence-of :label)
           (presence-of :street)
           (presence-of :locality)
           (presence-of :region)
           (presence-of :code)
           (presence-of :country))]
    (valid? v m)))

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
   (is-vector-of map? :address)
   (is-vector-of map? :tel)))
