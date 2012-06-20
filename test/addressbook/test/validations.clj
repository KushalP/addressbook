(ns addressbook.test.validations
  (:use [addressbook.data]
        [addressbook.validations]
        [addressbook.test.fixtures]
        [clojure.test]
        [validateur.validation]))

(testing "validations check map holds valid data"
  (testing "is-vector-of function"
    (let [v (validation-set (is-vector-of map? :a))]
      (deftest bad-inputs-do-not-validate
        (is (= false (valid? v {:a nil})))
        (is (= false (valid? v {:a ""})))
        (is (= false (valid? v {:a 42})))
        (is (= false (valid? v {:a '()})))
        (is (= false (valid? v {:a #{}})))
        (is (= false (valid? v {:a #""})))
        (is (= false (valid? v {:a [nil nil nil]})))
        (is (= false (valid? v {:a [#{"b" "o"} #{"r" "k"}]})))
        (is (= false (valid? v {:a ['(1 2) '(3 4)]}))))
      (deftest bad-inputs-show-an-error-message
        (is (= {:a #{"must be a vector of maps"}} (v {:a nil}))))
      (deftest vectors-of-maps-will-pass
        (is (= true (valid? v {:a []})))
        (is (= {} (v {:a []})))
        (is (= {} (v {:a [{:t 1000} {:company "cyberdyne"}]})))
        (is (= true (valid? v {:a [{:t 1000} {:company "cyberdyne"}]}))))))

  (testing "valid-tel?"
    (deftest bad-inputs-fail-to-pass
      (is (= false (valid-tel? {:bleh 2 :bloop 3})))
      (is (= false (valid-tel? [1 2 3]))))
    (deftest valid-telephones-pass
      (is (= true (valid-tel? {:type #{"home" "voice"}
                               :value "+1-404-555-1212"})))))

  (testing "valid-address?"
    (deftest bad-inputs-fail-to-pass
      (is (= false (valid-address? {:bleh 2 :bloop 3})))
      (is (= false (valid-address? [1 2 3])))))

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
               (record-validations base-record)))))))
