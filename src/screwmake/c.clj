(ns screwmake.c 
  (:refer-clojure :exclude [compile])
  (:require
    [babashka.process :as p]
    [screwmake.util :as util]
    [screwmake.builder :as b]
    [babashka.fs :as fs]))

(declare config-paths build)

(defrecord Config [target cc cflags ldflags dirs]
  b/Builder
    (run [config cmd args]
      (condp util/one-of cmd
        "build"        (build config)
        ["exec" "run"] (-> config
                           build
                           (list args)
                           flatten
                           (#(apply p/shell %)))
        "clean"        (fs/delete-tree (:out (:dirs config))))))

(defmethod b/make-config "C" [{config :config} name root]
  (apply ->Config
         (-> {:target name
              :cc "gcc"
              :cflags []
              :ldflags []
              :dirs {:out "./out"
                     :src "./src"}}
             (merge config)
             (config-paths name root)
             vals)))

(defn- config-paths [{target :target dirs :dirs :as conf} name root]
  (let [out' (util/fileify root (:out dirs) name)]
    (assoc conf :target (util/fileify out' "bin" target)
                :dirs {:out out'
                       :src (util/fileify root (:src dirs))})))

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
