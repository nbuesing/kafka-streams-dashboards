import Vue from 'vue'
import Vuex from 'vuex'
import VueSSE from 'vue-sse';
// import Axios from "./plugins/axios";
import App from './App.vue'
import router from './router'

import VueMaterial from 'vue-material'
import 'vue-material/dist/vue-material.min.css'
import 'vue-material/dist/theme/default-dark.css'



Vue.config.productionTip = false

Vue.use(Vuex)
Vue.use(VueMaterial)
//Vue.use(Axios)
Vue.use(VueSSE);

// Vue.use(VueAxios, Axios)

const store = new Vuex.Store({
  state: {
    userId: null,
    name: null
  },
  getters: {
    getUserId() {
      return userId;
    },
    getName() {
      return name;
    }
  },
  mutations: {
    setUserId(userId) {
      this.userId = userId;
    },
    setName(name) {
      this.name = name;
    }
  }
})

new Vue({
  store,
  render: h => h(App),
  router
}).$mount('#app')
