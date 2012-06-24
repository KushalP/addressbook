(ns addressbook.test.data
  (:use [addressbook.data]
        [addressbook.test.fixtures]
        [clojure.test]
        [somnium.congomongo]))

(testing "database interactions"
  (let [id (let [record (fetch-one :contacts
                                   :where {:formatted-name "Forrest Gump"})]
             (str (if (not (nil? record))
                    (:_id record)
                    (:_id (add-contact test-record)))))]
    (testing "get-contact"
      (deftest bad-id-produces-error-response
        (is (= {:error {:message "contact with id: 'bleh' not found"}}
               (get-contact "bleh")))
        (is (= {:error {:message "contact with id: '1faa22b333333cc44dd55555' not found"}}
               (get-contact "1faa22b333333cc44dd55555"))))
      (deftest existing-id-produces-record-map
        (is (= test-record
               (dissoc (get-contact id) :_id)))))))
