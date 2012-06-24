(ns addressbook.core
  (:use [compojure.core]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.json-params]
        [addressbook.data]
        [addressbook.format])
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [compojure.handler :as handler]
            [compojure.route :as route]))

(defn status-helper
  [body & [{success :success, failure :failure} codes] ]
  (if (contains? body :error)
    failure
    success))

(defroutes main-routes
  (GET "/" [] (slurp (io/file "resources/public/index.html")))
  (GET "/contact/:id" [id]
       (let [result (get-contact id)]
         {:headers {"Content-Type" "application/json"}
          :body (json/json-str result)
          :status (status-helper result {:success 200 :failure 404})}))
  (GET "/contact/:id/json" [id]
       (let [result (get-contact id)]
         {:headers {"Content-Type" "application/json"}
          :body (json/json-str result)
          :status (status-helper result {:success 200 :failure 404})}))
  (GET "/contact/:id/vcard" [id]
       (let [result (get-contact id)]
         {:headers {"Content-Type" "text/vcard"}
          :body (vcard result)
          :status (status-helper result {:success 200 :failure 404})}))
  (POST "/contact" request
        (let [result (add-contact (request :json-params))]
          {:headers {"Content-Type" "application/json"}
           :body (json/json-str result)
           :status (status-helper result {:success 201 :failure 400})}))
  (route/not-found "<h1>Error</h1>"))

(def app
  (handler/site (-> main-routes
                    wrap-json-params)))