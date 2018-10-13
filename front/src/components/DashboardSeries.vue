<template>
  <div>
    <v-card>
      <v-card-text>
        <v-layout row>
          Usage chart
          <v-spacer></v-spacer>
          <v-select
            :items="timeItems"
            label="Time Select"
            v-model="time"
          ></v-select>
        </v-layout>

        <v-divider></v-divider>
        <highcharts style="height: 200px" v-if="loaded" :options="options"></highcharts>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';
  import Highcharts from 'highcharts';
  import moment from 'moment';

  Highcharts.setOptions({
    global: {
      useUTC: false
    }
  });

  @Component
  export default class DashboardSereis extends Vue {
    @Prop(String) unit;

    data() {
      var options = {
        exporting: {enabled: false},
        title: {
          text: null
        },
        chart: {
          zoomType: 'x'
        },
        xAxis: {
          type: 'datetime'
        },
        legend: {
          enabled: false
        }
      };
      return {
        loaded: false,
        options: options,
        timeItems: [
          {text: 'Last 3 Days', value: '3days'},
          {text: 'Last Week', value: 'week'},
          {text: 'Last Month', value: 'month'}
        ],
        time: '3days'
      }
    }

    mounted() {
      var me = this;
      me.load();
    }

    @Watch('time')
    async load() {
      var me = this;
      var date = new Date();
      var end = moment(date).format('YYYY-MM-DD HH');
      var start;
      if (me.time == '3days') {
        start = moment(date).subtract(3, 'days').format('YYYY-MM-DD HH');
      }
      if (me.time == 'week') {
        start = moment(date).subtract(1, 'weeks').format('YYYY-MM-DD HH');
      }
      if (me.time == 'month') {
        start = moment(date).subtract(1, 'months').format('YYYY-MM-DD HH');
      }

      me.loaded = false;

      var url = 'meter/record/series?start=' + start + '&end=' + end;
      console.log('url', url);
      if (me.unit) {
        url = url + '&unit=' + me.unit;
      }
      var res = await
        axios.get(url);

      if (!res.data || !res.data.length) {
        return;
      }

      var yAxis = [];
      var series = [];
      res.data.forEach(function (data, i) {
        var unitRule = data.unitRule;
        var countingMethod = unitRule.countingMethod;
        yAxis.push({ // Primary yAxis
          labels: {
            format: '{value}',
            style: {
              color: Highcharts.getOptions().colors[i]
            }
          },
          title: {
            text: unitRule.name + ' (' + countingMethod + ')',
            style: {
              color: Highcharts.getOptions().colors[i]
            }
          },
          opposite: true
        });
        series.push({
          type: 'area',
          name: unitRule.name,
          yAxis: i,
          data: data.series
        });
      });
      me.options.yAxis = yAxis;
      me.options.series = series;
      me.loaded = true;
    }
  }
</script>
