<template>
  <v-card>
    <v-card-title>
      Limit 이력
    </v-card-title>

    <v-data-table
      :pagination.sync="pagination"
      :headers="headers"
      :items="limits"
      :total-items="totalLimits"
      :rows-per-page-items="[5,10,15,25]"
      :loading="loading"
      class="elevation-1"
    >
      <template slot="items" slot-scope="props">
        <tr>
          <td class="text-xs-right">{{props.item.time}}</td>
          <td class="text-xs-right">{{ props.item.current }}</td>
          <td class="text-xs-right">{{ props.item.limitAmount }}</td>
          <td class="text-xs-right">{{ props.item.plan }}</td>
        </tr>
      </template>
      <template slot="no-data">
        <v-alert v-if="!loading" :value="true" color="error" icon="warning">
          Sorry, nothing to display here :(
        </v-alert>
      </template>
    </v-data-table>
  </v-card>
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';
  import moment from 'moment';
  @Component
  export default class LimitHistory extends Vue {
    @Prop(String) unit;
    @Prop(String) user;

    data() {
      return {
        limits: [],
        totalLimits: 0,
        pagination: {
          rowsPerPage: 10,
          page: 1
        },
        loading: true,
        search: '',
        headers: [
          {text: '발생시간', value: 'time'},
          {text: '발생 사용량', value: 'current'},
          {text: '한도', value: 'limitAmount'},
          {text: 'Plan', value: 'plan'}
        ]
      }
    }

    mounted() {
      //http://localhost:8080/meter/limits/search/findByUnitAndUser?user=tester@gmail.com&unit=analytics&sort=time,desc
    }

    @Watch('unit')
    @Watch('pagination', {deep: true})
    async load() {
      this.loading = true;
      var me = this;
      var size = this.pagination.rowsPerPage;
      var page = this.pagination.page - 1;
      me.limits = [];

      var url = 'meter/limits/search/findByUnitAndUser?user=' +
        encodeURIComponent(me.user) + '&unit=' + me.unit + '&sort=time,desc&page=' + page + '&size=' + size;

      console.log(url);
      var res = await axios.get(url);

      var limits = res.data._embedded.limits;
      if (limits && limits.length) {
        limits.forEach(function (limit, i) {
          me.limits.push({
            time: moment( limit.time).format("YYYY-MM-DD HH:mm:ss"),
            current: limit.current,
            limitAmount: limit.limitAmount,
            plan: limit.addonPlan || limit.basePlan
          })
        })
      }
      this.totalLimits = res.data.page.totalElements;
      this.loading = false;
    }
  }
</script>
