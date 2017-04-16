(ns invetica.uri.example
  (:require [invetica.uri :as uri]
            [clojure.spec :as s])
  (:import (java.net URI)))

(s/def ::endpoint
  string?)

(s/def ::api
  (s/keys :req-un [::endpoint]))

(s/def ::uri
  (s/or :absolute ::uri/absolute-uri
        :relative ::uri/relative-uri))

(s/fdef str->uri
  :args (s/cat :api ::api :path ::uri/path)
  :ret ::uri/absolute-uri)

(defn endpoint+path
  [api path]
  {:post [(uri? %)]}
  (URI. (str (:endpoint api) path)))
