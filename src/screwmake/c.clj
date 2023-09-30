(ns screwmake.c 
  (:refer-clojure :exclude [compile])
  (:require
    [babashka.process :as p]
    [screwmake.util :as util]
    [babashka.fs :as fs]))

(defn- config-paths [{target :target dirs :dirs :as conf} name root]
  (let [out' (util/fileify root (:out dirs) name)]
    (-> conf
        (assoc :target (util/fileify out' "bin" target))
        (assoc-in [:dirs :out] out')
        (assoc-in [:dirs :src]
                  (util/fileify root
                                (:src dirs))))))

(defn compile [{cc :cc
                cflags :cflags}
               sources
               outs]
  (dorun
    (map (fn [[fst snd]]
           (apply p/shell
                  (flatten (list cc "-c" cflags fst "-o" snd))))
         (zipmap sources
                 outs))))

(defn link [{cc :cc
             ldflags :ldflags
             target :target}
            objs]
  (apply p/shell
         (flatten (list cc ldflags objs "-o" target))))

(defn build
  "Expects `conf` to be verified by the `make-config` function."
  [{{out :out src :src} :dirs
    :as conf}]
  (let [obj-dir (util/fileify out "obj")
        sources (util/get-source-files src "c")
        outs (map #(str (->> src
                            count
                            inc
                            (subs %)
                            fs/split-ext
                            first
                            (util/fileify obj-dir))
                        ".o")
                  sources)]
    (apply util/create-dirs (flatten [obj-dir (util/fileify out "bin") (map fs/parent outs)]))
    (compile conf sources outs)
    (link conf outs)
    (:target conf)))

(defn make-config [{target :target
                    cc :cc
                    ldflags :ldflags
                    cflags :cflags
                    {out :out
                     src :src} :dirs
                    :as conf}
                   name
                   root]
  (cond-> conf
    (nil? cc) (assoc :cc "gcc")
    (nil? ldflags) (assoc :ldflags [])
    (nil? cflags) (assoc :cflags [])
    (nil? out) (assoc-in [:dirs :out] "./out")
    (nil? src) (assoc-in [:dirs :src] "./src")
    (nil? target) (assoc :target name)
    true (config-paths name root)))

(comment
  (let [conf (make-config {:cc "clangd"
                           :cflags ["-Wall", "-g"]
                           :dirs {:out "./build"
                                  :src "./app"}
                           :target "foobaz"}
                          "foobar"
                          "/home/m3/projects/foobar")]
    (build conf)))
