(ns addressbook.core
  (:use [compojure.core]
        [ring.adapter.jetty]
        [ring.middleware.json]
        [ring.util.response :only (content-type response status)]
        [addressbook.data]
        [addressbook.format])
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [compojure.handler :as handler]
            [compojure.route :as route])
  (:gen-class))

(defn status-helper
  [body & [{success :success, failure :failure} codes] ]
  (if (or (contains? body :error)
          (contains? body :errors))
    failure
    success))

(defonce pages
  {:404 (slurp (io/file "resources/public/404.html"))
   :index (slurp (io/file "resources/public/index.html"))})

(defroutes main-routes
  (GET "/" [] (:index pages))
  (GET "/contact/:id" [id]
       (let [result (get-contact id)]
         (-> (response (json/json-str result))
             (content-type "application/json")
             (status (status-helper result {:success 200 :failure 404})))))
  (GET "/contact/:id/json" [id]
       (let [result (get-contact id)]
         (-> (response (json/json-str result))
             (content-type "application/json")
             (status (status-helper result {:success 200 :failure 404})))))
  (GET "/contact/:id/vcard" [id]
       (let [result (get-contact id)]
         (-> (response (vcard result))
             (content-type "text/vcard")
             (status (status-helper result {:success 200 :failure 404})))))
  (PUT "/contact/:id" request
       (let [result (update-contact! (:id (:params request))
                                     (request :json-params))]
         (-> (response (json/json-str result))
             (content-type "application/json")
             (status (status-helper result {:success 200 :failure 412})))))
  (POST "/contact" request
        (let [result (add-contact! (request :json-params))]
          (-> (response (json/json-str result))
             (content-type "application/json")
             (status (status-helper result {:success 201 :failure 400})))))
  (route/resources "/")
  (route/not-found (:404 pages)))

(def app
  (handler/site (-> main-routes
                    wrap-json-params
                    wrap-json-response)))

(defn -main [& args]
	(let [port (read-string (get (System/getenv) "PORT" "8080"))]
    (run-jetty app {:port port})))
