(defproject appengine "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.0-SNAPSHOT"]
                 [inflections "0.2"]]
  :dev-dependencies [[lein-clojars "0.5.0-SNAPSHOT"]
                     [com.google.appengine/appengine-api-1.0-sdk "1.3.2"]
                     [com.google.appengine/appengine-api-labs "1.3.2"]
                     [com.google.appengine/appengine-api-stubs "1.3.2"]
                     [com.google.appengine/appengine-local-runtime "1.3.2"]
                     [swank-clojure "1.1.0"]]
  :repositories [["maven-gae-plugin" "http://maven-gae-plugin.googlecode.com/svn/repository"]])
