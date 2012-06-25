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
               (dissoc (get-contact id) :_id)))))
    (testing "add-contact"
      (deftest bad-data-produces-error-response
        (is (= {:message "You have provided badly formatted data",
                :errors [[[:name :prefix] #{"can't be blank"}]
                         [[:name :family] #{"can't be blank"}]
                         [:name #{"can't be blank"}]
                         [[:name :suffix] #{"can't be blank"}]
                         [:org #{"can't be blank"}]
                         [:photo #{"can't be blank"}]
                         [:title #{"can't be blank"}]
                         [:email #{"can't be blank"}]
                         [[:name :additional] #{"can't be blank"}]
                         [[:name :given] #{"can't be blank"}]
                         [:address #{"must be a valid vector" "can't be blank"}]
                         [:formatted-name #{"can't be blank"}]
                         [:tel #{"must be a valid vector" "can't be blank"}]]}
               (add-contact {:test "bob"}))))
      (deftest good-data-is-added-to-the-database
        (is (= {:message "contact created"}
               (dissoc (add-contact (assoc-in test-record
                                              [:formatted-name]
                                              "Duh"))
                       :id)))))
    (testing "update-contact"
      (deftest cannot-update-non-existant-objectid
        (is (= {:error {:message "contact with id: 'bleh' not found"}}
               (update-contact "bleh" test-record))))
      (deftest cannot-update-if-given-empty-values-to-update-with
        (is (= {:error {:message "You must provide an id and the values to update"}}
               (update-contact nil nil)))
        (is (= {:error {:message "You must provide an id and the values to update"}}
               (update-contact id {}))))
      (deftest update-existing-record-with-values
        (let [result (add-contact (assoc-in test-record [:formatted-name]
                                            "TNGHT"))
              local-id (:id result)
              record (get-contact local-id)
              response (dosync
                        (update-contact local-id {:email "joe@bloggs.com"})
                        (get-contact local-id))]
          (is (= "TNGHT" (:formatted-name record)))
          (is (= "forrestgump@example.com" (:email record)))
          (is (= local-id (.toStringMongod (:_id response))))
          (is (= "joe@bloggs.com" (:email response))))))))
