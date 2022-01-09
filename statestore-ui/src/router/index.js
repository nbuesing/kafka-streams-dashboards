import Vue from 'vue'
import Router from 'vue-router'
import routes from './routes'

Vue.use(Router)

let router = new Router({
  routes, // short for routes: routes
  linkActiveClass: 'nav-item active'
})

export default router
