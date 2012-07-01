(ns addressbook.core
  (:use [compojure.core]
        [ring.adapter.jetty]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.json-params]
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

(defroutes main-routes
  (GET "/" [] (slurp (io/file "resources/public/index.html")))
  (GET "/contact/:id" [id]
       (let [result (get-contact! id)]
         {:headers {"Content-Type" "application/json"}
          :body (json/json-str result)
          :status (status-helper result {:success 200 :failure 404})}))
  (GET "/contact/:id/json" [id]
       (let [result (get-contact! id)]
         {:headers {"Content-Type" "application/json"}
          :body (json/json-str result)
          :status (status-helper result {:success 200 :failure 404})}))
  (GET "/contact/:id/vcard" [id]
       (let [result (get-contact! id)]
         {:headers {"Content-Type" "text/vcard"}
          :body (vcard result)
          :status (status-helper result {:success 200 :failure 404})}))
  (PUT "/contact/:id" request
       (let [result (update-contact! (:id (:params request))
                                     (request :json-params))]
         {:headers {"Content-Type" "application/json"}
          :body (json/json-str result)
          :status (status-helper result {:success 200 :failure 412})}))
  (POST "/contact" request
        (let [result (add-contact! (request :json-params))]
          {:headers {"Content-Type" "application/json"}
           :body (json/json-str result)
           :status (status-helper result {:success 201 :failure 400})}))
  (route/resources "/")
  (route/not-found "<h1>Error</h1>"))

(def app
  (handler/site (wrap-json-params main-routes)))

(defn -main [& args]
	(let [port (read-string (get (System/getenv) "PORT" "8080"))]
    (run-jetty app {:port port})))