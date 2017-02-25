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

(def test-coding-datom (r/atom ""))
(def coding-datom (r/atom {}))
(def tocode-datom (r/atom ""))
(def response-datom (r/atom ""))

(defn binary-choice [coding-datom field label opt1 opt2]
  [:p (str label " ")
   [:label
    [:input {:type "radio"
             :name (str field)
             :value opt1
             :on-change #(swap! coding-datom assoc field opt1)}]
    (str " " opt1)] " | "
   [:label
    [:input {:type "radio"
             :name (str field)
             :value opt2
             :on-change #(swap! coding-datom assoc field opt2)}]
    (str " " opt2)]
   ])

(defn line-input-field [coding-datom field]
  [:p
   [:input {:type "text"
            :on-change #(swap! coding-datom assoc field (-> % .-target .-value))}]
   " "])

(def test-site "http://www.modsy.com")

(defn send-data! [coding-datom]
  (POST "/file"
        {:params @coding-datom
         :handler #(reset! response-datom (str %))
         :error-handler #(reset! response-datom (str "error: " %))}))

(defn test-input-component []
  [:div.row
   [:div.col-md-12
     [line-input-field test-coding-datom]
    [:p
     [:input.btn.btn-primary
      {:type :submit
       :on-click #(send-data! test-coding-datom)
       :value "send data"}]]
    [:p (str "response: " @response-datom)]
    [:p (str "coding-datom is: " @test-coding-datom)]]])



(defn input-component []
  [:div.row
   [:div.col-md-12
    [line-input-field coding-datom :test]
    [binary-choice coding-datom :q1 "question one" "a1" "a2"]
    [binary-choice coding-datom :q2 "question two" "b1" "b2"]
    ]])

(defn target-component [url]
  [:div.row
   [:div.col-md-12
    [:h2 "Here's your site to code!"]
    [:iframe
     {:src url
      :width "800"
      :height "400"}]
    [:p
     [:a {:href url :target "_blank"} "site doesn't show up correctly above? open in a new tab."]]]])

(defn coding-page []
  [:div.container
   [target-component test-site]
   [input-component]
   [:div.row>div.col-sm-12
    [:p (str (js->clj @coding-datom))]]])

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
