<template>
  <div>
    <v-layout row wrap>
      <v-flex xs12>
        <dashboard-series></dashboard-series>
      </v-flex>
      <v-flex xs12>
        <account-list></account-list>
      </v-flex>
    </v-layout>
  </div>
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';
  import DashboardSeries from '@/components/DashboardSeries'
  import AccountList from '@/components/AccountList'

  @Component({
    components: {
      'dashboard-series': DashboardSeries,
      'account-list': AccountList
    }
  })
  export default class Dashboard extends Vue {
    data() {
      return {}
    }

    mounted() {

    }

    move(routerPath) {
      alert(routerPath);
      if (routerPath) {
        this.$router.push(routerPath)
      }
    }
  }
</script>
