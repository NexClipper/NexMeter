<template>
  <v-card>
    <v-card-title>
      <v-layout row>
        Units
        <v-spacer></v-spacer>
        <v-text-field
          v-model="search"
          append-icon="search"
          label="Search"
          single-line
          hide-details
        ></v-text-field>
        <v-btn small outline color="deep-orange" @click="addItem()">+ Add Unit</v-btn>
      </v-layout>
    </v-card-title>

    <v-data-table
      :pagination.sync="pagination"
      :headers="headers"
      :items="units"
      :search="search"
      class="elevation-1"
    >
      <template slot="items" slot-scope="props">
        <tr>
          <td @click="props.expanded = !props.expanded"><a>{{ props.item.name }}</a></td>
          <td @click="props.expanded = !props.expanded" class="text-xs-right">{{ props.item.ruleCount }}</td>
          <td @click="props.expanded = !props.expanded" class="text-xs-right">{{ props.item.plans }}</td>
          <td class="justify-center">
            <v-icon
              small
              class="mr-2"
              @click="editItem(props.item)"
            >
              edit
            </v-icon>
            <v-icon
              small
              @click="deleteConfirm(props.item)"
            >
              delete
            </v-icon>
          </td>
        </tr>
      </template>
      <template slot="expand" slot-scope="props">
        <dashboard-series :unit="props.item.name"></dashboard-series>
        <rule-table :unitId="props.item.id"></rule-table>
      </template>
      <template slot="no-data">
        <v-alert :value="true" color="error" icon="warning">
          Sorry, nothing to display here :(
        </v-alert>
      </template>
    </v-data-table>

    <unit-edit v-on:updated="load" ref="unit-edit"></unit-edit>
    <confirm
      :title="'Unit 삭제 확인'"
      :content="'Unit 을 삭제합니다. 집계된 데이터는 사라지지 않습니다.'"
      :ok-text="'삭제'"
      :cancel-text="'취소'"
      ref="deleteDialog">
    </confirm>
  </v-card>
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';
  import DashboardSeries from '@/components/DashboardSeries'
  import RuleTable from '@/components/RuleTable'
  import UnitEdit from '@/components/UnitEdit'
  import Confirm from '@/components/Confirm'

  @Component({
    components: {
      'dashboard-series': DashboardSeries,
      'rule-table': RuleTable,
      'unit-edit': UnitEdit,
      'confirm': Confirm
    }
  })
  export default class ContainerList extends Vue {
    data() {
      return {
        pagination: {
          rowsPerPage: 10
        },
        units: [],
        search: '',
        selected: [],
        headers: [
          {text: 'Name', value: 'name'},
          {text: 'Number of rules', value: 'ruleCount'},
          {text: 'Associated Plans', value: 'plans'},
          {text: 'Action', value: 'action'}
        ],

        dialog: false,
        notifications: false,
        sound: true,
        widgets: false
      }
    }

    mounted() {
      this.load();
    }

    async load() {
      var me = this;
      var res = await axios.get('meter/units');
      res.data._embedded.units.forEach(function (unit, i) {
        let split = unit._links.self.href.split('/');
        var id = split[split.length - 1];
        unit.id = parseInt(id);
        unit.ruleCount = unit.rules.length;

        unit.plans = '';
        if (unit.rules.length) {
          var plans = [];
          unit.rules.forEach(function (rule, i) {
            if (rule.basePlan && plans.indexOf(rule.basePlan) < 0) {
              plans.push(rule.basePlan);
            }
            if (rule.addonPlan && plans.indexOf(rule.addonPlan) < 0) {
              plans.push(rule.addonPlan);
            }
          });
          unit.plans = plans.join();
        }
      });
      this.units = res.data._embedded.units;
    }

    addItem() {
      this.$refs['unit-edit'].open();
    }

    editItem(unit) {
      this.$refs['unit-edit'].open(unit.id);
    }

    deleteConfirm(unit) {
      var me = this;
      this.$refs['deleteDialog'].open(function (confirm) {
        if (confirm) {
          me.deleteItem(unit);
        }
      })
    }

    async deleteItem(unit) {
      try {
        await axios.delete('meter/units/' + unit.id);
        this.$root.$children[0].info('deleted.');
        this.load();
      } catch (e) {
        console.log(e.response);
        this.$root.$children[0].error('failed.');
      }
    }

    move(id) {
      console.log(id);
      //this.$router.push('/containers/' + id + '/matrix');
    }
  }
</script>
