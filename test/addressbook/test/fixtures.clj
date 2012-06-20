(ns addressbook.test.fixtures)

(def test-record
  {:name {:family "Gump"
          :given "Forrest"
          :additional ""
          :prefix ""
          :suffix ""}
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