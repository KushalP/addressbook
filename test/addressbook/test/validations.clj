(ns addressbook.test.validations
  (:use [addressbook.validations]
        [clojure.test]
        [mississippi.core]))

(testing "validations check map holds valid vCard data"
  (let [test-record {:name {:family "Gump"
                            :given "Forrest"
                            :additional nil
                            :prefix nil
                            :suffix nil}
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
    (deftest requires-formatted-name
      (is (= false (valid? (validate (dissoc test-record :formatted-name) vcard-validations))))
      (is (= {:formatted-name '("required")} (:errors (validate (dissoc test-record :formatted-name) vcard-validations))))
      (is (= true (valid? (validate test-record vcard-validations)))))
    (deftest email-should-be-properly-formatted
      (is (= false (valid? (validate (update-in test-record [:email] (fn [x] "bleh")) vcard-validations))))
      (is (= {:email '("invalid email address")} (:errors (validate (dissoc test-record :email) vcard-validations))))
      (is (= {:email '("invalid email address")} (:errors (validate (update-in test-record [:email] (fn [x] "bleh")) vcard-validations))))
      (is (= true (valid? (validate test-record vcard-validations)))))))
