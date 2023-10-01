(ns screwmake.app
  (:require
   [cheshire.core :as json]
   [screwmake.util :as util]
   [screwmake.c]
   [babashka.fs :as fs]
   [babashka.process :as p]
   [screwmake.builder :as b]))

(defn get-config [root]
  (let [path (util/fileify root "screwmake.json")]
    (if (fs/exists? path)
      (json/parse-string (slurp path) true)
      (do
        (println "Please create a config file")
        (System/exit 1)))))

(defn -main [project-name cmd & args]
  (let [root (util/get-root)
        config (get-config root)
        {cmds :commands
         :as project-config} ((keyword project-name) config)
        custom-cmd ((keyword cmd) cmds)
        builder-config (b/make-config project-config project-name root)]
    (when custom-cmd
          (apply p/shell custom-cmd)
          (System/exit 0))
    (b/run builder-config cmd args)))
  ; (let [root (util/get-root)
  ;       config (get-config root)
  ;       {lang :language
  ;        cmds :commands
  ;        :as project-config} ((keyword project-name) config)]
  ;   (condp = lang
  ;     "C" (if (contains? cmds cmd)
  ;           (apply p/shell ((keyword cmd) cmds))
  ;           (let [build-config (c/make-config (:build project-config)
  ;                                             project-name
  ;                                             root)]
  ;             (condp util/one-of cmd
  ;               "build"         (c/build build-config)
  ;               ["exec", "run"] (-> build-config
  ;                                  c/build
  ;                                  (list args)
  ;                                  flatten
  ;                                  (#(apply p/shell %)))
  ;               "clean"         (fs/delete-tree (:out (:dirs build-config))))))
  ;     (do
  ;       (println (str "Invalid language for project `" project-name "`"))
  ;       (System/exit 1)))))
