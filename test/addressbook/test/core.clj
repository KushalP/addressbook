(ns addressbook.test.core
  (:use [addressbook.core]
        [addressbook.data]
        [clojure.test]
        [ring.mock.request]
        [somnium.congomongo])
  (:require [clojure.data.json :as json]))

(def test-conn
  (make-connection "contacts-development"
                   :host "127.0.0.1"
                   :port 27017))

(set-write-concern test-conn :safe)
(set-connection! test-conn)
(drop-coll! "contacts")

(def test-record
  {:name {:family "Gump"
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
   :rev "20080424T195243Z"})

(def id
  (let [record (fetch-one :contacts
                          :where {:formatted-name "Forrest Gump"})]
    (if (not (nil? record))
      (:_id record)
      (:_id (add-contact test-record)))))

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