(ns invetica.uri.literals
  (:import
   (java.io Writer)
   (java.net URI)))

(defn uri
  [s]
  {:pre [(string? s)]}
  (URI. s))

(defmethod print-method URI
  [^URI uri ^Writer w]
  (.write w (format "#invetica/uri \"%s\"" uri)))
