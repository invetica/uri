(ns invetica.uri.literals
  (:import java.net.URI
           java.io.Writer))

(defn uri
  [s]
  {:pre [(string? s)]}
  (URI. s))

(defmethod print-method URI
  [^URI uri ^Writer w]
  (.write w (format "#invetica/uri \"%s\"" uri)))
