(ns invetica.uri-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [invetica.test.spec :as test.spec]
            [invetica.uri :as sut]))

(deftest t-specs
  (test.spec/is-well-specified 'invetica.uri))

(defspec t-absolute-uri
  (prop/for-all [uri (s/gen ::sut/absolute-uri)]
    (and (sut/absolute-uri-str? (str uri))
         (not (sut/relative-uri-str? (str uri))))))

(defspec t-relative-uri
  (prop/for-all [uri (s/gen ::sut/relative-uri)]
    (and (not (sut/absolute-uri-str? (str uri)))
         (sut/relative-uri-str? (str uri)))))
