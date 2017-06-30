(ns invetica.uri
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as sgen]
   [clojure.string :as str])
  (:import
   (java.net Inet4Address Inet6Address InetAddress URI URL URISyntaxException)))

;; -----------------------------------------------------------------------------
;; Generator utils

(def ^:private some-string? (complement str/blank?))

(defn- maybe
  [k]
  (sgen/one-of [(sgen/return nil) k]))

(defn- some-string
  ([]  (some-string (sgen/string-alphanumeric)))
  ([k] (sgen/such-that some-string? k)))

;; -----------------------------------------------------------------------------
;; Absolute

(s/def ::absolute? boolean?)

;; -----------------------------------------------------------------------------
;; Schemes

(def ^:private schemes
  #{"file" "ftp" "gopher" "http" "https"})

(s/def ::scheme
  (s/with-gen string? #(sgen/elements schemes)))

;; -----------------------------------------------------------------------------
;; User info

(s/def ::user string?)
(s/def ::password string?)

(s/def ::user-info
  (s/with-gen string?
    (fn []
      (sgen/fmap #(str/join ":" %)
                 (sgen/vector
                  (sgen/not-empty (sgen/string-alphanumeric))
                  1 2)))))

;; -----------------------------------------------------------------------------
;; Ports

(s/def ::port
  (s/or :neg #{-1}
        :zero #{0}
        :allocatable (s/int-in 1 65536)))

;; -----------------------------------------------------------------------------
;; IP addresses

(s/def ::octet
  (s/with-gen integer?
    (fn [] (sgen/choose 0 255))))

(defn ipv4?
  [x]
  (instance? Inet4Address x))

(s/def ::ipv4
  (s/with-gen ipv4?
    (fn []
      (sgen/fmap #(InetAddress/getByAddress (byte-array 4 %))
                 (sgen/vector (s/gen ::octet) 4)))))

(defn ipv6?
  [x]
  (instance? Inet6Address x))

(s/def ::ipv6
  (s/with-gen ipv6?
    (fn []
      (sgen/fmap #(InetAddress/getByAddress (byte-array 16 %))
                 (sgen/vector (s/gen ::octet) 16)))))

;; -----------------------------------------------------------------------------
;; Hosts

(s/def ::domain-part
  (s/with-gen string?
    (fn []
      (sgen/fmap #(apply str %)
                 (sgen/tuple (sgen/char-alpha) (some-string))))))

(s/def ::hostname
  (s/with-gen string?
    (fn []
      (sgen/fmap #(str/join "." %)
                 (sgen/not-empty (sgen/vector (s/gen ::domain-part)))))))

(s/def ::host
  (s/with-gen string?
    (fn []
      (sgen/one-of [(sgen/fmap #(.getHostAddress ^Inet4Address %)
                               (s/gen ::ipv4))
                    (s/gen ::hostname)
                    (sgen/fmap #(str "[" (.getHostAddress ^Inet6Address %) "]")
                               (s/gen ::ipv6))]))))

;; -----------------------------------------------------------------------------
;; Paths

(s/def ::path
  (s/with-gen string?
    (fn []
      (sgen/fmap (fn [[parts ext]]
                   (str "/" (str/join "/" parts) (some-> ext (str "." ext))))
                 (sgen/tuple
                  (sgen/vector
                   (sgen/one-of (conj (repeatedly 100 some-string)
                                      (sgen/return ".."))))
                  (maybe (some-string)))))))

;; -----------------------------------------------------------------------------
;; Query string

(s/def ::query-string string?)

;; -----------------------------------------------------------------------------
;; Fragments

(s/def ::fragment (s/nilable string?))

;; -----------------------------------------------------------------------------
;; Parse or nil

(s/fdef try-parse
  :args (s/cat :s (s/nilable string?))
  :ret (s/nilable uri?)
  :fn #(or (some? (:args %)) (nil? (:ret %))))

(defn- ^URI try-parse
  [s]
  (try
    (some-> s URI.)
    (catch URISyntaxException _)))

;; -----------------------------------------------------------------------------
;; Absolute URIs

(s/def ::absolute-uri
  (s/with-gen uri?
    (fn []
      (sgen/fmap #(URI. ^String (:scheme %)
                        ^String (:host %)
                        ^String (:path %)
                        ^String (:fragment %))
                 (sgen/hash-map
                  :fragment (s/gen ::fragment)
                  :host (s/gen ::host)
                  :path (s/gen ::path)
                  :scheme (sgen/elements schemes))))))

(s/fdef absolute-uri-str?
  :args (s/cat :s (s/nilable string?))
  :ret boolean?)

(defn absolute-uri-str?
  "Returns true when `s` is a stringified, absolute URI (see `java.net.URI`'s
  `isAbsolute`)."
  [s]
  {:pre [(or (nil? s) (string? s))]}
  (boolean (some-> s try-parse .isAbsolute)))

(s/def ::absolute-uri-str
  (s/with-gen (s/and string? absolute-uri-str?)
    (fn []
      (sgen/fmap str (s/gen ::absolute-uri)))))

;; -----------------------------------------------------------------------------
;; Relative URIs

(s/def ::relative-uri
  (s/with-gen uri?
    (fn []
      (sgen/fmap #(URI. nil
                        ^String (:host %)
                        ^String (:path %)
                        ^String (:fragment %))
                 (sgen/hash-map
                  :fragment (s/gen ::fragment)
                  :host (s/gen ::host)
                  :path (s/gen ::path))))))

(s/fdef relative-uri-str?
  :args (s/cat :s (s/nilable string?))
  :ret boolean?)

(def relative-uri-str?
  "Complement of `invetica.uri/absolute-uri-str?`."
  (complement absolute-uri-str?))

(s/def ::relative-uri-str
  (s/with-gen (s/and string? relative-uri-str?)
    (fn []
      (sgen/fmap str (s/gen ::relative-uri)))))

;; -----------------------------------------------------------------------------
;; Manipulation

(s/def ::uri-map
  (s/keys :req [::absolute?
                ::path
                ::scheme]
          :opt [::fragment
                ::password
                ::port
                ::query-string
                ::user
                ::user-info]))

(defprotocol IParseURIs
  (parse [x]))

(defn- parse-user-info
  [s]
  (let [[user password] (str/split s #":" 2)]
    {::password password
     ::user user
     ::user-info s}))

(defn- parse*
  [^URI uri]
  (let [fragment     (.getFragment uri)
        port         (.getPort uri)
        query-string (.getQuery uri)
        user-info    (.getUserInfo uri)]
    (cond-> {::absolute? (.isAbsolute uri)
             ::host      (.getHost uri)
             ::path      (.getPath uri)
             ::scheme    (.getScheme uri)}
      (not= -1 port)       (assoc ::port port)
      (some? fragment)     (assoc ::fragment fragment)
      (some? query-string) (assoc ::query-string query-string)
      (some? user-info)    (merge (parse-user-info user-info)))))

(s/fdef parse-uri
  :args (s/cat :x (s/or :str string? :uri uri?))
  :ret (s/nilable ::uri-map))

(extend-protocol IParseURIs
  String
  (parse [x] (some-> x try-parse parse*))
  URI
  (parse [x] (parse* x))
  URL
  (parse [x] (-> x .toURI parse*)))

(defn- authority
  [{:keys [::user-info ::host ::port]}]
  (str (when user-info
         (str user-info "@"))
       host
       (when (and port (not= -1 port))
         (str ":" port))))

(s/fdef unparse
  :args (s/cat :m ::uri-map)
  :ret uri?)

(defn unparse
  [m]
  (URI. ^String (::scheme m)
        ^String (authority m)
        ^String (::path m)
        ^String (::query-string m)
        ^String (::fragment m)))
