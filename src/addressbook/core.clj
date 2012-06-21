(ns addressbook.core
  (:use [compojure.core]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.json-params]
        [addressbook.data]
        [addressbook.format])
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
  (GET "/contact/:id/vcard" [id]
       (let [result (get-contact-wrapper id)
             body (-> result
                      (vcard))
             status (status-helper result)]
         {:headers {"Content-Type" "text/vcard"}
          :body body
          :status status}))
  (POST "/contact" {params :params}
        (let [result (add-contact params)]
          {:headers {"Content-Type" "application/json"}
           :body (-> {:message "contact created"
                      :id (:_id result)}
                     json/json-str)
           :status 201}))
  (route/not-found "<h1>Error</h1>"))

(def app
  (handler/site (-> main-routes
                    wrap-params
                    wrap-json-params)))
