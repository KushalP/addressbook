(ns addressbook.test.data
  (:use [addressbook.data]
        [addressbook.test.fixtures]
        [clojure.test]
        [monger.conversion :only [from-db-object]])
  (:require [monger.collection :as mc])
  (:import [org.bson.types ObjectId]))

(testing "database interactions"
  (let [id (let [record (-> (mc/find-one "contacts"
                                         {:formatted-name "Forrest Gump"})
                            (from-db-object true))]
             (-> (if (not (nil? record))
                   record
                   (add-contact! test-record))
                 :_id
                 .toStringMongod))]
    (testing "get-contact"
      (deftest bad-id-produces-error-response
        (is (= {:error {:message "contact with id: 'bleh' not found"}}
               (get-contact "bleh")))
        (is (= {:error {:message "contact with id: '1faa22b333333cc44dd55555' not found"}}
               (get-contact "1faa22b333333cc44dd55555"))))
      (deftest existing-id-produces-record-map
        (is (= test-record
               (dissoc (get-contact id) :_id))))
      (deftest test-record-should-have-keywords
        (is (= (flatten-contact-values test-record)
               (-> (mc/find-one "contacts"
                                {:formatted-name "Forrest Gump"})
                   (from-db-object true)
                   :keywords
                   (set))))))
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
          (is (= "joe@bloggs.com" (:email response)))
          (is (= (-> (mc/find-one "contacts"
                                  {:_id (ObjectId. local-id)})
                     (from-db-object true)
                     :keywords
                     (set))
                 (flatten-contact-values (-> response
                                             (dissoc :_id)))))))
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
        (is (= #{"united states of america" "shrimp man" "forrestgump@example.com" "baytown" "bubba gump shrimp co." "20080424t195243z" "la 30314" "gump" "work" "voice" "forrest gump" "la" "http://www.example.com/dir_photos/my_photo.gif" "+1-404-555-1212" "30314" "42 plantation st." "forrest" "+1-111-555-1212" "home"}
               (flatten-contact-values test-record)))))
    (testing "search-contact"
      (deftest should-produce-error-response-for-non-set-of-strings
        (let [error-msg {:error {:message "You must provide a set of search terms"}}]
          (is (= error-msg (search-contacts nil)))
          (is (= error-msg (search-contacts {:d 3})))
          (is (= error-msg (search-contacts [1 2 3])))
          (is (= error-msg (search-contacts '("a" "b" "c"))))
          (is (= error-msg (search-contacts "bleh")))
          (is (= error-msg (search-contacts #{1 2 3 4})))
          (is (= error-msg (search-contacts #{:a :b :c :d})))))
      (deftest should-produce-nil-when-no-results-are-found
        (is (= nil (search-contacts #{"bleh" "bloop"})))))))
