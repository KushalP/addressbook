(ns addressbook.core
  (:use [compojure.core]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.json-params]
        [addressbook.data]
        [addressbook.format])
  (:require [clojure.data.json :as json]
            [compojure.handler :as handler]
            [compojure.route :as route]))

(defn status-helper
  [body & [{success :success, failure :failure} codes] ]
  (if (contains? body :error)
    failure
    success))

(defroutes main-routes
  (GET "/" [] "<h1>TODO: Write docs</h1>")
  (GET "/contact/:id" [id]
       (let [result (get-contact id)
             body (-> result
                      (json/json-str))]
         {:headers {"Content-Type" "application/json"}
          :body body
          :status (status-helper result {:success 200 :failure 404})}))
  (GET "/contact/:id/json" [id]
       (let [result (get-contact id)
             body (-> result
                      (json/json-str))]
         {:headers {"Content-Type" "application/json"}
          :body body
          :status (status-helper result {:success 200 :failure 404})}))
  (GET "/contact/:id/vcard" [id]
       (let [result (get-contact id)
             body (-> result
                      (vcard))]
         {:headers {"Content-Type" "text/vcard"}
          :body body
          :status (status-helper result {:success 200 :failure 404})}))
  (POST "/contact" {params :params}
        (let [result (add-contact params)]
          {:headers {"Content-Type" "application/json"}
           :body (-> {:message "contact created"
                      :id (:_id result)}
                     json/json-str)
           :status (status-helper result {:success 201 :failure 400})}))
  (route/not-found "<h1>Error</h1>"))

(def app
  (handler/site (-> main-routes
                    wrap-params
                    wrap-json-params)))
