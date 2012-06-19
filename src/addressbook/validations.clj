(ns addressbook.validations
  (:use validateur.validation))

(def email-regex
  #"(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}\b")

(def record-validations
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
   (format-of :email :format email-regex :allow-blank true)))
