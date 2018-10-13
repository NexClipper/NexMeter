<template>
  <div>
    <v-card>
      <v-card-text>
        <v-layout row>

          <v-flex xs5>
            <v-menu
              :close-on-content-click="false"
              v-model="menu1"
              :nudge-right="40"
              lazy
              transition="scale-transition"
              offset-y
              full-width
              max-width="290px"
              min-width="290px"
            >
              <v-text-field
                slot="activator"
                v-model="startDateFormatted"
                label="Start"
                hint="MM/DD/YYYY format"
                persistent-hint
                prepend-icon="event"
                readonly
              ></v-text-field>
              <v-date-picker v-model="startDateFormatted" no-title @input="menu1 = false"></v-date-picker>
            </v-menu>
          </v-flex>

          <v-flex xs5>
            <v-menu
              :close-on-content-click="false"
              v-model="menu2"
              :nudge-right="40"
              lazy
              transition="scale-transition"
              offset-y
              full-width
              max-width="290px"
              min-width="290px"
            >
              <v-text-field
                slot="activator"
                v-model="endDateFormatted"
                label="End"
                hint="MM/DD/YYYY format"
                persistent-hint
                prepend-icon="event"
                readonly
              ></v-text-field>
              <v-date-picker v-model="endDateFormatted" no-title @input="menu2 = false"></v-date-picker>
            </v-menu>
          </v-flex>
          <v-flex xs2>
            <v-select
              :items="timeItems"
              label="Time Select"
              v-model="time"
            ></v-select>
          </v-flex>
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
  export default class UserSeries extends Vue {
    @Prop(String) unit;
    @Prop(String) user;

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
          {text: 'Next Invoice', value: 'next'},
          {text: 'Last 3 Days', value: '3days'},
          {text: 'Last Week', value: 'week'},
          {text: 'Last Month', value: 'month'},
        ],
        time: '3days',
        blockLoad: false,

        //target Invoice to show.
        targetInvoice: null,
        start: null,
        end: null,


        menu1: false,
        menu2: false
      }
    }

    // computed
    get startDateFormatted() {
      return this.formatDate(this.start)
    }

    set startDateFormatted(val) {
      return this.parseDate(val, 'start')
    }

    get endDateFormatted() {
      return this.formatDate(this.end)
    }

    set endDateFormatted(val) {
      return this.parseDate(val, 'end')
    }

    formatDate(date) {
      if (!date) return null;
      let formatted = moment(date).format('YYYY-MM-DD');
      return formatted;
    }

    parseDate(formatted, which) {
      if (!formatted) return null;
      if (which == 'start') {
        this.start = moment(formatted, 'YYYY-MM-DD').toDate();
      } else {
        this.end = moment(formatted, 'YYYY-MM-DD').toDate();
      }
    }

    mounted() {
      var me = this;
      me.setTime();
    }

    @Watch('time')
    setTime() {
      var me = this;
      var date = new Date();
      var end = moment(date).toDate();
      var start;
      if (me.time == '3days') {
        start = moment(date).subtract(3, 'days').toDate();
      }
      if (me.time == 'week') {
        start = moment(date).subtract(1, 'weeks').toDate();
      }
      if (me.time == 'month') {
        start = moment(date).subtract(1, 'months').toDate();
      }

      me.start = start;
      me.end = end;
      me.blockLoad = true;
    }

    @Watch('unit')
    @Watch('start')
    @Watch('end')
    async load() {
      if (this.blockLoad) {
        console.log('blockLoad');
        this.blockLoad = false;
        return;
      }
      var me = this;
      var end = moment(me.end).format('YYYY-MM-DD HH');
      var start = moment(me.start).format('YYYY-MM-DD HH');

      me.loaded = false;

      var url = 'meter/record/series?start=' + encodeURIComponent(start) +
        '&end=' + encodeURIComponent(end) + '&user=' + encodeURIComponent(this.user) + '&unit=' + this.unit;
      console.log('url', url);

      var res = await axios.get(url);

      if (!res.data || !res.data.length) {
        return;
      }

      var yAxis = {
        labels: {
          format: '{value}',
          style: {
            color: Highcharts.getOptions().colors[1]
          }
        },
        title: {
          text: this.unit,
          style: {
            color: Highcharts.getOptions().colors[1]
          }
        },
        opposite: true
      };
      var series = [];
      var usages = res.data[0].usages;
      usages.forEach(function (data, i) {
        console.log(data);
        var rule = data.rule;
        var countingMethod = rule.countingMethod;
        var label = 'Non-billing';
        if (rule.applyPlan) {
          label = rule.addonPlan ? rule.addonPlan : rule.basePlan;
        }
        series.push({
          type: 'area',
          name: label,
          //yAxis: i,
          color: Highcharts.getOptions().colors[i],
          data: data.series
        });
      });
      me.options.yAxis = yAxis;
      me.options.series = series;
      me.loaded = true;
    }
  }
</script>
