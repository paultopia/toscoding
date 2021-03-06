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

(def coding-datom (r/atom {}))
(def response-datom (r/atom ""))
(defonce current-site (r/atom ""))

(defn nav-link [uri title page collapsed?]
  [:li.nav-item
   {:class (when (= page (session/get :page)) "active")}
   [:a.nav-link
    {:href uri
     :on-click #(reset! collapsed? true)} title]])

(defn quit-link [current-site]
  [:li.nav-item
   [:a.nav-link
    {:href "#/quit"
     :on-click #(POST "/quit" {:params @current-site})} "QUIT"]])

(defn navbar []
  (let [collapsed? (r/atom true)]
    (fn []
      [:nav.navbar.navbar-dark.bg-primary
       [:button.navbar-toggler.hidden-sm-up
        {:on-click #(swap! collapsed? not)} "☰"]
       [:div.collapse.navbar-toggleable-xs
        (when-not @collapsed? {:class "in"})
        [:a.navbar-brand {:href "#/"} "Coding Contracts"]
        [:ul.nav.navbar-nav
         [nav-link "#/" "Instructions" :home collapsed?]
         [nav-link "#/coding" "Coding" :coding collapsed?]
         [quit-link current-site]]]])))

(defn quit-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:p "Good job!  If you decide you want to start coding again, "
      [:a {:href "/"} "go back home."]]
     ]]])

(defn home-page []
  [:div.container
   (when-let [docs (session/get :docs)]
     [:div.row>div.col-sm-12
      [:div {:dangerouslySetInnerHTML
             {:__html (md->html docs)}}]])])

(defn not-checker [coding-datom field]
  (let [answer (get @coding-datom field "not-found")]
    (cond
      (= answer true) false
      (= answer false) true
      :else nil)))

(defn binary-choice [coding-datom field lab]
  [:p (str lab " ") [:br]
   [:label
    [:input {:type "radio"
             :name (str field)
             :checked (get @coding-datom field nil)
             :value true
             :on-click #(swap! coding-datom assoc field true)}]
    " Yes. "] [:br]
   [:label
    [:input {:type "radio"
             :name (str field)
             :checked (not-checker coding-datom field)
             :value false
             :on-click #(swap! coding-datom assoc field false)}]
    " No. "]])

(defn bigtext [coding-datom field]
  [:p
   [:textarea {:rows "10"
               :cols "80"
               :value (get @coding-datom field nil)
               :on-change #(swap! coding-datom assoc field (-> % .-target .-value))}]])

(defn line-input-field [coding-datom field]
  [:p
   [:input {:type "text"
            :on-change #(swap! coding-datom assoc field (-> % .-target .-value))}]
   " "])

(defn send-data! [coding-datom]
  (do (POST "/file"
          {:params @coding-datom
           :handler #(reset! current-site %)
           :error-handler #(.log js/console (str "error: " %))})
      false))

(defn input-component [current-site]
  [:div.row
   [:div.col-md-12
    [:form {:id "coding-form"}
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
                     (send-data! coding-datom)
                     (reset! coding-datom {})
                     (.preventDefault %))
        :value "submit"}]]
    ]]])

(defn debug-component []
  [:div.row>div.col-sm-12
   [:p (str @coding-datom)]
   [:p (str @response-datom)]
   [:p (str @current-site)]])

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
   ;[debug-component]
   ])

(def pages
  {:home #'home-page
   :quit #'quit-page
   :coding #'coding-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/quit" []
  (session/put! :page :quit))

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
