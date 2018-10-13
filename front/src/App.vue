<template>
  <div>
    <router-view></router-view>

    <!--글로벌 알림 컴포넌트-->
    <v-snackbar
      ref="snackbar"
      v-model="snackbar.trigger"
      right
      top
      :color="snackbar.context"
    >
      {{snackbar.text}}
      <v-btn
        flat
        @click="snackbar.trigger = false"
      >
        Close
      </v-btn>
    </v-snackbar>
  </div>
</template>
<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';

  @Component
  export default class App extends Vue {
    data() {
      return {
        snackbar: {
          top: true,
          right: true,
          timeout: 6000,
          trigger: false,
          mode: 'multi-line',
          context: 'info',
          text: ''
        },
      }
    }

    mounted() {
      //this.startSSE();
    }

    startSSE() {
      var me = this;
      me.evtSource = new EventSource(window.baseUrl + '/docker/emitter');

      me.evtSource.onmessage = function (e) {
        var parse = JSON.parse(e.data);
        console.log('message', JSON.parse(e.data));
        window.busVue.$emit('container', parse);
      };

      me.evtSource.onerror = function (e) {
        if (me.evtSource) {
          console.log("closing evtSource and reconnect");
          me.evtSource.close();
          me.startSSE();
        }
      }
    }

    info(msg) {
      this.snackbar.context = 'info';
      this.snackbar.text = msg;
      this.snackbar.trigger = true;
    }

    error(msg) {
      this.snackbar.context = 'error';
      this.snackbar.text = msg;
      this.snackbar.trigger = true;
    }

    warning(msg) {
      this.snackbar.context = 'warning';
      this.snackbar.text = msg;
      this.snackbar.trigger = true;
    }

    success(msg) {
      this.snackbar.context = 'cyan darken-2';
      this.snackbar.text = msg;
      this.snackbar.trigger = true;
    }
  }
</script>

<style lang="scss" rel="stylesheet/scss">
  .header-top-line {
    width: 100%;
    background: darkblue;
    height: 3px;
  }
</style>
