(ns hface.dash.refresh
    (:require [hface.stats :refer [os-mem-used 
                                   map-mem-used 
                                   node-total-memory]]
              [cognitect.transit :as t]
              [hface.tools :refer [info]]
              [goog.net.XhrIo :as xhr]))

(defonce refresh-interval 2000)

(defn refresh-stats [stats]
  (xhr/send "cluster-stats"
            (fn [event]
              (let [t-reader (t/reader :json)
                    res (-> event .-target .getResponseText)]
                (reset! stats (t/read t-reader res))))
            "GET"))

(defn refresh-cpu [stats chart]
  (when chart
    (let [cpu-usage (-> @stats :aggregated 
                               :top 
                               :os-process-cpu-load)]
      (if (and (> cpu-usage 0) 
               (<= cpu-usage 100))
        (.load chart (clj->js {:columns [["cpu usage" cpu-usage]]}))))))

(defn refresh-os-mem [stats chart]
  (when chart
    (let [mem-usage (-> @stats :aggregated 
                               :top
                               os-mem-used)]
      (.load chart (clj->js {:columns [["memory usage" mem-usage]]})))))

(defn refresh-mem [stats chart]
  (when chart
    (let [mem-usage (-> @stats :aggregated
                               :map-stats
                               vals
                               (map-mem-used (node-total-memory stats)))]
      (.load chart (clj->js {:columns [["memory usage" mem-usage]]})))))

(def s (atom -1)) ;; TODO: refactor to use real timeseries seconds vs. a dummy global sequence

(defn update-map-area [active-map stats chart] 
  (when (and chart @active-map @stats)
    (if-let [ms (-> @stats :aggregated :map-stats)]
      (let [m-stats (.get ms (keyword @active-map))]

        (.flow chart (clj->js {:columns [;;["x" (.getSeconds (js/Date.))]
                                         ["x" (swap! s #(+ % 2))]
                                         ["puts" (:put-rate m-stats)]
                                         ["hits" (:hit-rate m-stats)]
                                         ["gets" (:get-rate m-stats)]]
                               :duration 1500}))))))
