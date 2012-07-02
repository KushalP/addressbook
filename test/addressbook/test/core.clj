(ns addressbook.test.core
  (:use [addressbook.core]
        [addressbook.data]
        [addressbook.test.fixtures]
        [clojure.test]
        [ring.mock.request]
        [somnium.congomongo])
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]))

(testing "Restful endpoints for handlers"
  (let [test-conn (make-connection "contacts-development"
                                   :host "127.0.0.1"
                                   :port 27017)
        init-db (do (set-write-concern test-conn :safe)
                    (set-connection! test-conn)
                    (drop-coll! "contacts"))
        id (let [record (fetch-one :contacts
                                   :where {:formatted-name "Forrest Gump"})]
             (if (not (nil? record))
               (:_id record)
               (:id (add-contact! test-record))))]
    (testing "GET / (homepage)"
      (deftest base-route-returns-docs
        (is (= 200 (:status (app (request :get "/")))))
        (is (= (slurp (io/file "resources/public/index.html"))
               (:body (app (request :get "/")))))))
    (testing "GET (404 error)"
      (deftest not-available-urls-produce-404-page
        (is (= 404 (:status (app (request :get "/bleh")))))
        (is (= (slurp (io/file "resources/public/404.html"))
               (:body (app (request :get "/bleh")))))))
    (testing "GET /contact/:id"
      (deftest contact-with-id-produces-json-response
        (is (= 200 (:status (app (request :get (str "/contact/" id))))))
        (is (= "{\"name\":{\"prefix\":\"\",\"family\":\"Gump\",\"suffix\":\"\",\"additional\":\"\",\"given\":\"Forrest\"},\"org\":\"Bubba Gump Shrimp Co.\",\"photo\":\"http:\\/\\/www.example.com\\/dir_photos\\/my_photo.gif\",\"title\":\"Shrimp Man\",\"email\":\"forrestgump@example.com\",\"rev\":\"20080424T195243Z\",\"address\":[{\"country\":\"United States of America\",\"locality\":\"Baytown\",\"code\":\"30314\",\"street\":\"42 Plantation St.\",\"type\":\"work\",\"region\":\"LA\",\"label\":\"42 Plantation St.\\nBaytown, LA 30314\\nUnited States of America\"}],\"formatted-name\":\"Forrest Gump\",\"tel\":[{\"type\":\"work,voice\",\"value\":\"+1-111-555-1212\"},{\"type\":\"home,voice\",\"value\":\"+1-404-555-1212\"}]}"
               ;; remove the :_id as the response may have been created
               ;; asas part of the test.
               (-> (:body (app (request :get (str "/contact/" id))))
                   (json/read-json)
                   (dissoc :_id)
                   (json/json-str)))))
      (deftest contact-with-bad-id-produces-error-response
        (is (= 404 (:status (app (request :get "/contact/meh")))))
        (is (= "{\"error\":{\"message\":\"contact with id: 'meh' not found\"}}"
               (:body (app (request :get "/contact/meh"))))))
      (deftest contact-with-id-and-vcard-produces-vcard-response
        (is (= 200 (:status (app (request :get (str "/contact/" id "/vcard"))))))
        (is (= "BEGIN:VCARD\nVERSION:4.0\nN:Gump;Forrest;;;\nFN: Forrest Gump\nORG:Bubba Gump Shrimp Co.\nTITLE:Shrimp Man\nPHOTO:http://www.example.com/dir_photos/my_photo.gif\nTEL;TYPE=\"work,voice\";VALUE=uri:tel:+1-111-555-1212\nTEL;TYPE=\"home,voice\";VALUE=uri:tel:+1-404-555-1212\nADR;TYPE=work;LABEL=\"42 Plantation St.\nBaytown, LA 30314\nUnited States of America\"\n :;;42 Plantation St.;Baytown;LA;30314;United States of America\nEMAIL:forrestgump@example.com\nREV:20080424T195243Z\nEND:VCARD"
               (:body (app (request :get (str "/contact/" id "/vcard")))))))
      (deftest contact-with-non-existant-id-and-vcard-produces-empty-vcard
        (is (= 404 (:status (app (request :get "/contact/bloop/vcard")))))
        (is (= "BEGIN:VCARD\nVERSION:4.0\nN:;;;;\nFN: \nORG:\nTITLE:\nPHOTO:\nEMAIL:\nREV:\nEND:VCARD"
               (:body (app (request :get "/contact/bloop/vcard")))))))
    (testing "POST /contact"
      (deftest contact-is-not-created-on-invalid-post-data
        (let [response (app (-> (request :post "/contact")
                                (content-type "application/json")
                                (body (json/json-str {:test "dummy"}))))]
          (is (= {"Content-Type" "application/json"} (:headers response)))
          (is (= 400 (:status response)))
          (is (= {:message "You have provided badly formatted data",
                  :errors [[["name" "prefix"] ["can't be blank"]]
                           [["name" "family"] ["can't be blank"]]
                           ["name" ["can't be blank"]]
                           [["name" "suffix"] ["can't be blank"]]
                           ["org" ["can't be blank"]]
                           ["photo" ["can't be blank"]]
                           ["title" ["can't be blank"]]
                           ["email" ["can't be blank"]]
                           [["name" "additional"] ["can't be blank"]]
                           [["name" "given"] ["can't be blank"]]
                           ["address" ["must be a valid vector" "can't be blank"]]
                           ["formatted-name" ["can't be blank"]]
                           ["tel" ["must be a valid vector" "can't be blank"]]]}
                 (-> (:body response)
                     (json/read-json))))))
      (deftest contact-is-not-created-on-incomplete-post-data
        (let [response (app (-> (request :post "/contact")
                                (content-type "application/json")
                                (body (-> test-record
                                          (dissoc :tel)
                                          (dissoc :email)
                                          (json/json-str)))))]
          (is (= {"Content-Type" "application/json"} (:headers response)))
          (is (= 400 (:status response)))
          (is (= {:message "You have provided badly formatted data",
                  :errors [["email" ["can't be blank"]]
                           ["tel" ["must be a valid vector" "can't be blank"]]]}
                 (-> (:body response)
                     (json/read-json))))))
      (deftest contact-is-created-on-post
        (let [response (app (-> (request :post "/contact")
                                (content-type "application/json")
                                (body (json/json-str
                                       (assoc-in test-record
                                                 [:formatted-name]
                                                 "Dummy")))))]
          (is (= {"Content-Type" "application/json"} (:headers response)))
          ;; Drop the :id key as this is likely to be dynamically generated
          ;; for each test run.
          (is (= {:message "contact created"}
                 (-> (:body response)
                     (json/read-json)
                     (dissoc :id))))
          (is (= 201 (:status response)))
          (is (= "Dummy"
                 (:formatted-name (fetch-one :contacts
                                             :where {:formatted-name "Dummy"})))))))
    (testing "PUT /contact/:id"
      (deftest error-returned-if-keys-to-update-not-allowed
        (let [response (app (-> (request :put "/contact/bloop")
                                (content-type "application/json")
                                (body (json/json-str {:test "dummy"}))))]
          (is (= {:error {:message "You cannot update those keys. See 'allowed-keys' for a list of updatable keys.",
                          :allowed-keys ["name" "formatted-name" "org" "title" "photo" "tel" "address" "email"]}}
                 (json/read-json (:body response))))))
      (deftest error-returned-if-object-id-does-not-exist
        (let [response (app (-> (request :put (str "/contact/bleh"))
                                (content-type "application/json")
                                (body (json/json-str {:photo "http://meh.com"}))))]
          (is (= {:error {:message "contact with id: 'bleh' not found"}}
                 (json/read-json (:body response)))))))))
