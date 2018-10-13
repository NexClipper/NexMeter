// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vuetify from 'vuetify'

Vue.use(Vuetify);

import Highcharts from 'highcharts'
import exportingInit from 'highcharts/modules/exporting'
import stockInit from 'highcharts/modules/stock'
import mapInit from 'highcharts/modules/map'

exportingInit(Highcharts)
stockInit(Highcharts);
mapInit(Highcharts);

import HighchartsVue from 'highcharts-vue'

Vue.use(HighchartsVue);
import 'vuetify/dist/vuetify.min.css'

var VueCodeMirror = require('vue-codemirror-lite')
Vue.use(VueCodeMirror);

require('codemirror/mode/javascript/javascript');
require('codemirror/mode/yaml/yaml.js');
require('codemirror/mode/vue/vue');

require('codemirror/theme/dracula.css');
require('codemirror/addon/hint/show-hint.js');
require('codemirror/addon/hint/show-hint.css');
require('codemirror/addon/hint/javascript-hint.js');
import VueHighlightJS from '../node_modules/vue-highlight.js';
import 'highlight.js/styles/vs2015.css';

Vue.use(VueHighlightJS);
window.busVue = new Vue();


var VueD3 = require('vue-d3');
Vue.use(VueD3);


import App from './App'
import router from './router'

/* eslint-disable no-new */
new Vue({
  el: '#app',
  router,
  template: '<App/>',
  components: {App}
});
