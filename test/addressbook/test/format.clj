(ns addressbook.test.format
  (:use [clojure.test]
        [addressbook.format]))

(deftest convert-internal-map-to-vcard-json
  (let [record {:name {:family "Gump"
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
                :rev "20080424T195243Z"}]
    (is (= (vcard record)
           "BEGIN:VCARD
VERSION:4.0
N:Gump;Forrest;;;
FN: Forrest Gump
ORG:Bubba Gump Shrimp Co.
TITLE:Shrimp Man
PHOTO:http://www.example.com/dir_photos/my_photo.gif
TEL;TYPE=\"work,voice\";VALUE=uri:tel:+1-111-555-1212
TEL;TYPE=\"home,voice\";VALUE=uri:tel:+1-404-555-1212
ADR;TYPE=work;LABEL=\"42 Plantation St.\nBaytown, LA 30314\nUnited States of America\"
 :;;42 Plantation St.;Baytown;LA;30314;United States of America
EMAIL:forrestgump@example.com
REV:20080424T195243Z
END:VCARD"))))