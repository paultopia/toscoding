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


(def coding-datom (r/atom {}))
(def response-datom (r/atom ""))
(defonce current-site (r/atom ""))


(defn binary-choice [coding-datom field label]
  [:p (str label " ") [:br]
   [:label
    [:input {:type "radio"
             :name (str field)
             :value true
             :on-change #(swap! coding-datom assoc field true)}]
    " Yes. "] [:br]
   [:label
    [:input {:type "radio"
             :name (str field)
             :value false
             :on-change #(swap! coding-datom assoc field false)}]
    " No. "]
   ])

(defn bigtext [coding-datom field]
  [:p
   [:textarea {:rows "10"
               :cols "80"
               :on-change #(swap! coding-datom assoc field (-> % .-target .-value))}
    ""]])

(defn line-input-field [coding-datom field]
  [:p
   [:input {:type "text"
            :on-change #(swap! coding-datom assoc field (-> % .-target .-value))}]
   " "])


(defn send-data! [coding-datom]
  (POST "/file"
        {:params @coding-datom
         :handler #(reset! current-site %)
         :error-handler #(reset! response-datom (str "error: " %))}))


(defn input-component [current-site]
  [:div.row
   [:div.col-md-12
    [binary-choice coding-datom :found-k "Could you find a terms of service contract? "]
    [:p "If not, just scroll to the bottom and hit submit. Otherwise, answer the questions below."]
    [:h3 "Procedural provisions"]
    [binary-choice coding-datom :arbitration "Is there a compelled arbitration clause? "]
    [binary-choice coding-datom :choice-of-law "Is there a choice of law clause? "]
    [binary-choice coding-datom :class-action-wavier "Do users waive class actions? "]
    [:h3 "Substantive disadvantages"]
    [binary-choice coding-datom :waive-warranties "Do users waive warranties? "]
    [binary-choice coding-datom :assume-risk "Do users assume the risk of negligence? "]
    [binary-choice coding-datom :indemnification "Do users indemnify site against claims? "]
    [:h3 "Property-like rights"]
    [binary-choice coding-datom :ip-in-personal-data "Do users grant IP rights in personal data other than for providing site services? "]
    [binary-choice coding-datom :license-not-sale "Does transaction claim to be a \"license\" rather than a \"sale\"? "]
    [:h3 "User options"]
    [binary-choice coding-datom :snail-mail "Must user use physical mail to exercise some right? "]
    [binary-choice coding-datom :opt-out "Does contract provide a way to opt-out of some term? "]
    [:h3 "Copy and paste the contract below"]
    [bigtext coding-datom :contract]
    [:p
     [:input.btn.btn-primary
      {:type :submit
       :on-click #(do
                    (swap! coding-datom assoc :url @current-site)
                    (send-data! coding-datom))
       :value "submit"}]]
    ]])

(defn target-component [current-site]
  [:div.row
   [:div.col-md-12
    [:h2 "Here's your site to code!"]
    [:iframe
     {:src @current-site
      :width "800"
      :height "400"}]
    [:p
     [:a {:href @current-site :target "_blank"} "site doesn't show up correctly above? open in a new tab."]]]])

(defn coding-page []
  [:div.container
   [target-component current-site]
   [input-component current-site]
   [:div.row>div.col-sm-12
    [:p (str "response: " @response-datom)]
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

(defn initialize-current-site! []
  (GET "/init"
        {:handler #(reset! current-site %)
         :error-handler #(reset! response-datom (str "error: " %))}))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (load-interceptors!)
  (initialize-current-site!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
