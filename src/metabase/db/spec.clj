(ns metabase.db.spec
  "Functions for creating JDBC DB specs for a given engine.
  Only databases that are supported as application DBs should have functions in this namespace;
  otherwise, similar functions are only needed by drivers, and belong in those namespaces."
  (:require [clojure.string :as str]
            [medley.core :as m]
            [ring.util.codec :as codec]))

(defn h2
  "Create a Clojure JDBC database specification for a H2 database."
  [{:keys [db]
    :or   {db "h2.db"}
    :as   opts}]
  (merge {:classname   "org.h2.Driver"
          :subprotocol "h2"
          :subname     db}
         (dissoc opts :db)))

(defn- remove-required-keys [db-spec & more-keys]
  (apply dissoc db-spec (concat [:host :port :db :dbname :user :password :additional-options]
                                more-keys)))

(defn- purge-nil-values [m]
  (m/remove-kv (fn [k v] (or (nil? k) (nil? v))) m))

(defn- make-subname [host port db extra-connection-params]
  (let [query-params (codec/form-encode (purge-nil-values extra-connection-params))]
    (str "//" host ":" port "/" db (when-not (str/blank? query-params)
                                     (str "?" query-params)))))

(defn postgres
  "Create a Clojure JDBC database specification for a Postgres database."
  [{:keys [host port db]
    :or   {host "localhost", port 5432, db ""}
    :as   opts}]
  (let [extra-conn-params (-> opts
                              (remove-required-keys :sslfactory)
                              (merge {:OpenSourceSubProtocolOverride true}))]
    (merge {:classname   "org.postgresql.Driver"
            :subprotocol "postgresql"
            :subname     (make-subname host port db extra-conn-params)}
           (dissoc opts :host :port :db))))

(defn mysql
  "Create a Clojure JDBC database specification for a MySQL or MariaDB database."
  [{:keys [host port db]
    :or   {host "localhost", port 3306, db ""}
    :as   opts}]
  (let [extra-connection-params (remove-required-keys opts)]
    (merge {:classname   "org.mariadb.jdbc.Driver"
            :subprotocol "mysql"
            :subname     (make-subname host port db extra-connection-params)}
           (dissoc opts :host :port :db))))


;; !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
;; !!                                                                                                               !!
;; !!   Don't put database spec functions for new drivers in this namespace. These ones are only here because they  !!
;; !!  can also be used for the application DB in metabase.driver. Put functions like these for new drivers in the  !!
;; !!                                            driver namespace itself.                                           !!
;; !!                                                                                                               !!
;; !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
