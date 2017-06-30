(ns invetica.uri-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer :all]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.properties :as prop]
   [invetica.test.spec :as test.spec]
   [invetica.uri :as sut])
  (:import
   (java.net URI)))

(use-fixtures :once test.spec/instrument)

(deftest t-specs
  (test.spec/is-well-specified 'invetica.uri))

;; -----------------------------------------------------------------------------
;; Predicates

(defspec t-absolute-uri
  (prop/for-all [uri (s/gen ::sut/absolute-uri)]
    (and (sut/absolute-uri-str? (str uri))
         (not (sut/relative-uri-str? (str uri))))))

(defspec t-relative-uri
  (prop/for-all [uri (s/gen ::sut/relative-uri)]
    (and (not (sut/absolute-uri-str? (str uri)))
         (sut/relative-uri-str? (str uri)))))

;; -----------------------------------------------------------------------------
;; Manipulation

(deftest t-parse
  (is (= {::sut/absolute? true
          ::sut/fragment "frag"
          ::sut/host "www2.example.com"
          ::sut/password "pass"
          ::sut/path "/foo/bar/"
          ::sut/port 6789
          ::sut/query-string "a=1&a=2&b=3"
          ::sut/scheme "https"
          ::sut/user "user"
          ::sut/user-info "user:pass"}
         (sut/parse
          "https://user:pass@www2.example.com:6789/foo/bar/?a=1&a=2&b=3#frag")))
  (is (= {::sut/absolute? true
          ::sut/host "example.com"
          ::sut/path "/foo"
          ::sut/scheme "http"}
         (sut/parse "http://example.com/foo"))))

(deftest t-unparse
  (is (= (URI.
          "https://user:pass@www2.example.com:6789/foo/bar/?a=1&a=2&b=3#frag")
         (sut/unparse {::sut/absolute? true
                       ::sut/fragment "frag"
                       ::sut/host "www2.example.com"
                       ::sut/password "pass"
                       ::sut/path "/foo/bar/"
                       ::sut/port 6789
                       ::sut/query-string "a=1&a=2&b=3"
                       ::sut/scheme "https"
                       ::sut/user "user"
                       ::sut/user-info "user:pass"}))))

(defspec t-parse-unparse
  (prop/for-all [uri (s/gen ::sut/absolute-uri)]
    (= uri (-> uri sut/parse sut/unparse))))
