(defproject invetica/uri "0.4.0-SNAPSHOT"
  :description "Specs, generators, and common utilities for URIs."
  :url "https://github.com/invetica/uri"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]]
  :min-lein-version "2.5.0"
  :aliases {"lint" ["do" ["whitespace-linter"] ["eastwood"]]}
  :profiles
  {:dev {:dependencies [[invetica/spec "0.4.0"]
                        [org.clojure/test.check "0.10.0-alpha1"]
                        [org.clojure/tools.namespace "0.3.0-alpha4"]]
         :plugins [[jonase/eastwood "0.2.3"]
                   [listora/whitespace-linter "0.1.0"]]
         :source-paths ["dev"]}})
