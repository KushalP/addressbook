(ns addressbook.validations
  (:use mississippi.core))

(def vcard-validations
  {:formatted-name [(comp not nil?) :msg "required"]
   :email [(matches-email)]})