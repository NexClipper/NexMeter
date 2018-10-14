<template>
  <div>
    <v-layout row>
      <div>
        <div class="subheading"><span class="caption">Account:</span> {{externalKey}}</div>
        <div class="subheading"><span class="caption">Active Subscribing:</span> {{activePlans}}</div>
      </div>
      <v-spacer></v-spacer>
      <v-select
        :items="units"
        label="Select Unit"
        v-model="selectedUnit"
      ></v-select>
    </v-layout>
    <v-layout row>
      <v-flex xs12>
        <user-series v-if="selectedUnit" :unit="selectedUnit" :user="externalKey"></user-series>
      </v-flex>
    </v-layout>
    <v-layout row>
      <v-flex xs6>
        <limit-viewer v-if="selectedUnit" :unit="selectedUnit" :user="externalKey"></limit-viewer>
      </v-flex>
      <v-flex xs6>
        <limit-history v-if="selectedUnit" :unit="selectedUnit" :user="externalKey"></limit-history>
      </v-flex>
    </v-layout>

  </div>
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';
  import UserSeries from '@/components/UserSeries'
  import LimitViewer from '@/components/LimitViewer'
  import LimitHistory from '@/components/LimitHistory'

  @Component({
    components: {
      'user-series': UserSeries,
      'limit-viewer': LimitViewer,
      'limit-history': LimitHistory
    }
  })
  export default class Units extends Vue {
    @Prop(String) externalKey;

    data() {
      return {
        selectedUnit: null,
        units: [],
        account: null,
        activePlans: null
      }
    }

    mounted() {
      this.loadUnit();
      this.loadAccountSubscriptions();
    }

    async loadUnit() {
      var res = await axios.get('meter/units');
      var units = [];
      res.data._embedded.units.forEach(function (unit, i) {
        units.push(unit.name);
      });
      this.units = units;
      this.selectedUnit = this.units[0];
    }

    async loadAccountSubscriptions() {
      var me = this;
      me.activePlans = 'None';
      var res = await axios.get('meter/billing/subscriptions?user=' + encodeURIComponent(this.externalKey));
      if (res.data.subscriptions.length) {
        var plans = [];
        res.data.subscriptions.forEach(function (subscription, i) {
          plans.push(subscription.plan);
        });
        me.activePlans = plans.join();
      }

      //TODO add kaui link
      //http://localhost:9090/kaui/accounts/24450a04-9e19-48c5-aefe-c416a26d9798
      //this.account = res.data;


      //TODO get next invoice date
      // # Fetched asynchronously, as it takes time. This also helps with enforcing permissions.
      //     def next_invoice_date
      //   next_invoice = Kaui::Invoice.trigger_invoice_dry_run(params.require(:account_id), nil, true, options_for_klient)
      //   render :json => next_invoice ? next_invoice.target_date.to_json : nil
      //   end
    }
  }
</script>
