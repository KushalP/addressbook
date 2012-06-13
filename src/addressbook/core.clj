(ns addressbook.core
  (:use [compojure.core]
        [ring.middleware.params :only [wrap-params]]
        [addressbook.data])
  (:require [clojure.data.json :as json]
            [compojure.handler :as handler]
            [compojure.route :as route]))

(defn get-contact-wrapper
  [id]
  (try
    (get-contact id)
    (catch IllegalArgumentException e
      {:error {:message (str "contact with id: '" id "' not found")}})))

(defn status-helper
  [body]
  (if (contains? body :error)
    404
    200))

(defroutes main-routes
  (GET "/" [] "<h1>TODO: Write docs</h1>")
  (GET "/contact/:id" [id]
       (let [result (get-contact-wrapper id)
             body (-> result
                      (json/json-str))
             status (status-helper result)]
         {:headers {"Content-Type" "application/json"}
          :body body
          :status status}))
  (GET "/contact/:id/json" [id]
       (let [result (get-contact-wrapper id)
             body (-> result
                      (json/json-str))
             status (status-helper result)]
         {:headers {"Content-Type" "application/json"}
          :body body
          :status status}))
  (route/not-found "<h1>Error</h1>"))

(def app
  (handler/site (-> main-routes
                    wrap-params)))
