<template>
  <v-card v-if="limitData">
    <v-card-title>
      <v-layout row>
        Current Limit
        <v-spacer></v-spacer>
        <v-btn small outline color="deep-orange" @click="openReset()">Reset</v-btn>
      </v-layout>
    </v-card-title>
    <v-data-table
      :items="limitData"
      hide-actions
      hide-headers
      class="elevation-1"
    >
      <template slot="items" slot-scope="props">
        <td>{{ props.item.text}}</td>
        <td>{{ props.item.value}}</td>
      </template>
    </v-data-table>

    <v-dialog v-model="dialog" persistent max-width="500px">
      <v-card>
        <v-card-title>
          <span class="headline">Reset current limit</span>
        </v-card-title>
        <v-card-text>
          <v-flex xs12 sm6 md4>
            <v-text-field type="number" v-model="current"
                          label="Amount" required></v-text-field>
          </v-flex>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="blue darken-1" flat @click.native="dialog = false">Close</v-btn>
          <v-btn color="blue darken-1" flat @click.native="reset">Save</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';

  @Component
  export default class LimitViewer extends Vue {
    @Prop(String) unit;
    @Prop(String) user;

    data() {
      return {
        limit: {},
        limitData: [],
        interval: null,
        dialog: false,
        current: null
      }
    }

    mounted() {
      var me = this;
      me.interval = setInterval(me.load, 1000);
    }

    destroyed() {
      var me = this;
      clearInterval(me.interval);
    }

    @Watch('unit')
    @Watch('user')
    async load() {
      var me = this;
      var res = await axios.get('meter/limit/check/' + me.unit + '?user=' + encodeURIComponent(me.user));
      me.limit = res.data;
      var resetTime = (new Date(me.limit.reset)).toUTCString().match(/(\d\d:\d\d:\d\d)/)[0];

      me.limitData = [
        {text: 'Unit', value: me.unit},
        {text: '현재 사용량', value: me.limit.current},
        {text: '남은 사용량', value: me.limit.remaining},
        {text: '초기화 까지 남은 시간', value: resetTime}
      ]
    }

    openReset() {
      this.current = this.limit.current;
      this.dialog = true;
    }

    async reset() {
      var current = this.current;
      try {
        await axios.put('meter/limit/reset/' + this.unit + '?user=' + encodeURIComponent(this.user)
          + '&amount=' + current);
        this.$root.$children[0].info('updated.');
      } catch (e) {
        console.log(e.response);
        this.$root.$children[0].error('failed.');
      }

    }
  }
</script>
