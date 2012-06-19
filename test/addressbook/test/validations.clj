(ns addressbook.test.validations
  (:use [addressbook.data]
        [addressbook.validations]
        [clojure.test]
        [validateur.validation]))

(testing "validations check map holds valid data"
  (let [test-record {:name {:family "Gump"
                            :given "Forrest"
                            :additional ""
                            :prefix ""
                            :suffix ""}
                     :formatted-name "Forrest Gump"
                     :org "Bubba Gump Shrimp Co."
                     :title "Shrimp Man"
                     :photo "http://www.example.com/dir_photos/my_photo.gif"
                     :tel [{:type #{"work" "voice"}
                            :value "+1-111-555-1212"}
                           {:type #{"home" "voice"}
                            :value "+1-404-555-1212"}]
                     :address [{:type "work"
                                :label "42 Plantation St.\nBaytown, LA 30314\nUnited States of America"
                                :street "42 Plantation St."
                                :locality "Baytown"
                                :region "LA"
                                :code "30314"
                                :country "United States of America"}]
                     :email "forrestgump@example.com"}]
    (testing "is-vector-of-maps function"
      (let [v (validation-set (is-vector-of-maps :tel))]
        (deftest bad-inputs-do-not-validate
          (is (= false (valid? v {:tel nil})))
          (is (= false (valid? v {:tel ""})))
          (is (= false (valid? v {:tel 42})))
          (is (= false (valid? v {:tel '()})))
          (is (= false (valid? v {:tel #{}})))
          (is (= false (valid? v {:tel #""})))
          (is (= false (valid? v {:tel [nil nil nil]})))
          (is (= false (valid? v {:tel [#{"b" "o"} #{"r" "k"}]})))
          (is (= false (valid? v {:tel ['(1 2) '(3 4)]}))))
        (deftest bad-inputs-show-an-error-message
          (is (= {:tel #{"must be a vector of maps"}} (v {:tel nil}))))
        (deftest vectors-of-maps-will-pass
          (is (= true (valid? v {:tel []})))
          (is (= {} (v {:tel []})))
          (is (= {} (v {:tel [{:t 1000} {:company "cyberdyne"}]})))
          (is (= true (valid? v {:tel [{:t 1000} {:company "cyberdyne"}]}))))))

    (testing "map structure"
      (testing "formatted-name"
        (deftest formatted-name-should-be-present
          (is (= false
                 (valid? record-validations
                         (dissoc test-record :formatted-name))))
          (is (= {:formatted-name #{"can't be blank"}}
                 (record-validations (dissoc test-record :formatted-name))))
          (is (= true
                 (valid? record-validations test-record)))))

      (testing "email"
        (deftest email-should-be-properly-formatted
          (is (= false
                 (valid? record-validations
                         (update-in test-record [:email] (fn [x] "bleh")))))
          (is (= false
                 (valid? record-validations
                         (update-in test-record [:email] (fn [x] nil)))))
          (is (= {:email #{"has incorrect format"}}
                 (record-validations
                  (update-in test-record [:email] (fn [x] "bleh")))))
          (is (= true
                 (valid? record-validations test-record))))
        (deftest email-key-should-exist
          (is (= {:email #{"can't be blank"}}
                 (record-validations (dissoc test-record :email)))))
        (deftest email-can-be-blank
          (is (= true
                 (valid? record-validations
                         (update-in test-record [:email] (fn [x] "")))))))

      (testing "base-record"
        (deftest should-have-minimally-viable-map-structure
          (is (= true
                 (valid? record-validations
                         base-record)))
          (is (= {}
                 (record-validations base-record))))))))
