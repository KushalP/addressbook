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
                    (:_id (add-contact! test-record)))))]
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
               (add-contact! {:test "bob"}))))
      (deftest good-data-is-added-to-the-database
        (is (= {:message "contact created"}
               (dissoc (add-contact! (assoc-in test-record
                                               [:formatted-name]
                                               "Duh"))
                       :id)))))
    (testing "update-contact"
      (deftest cannot-update-non-existant-objectid
        (is (= {:error {:message "contact with id: 'bleh' not found"}}
               (update-contact! "bleh" test-record))))
      (deftest cannot-update-if-given-empty-values-to-update-with
        (is (= {:error {:message "You must provide an id and the values to update"}}
               (update-contact! nil nil)))
        (is (= {:error {:message "You must provide an id and the values to update"}}
               (update-contact! id {}))))
      (deftest update-existing-record-with-values
        (let [result (add-contact! (assoc-in test-record [:formatted-name]
                                             "TNGHT"))
              local-id (:id result)
              record (get-contact local-id)
              response (dosync
                        (update-contact! local-id {:email "joe@bloggs.com"})
                        (get-contact local-id))]
          (is (= "TNGHT" (:formatted-name record)))
          (is (= "forrestgump@example.com" (:email record)))
          (is (= local-id (.toStringMongod (:_id response))))
          (is (= "joe@bloggs.com" (:email response)))))
      (deftest do-not-update-if-params-are-not-allowed
        (let [result (add-contact! (assoc-in test-record [:formatted-name]
                                             "TNGHT"))
              local-id (:id result)
              record (get-contact local-id)
              response (update-contact! local-id {:bleh "bloop"})]
          (is (= nil (:bleh response)))
          (is (= "TNGHT" (:formatted-name record)))
          (is (= {:error {:message "You cannot update those keys. See 'allowed-keys' for a list of updatable keys.",
                          :allowed-keys '(:name :formatted-name :org :title :photo :tel :address :email)}}
                 response))
          (is (= nil (:bleh response)))))
      (deftest update-record-if-id-exists-and-params-allowed
        (let [result (add-contact! (assoc-in test-record [:formatted-name]
                                             "TNGHT"))
              local-id (:id result)
              record (get-contact local-id)
              response (update-contact! local-id {:photo "http://meh.com"})]
          (is (= {:success true}
                 response)))))
    (testing "flatten-vector-of-maps"
      (let [vec-of-maps [{:type "work,voice"
                          :value "+1-111-555-1212"}
                         {:type "home,voice"
                          :value "+1-404-555-1212"}]]
        (deftest should-produce-nil-for-nil-values
          (is (= nil (flatten-vector-of-maps {})))
          (is (= nil (flatten-vector-of-maps [])))
          (is (= nil (flatten-vector-of-maps '())))
          (is (= nil (flatten-vector-of-maps nil))))
        (deftest should-produce-nil-for-non-vector-structures
          (is (= nil (flatten-vector-of-maps [1 2 3])))
          (is (= nil (flatten-vector-of-maps 3)))
          (is (= nil (flatten-vector-of-maps "bleh")))
          (is (= nil (flatten-vector-of-maps '(:a :b :c)))))
        (deftest should-flatten-vector-of-maps-into-vector
          (is (= true (vector? (flatten-vector-of-maps vec-of-maps))))
          (is (= ["work,voice" "+1-111-555-1212"
                  "home,voice" "+1-404-555-1212"]
                 (flatten-vector-of-maps vec-of-maps))))))
    (testing "flatten-contact-values"
      (deftest should-produce-nil-for-nil-values
        (is (= nil (flatten-contact-values {})))
        (is (= nil (flatten-contact-values [])))
        (is (= nil (flatten-contact-values '())))
        (is (= nil (flatten-contact-values nil))))
      (deftest should-produce-nil-for-non-map-structures
        (is (= nil (flatten-contact-values [1 2 3])))
        (is (= nil (flatten-contact-values 3)))
        (is (= nil (flatten-contact-values "bleh")))
        (is (= nil (flatten-contact-values '(:a :b :c)))))
      (deftest should-flatten-map-into-set
        (is (= true (set? (flatten-contact-values test-record))))
        (is (= #{"home,voice" "United States of America" "Shrimp Man" "forrestgump@example.com" "Baytown" "Bubba Gump Shrimp Co." "20080424T195243Z" "work" "Gump" "42 Plantation St.\nBaytown, LA 30314\nUnited States of America" "Forrest Gump" "LA" "http://www.example.com/dir_photos/my_photo.gif" "+1-404-555-1212" "work,voice" "42 Plantation St." "30314" "+1-111-555-1212" "Forrest"}
               (flatten-contact-values test-record)))))))
