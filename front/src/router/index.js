import Vue from 'vue'
import Router from 'vue-router'
import Home from '@/views/Home'
import Dashboard from '@/views/Dashboard'
import Units from '@/views/Units'
import Accounts from '@/views/Accounts'
import AccountsDetail from '@/views/AccountsDetail'

Vue.use(Router);

/**
 * axios & backendUrl
 */

var NODE_ENV = process.env.NODE_ENV;
if (NODE_ENV == 'development') {
  window.baseUrl = 'http://localhost:8080/';
} else {
  window.baseUrl = location.protocol + '//' + location.hostname + (location.port ? ':' + location.port : '');
}

const _axios = require('axios');
var axios = _axios.create({
  baseURL: window.baseUrl,
  timeout: 10000
});
window.axios = axios;


export default new Router({
  base: '/',
  routes: [
    {
      path: '/',
      redirect: '/dashboard',
      name: 'home',
      component: Home,
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: Dashboard,
        },
        {
          path: 'units',
          name: 'units',
          component: Units,
        },
        {
          path: 'accounts',
          name: 'accounts',
          component: Accounts,
        },
        {
          path: 'accounts/:externalKey',
          name: 'accountsDetail',
          component: AccountsDetail,
          props: function (route) {
            return {
              externalKey: route.params.externalKey
            }
          }
        }
      ]
    }
  ]
})
