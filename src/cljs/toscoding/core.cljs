(ns toscoding.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [toscoding.ajax :refer [load-interceptors!]]
            [ajax.core :refer [GET POST]])
  (:import goog.History))

(defn nav-link [uri title page collapsed?]
  [:li.nav-item
   {:class (when (= page (session/get :page)) "active")}
   [:a.nav-link
    {:href uri
     :on-click #(reset! collapsed? true)} title]])

(defn navbar []
  (let [collapsed? (r/atom true)]
    (fn []
      [:nav.navbar.navbar-dark.bg-primary
       [:button.navbar-toggler.hidden-sm-up
        {:on-click #(swap! collapsed? not)} "☰"]
       [:div.collapse.navbar-toggleable-xs
        (when-not @collapsed? {:class "in"})
        [:a.navbar-brand {:href "#/"} "toscoding"]
        [:ul.nav.navbar-nav
         [nav-link "#/" "Home" :home collapsed?]
         [nav-link "#/about" "About" :about collapsed?]
         [nav-link "#/coding" "Coding" :coding collapsed?]
         ]]])))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src (str js/context "/img/warning_clojure.png")}]]]])

(defn home-page []
  [:div.container
   (when-let [docs (session/get :docs)]
     [:div.row>div.col-sm-12
      [:div {:dangerouslySetInnerHTML
             {:__html (md->html docs)}}]])])

;; most of the stuff up there is just template-generated boilerplate that I can't bother to get rid of.

;; data atoms. I'm going to send all data as simple strings, and then let them decode as json or whatever on either end, because this is FAST DEPLOY TIME.

(def coding-datom (r/atom ""))
(def tocode-datom (r/atom ""))
(def response-datom (r/atom ""))

(defn input-field [coding-datom]
  [:input {:type "text"
           :value @coding-datom
           :on-change #(reset! coding-datom (-> % .-target .-value))}])

(def test-site "http://www.google.com")

(defn send-data! [coding-datom]
  (POST "/file"
        {:params @coding-datom
         :handler #(reset! response-datom (str %))
         :error-handler #(reset! response-datom (str "error: " %))}))

(defn test-input-component []
  [:div.col-md-12
   [:p
    [input-field coding-datom] " "]
   [:p
    [:input.btn.btn-primary
     {:type :submit
      :on-click #(send-data! coding-datom)
      :value "send data"}]]
   [:p (str "response: " @response-datom)]
   [:p (str "coding-datom is: " @coding-datom)]])

(defn coding-page []
  [:div.container
   [:div.row
    [test-input-component]
    ]])

(def pages
  {:home #'home-page
   :about #'about-page
   :coding #'coding-page
   })

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/about" []
  (session/put! :page :about))

(secretary/defroute "/coding" []
  (session/put! :page :coding))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          HistoryEventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET "/docs" {:handler #(session/put! :docs %)}))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
