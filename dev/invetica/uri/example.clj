(ns invetica.uri.example
  (:require
   [clojure.spec.alpha :as s]
   [invetica.uri :as uri])
  (:import
   (java.net URI)))

(s/def ::endpoint
  string?)

(s/def ::api
  (s/keys :req-un [::endpoint]))

(s/def ::uri
  (s/or :absolute ::uri/absolute-uri
        :relative ::uri/relative-uri))

(s/fdef endpoint+path
  :args (s/cat :api ::api :path ::uri/path)
  :ret ::uri/absolute-uri)

(defn endpoint+path
  [api path]
  {:post [(uri? %)]}
  (URI. (str (:endpoint api) path)))
