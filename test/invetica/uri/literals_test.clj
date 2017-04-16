(ns invetica.uri.literals-test
  (:require [clojure.spec :as s]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [invetica.uri :as uri]
            [invetica.uri.literals]))

(defspec t-uri-roundtrip
  (prop/for-all [uri (s/gen ::uri/absolute-uri)]
    (= uri (read-string (pr-str uri)))))
