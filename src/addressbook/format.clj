(ns addressbook.format
  (:use clostache.parser))

(defn vcard
  "Converts the intermal map representation into a format meeting the vCard 4.0 specification"
  [record]
  (render "BEGIN:VCARD
VERSION:4.0
N:{{name.family}};{{name.given}};{{name.additional}};{{name.prefix}};{{name.suffix}}
FN: {{formatted-name}}
ORG:{{org}}
TITLE:{{title}}
PHOTO:{{photo}}
{{#tel}}
TEL;TYPE=\"{{&type}}\";VALUE=uri:tel:{{value}}
{{/tel}}
{{#address}}
ADR;TYPE={{type}};LABEL=\"{{&label}}\"
 :{{pobox}};{{ext}};{{street}};{{locality}};{{region}};{{code}};{{country}}
{{/address}}
EMAIL:{{email}}
REV:{{rev}}
END:VCARD" record))