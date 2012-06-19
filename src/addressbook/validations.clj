(ns addressbook.validations
  (:use mississippi.core))

(def record-validations
  {:formatted-name [(comp not nil?) :msg "required"]
   :email [(matches-email)]})