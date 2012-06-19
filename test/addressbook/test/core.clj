(ns addressbook.test.core
  (:use [addressbook.core]
        [addressbook.data]
        [clojure.test]
        [ring.mock.request]
        [somnium.congomongo])
  (:require [clojure.data.json :as json]))

(testing "Restful endpoints for handlers"
  (let [test-record {:name {:family "Gump"
                            :given "Forrest"
                            :additional nil
                            :prefix nil
                            :suffix nil}
                     :formatted-name "Forrest Gump"
                     :org "Bubba Gump Shrimp Co."
                     :title "Shrimp Man"
                     :photo "http://www.example.com/dir_photos/my_photo.gif"
                     :tel [{:type "work,voice"
                            :value "+1-111-555-1212"}
                           {:type "home,voice"
                            :value "+1-404-555-1212"}]
                     :address [{:type "work"
                                :label "42 Plantation St.\nBaytown, LA 30314\nUnited States of America"
                                :street "42 Plantation St."
                                :locality "Baytown"
                                :region "LA"
                                :code "30314"
                                :country "United States of America"}]
                     :email "forrestgump@example.com"
                     :rev "20080424T195243Z"}
        test-conn (make-connection "contacts-development"
                                   :host "127.0.0.1"
                                   :port 27017)
        init-db (do (set-write-concern test-conn :safe)
                    (set-connection! test-conn)
                    (drop-coll! "contacts"))
        id (let [record (fetch-one :contacts
                                   :where {:formatted-name "Forrest Gump"})]
             (if (not (nil? record))
               (:_id record)
               (:_id (add-contact test-record))))]
    (deftest base-route-returns-docs
      (is (= 200 (:status (app (request :get "/")))))
      (is (= "<h1>TODO: Write docs</h1>" (:body (app (request :get "/"))))))

    (deftest contact-with-id-produces-json-response
      (is (= 200 (:status (app (request :get (str "/contact/" id))))))
      (is (= "{\"name\":{\"prefix\":null,\"family\":\"Gump\",\"suffix\":null,\"additional\":null,\"given\":\"Forrest\"},\"org\":\"Bubba Gump Shrimp Co.\",\"photo\":\"http:\\/\\/www.example.com\\/dir_photos\\/my_photo.gif\",\"title\":\"Shrimp Man\",\"email\":\"forrestgump@example.com\",\"rev\":\"20080424T195243Z\",\"address\":[{\"country\":\"United States of America\",\"locality\":\"Baytown\",\"code\":\"30314\",\"street\":\"42 Plantation St.\",\"type\":\"work\",\"region\":\"LA\",\"label\":\"42 Plantation St.\\nBaytown, LA 30314\\nUnited States of America\"}],\"formatted-name\":\"Forrest Gump\",\"tel\":[{\"type\":\"work,voice\",\"value\":\"+1-111-555-1212\"},{\"type\":\"home,voice\",\"value\":\"+1-404-555-1212\"}]}"
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

    (deftest contact-is-created-on-post
      (let [response (app (-> (request :post "/contact")
                              (header "content-type" "application/x-www-form-urlencoded")
                              (body (update-in test-record [:formatted-name] (fn [_] "Dummy")))))]
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
                                           :where {:formatted-name "Dummy"}))))))))
