<template>
  <v-card v-if="unit">
    <v-card-title>
      <v-layout row>
        Rules
        <v-spacer></v-spacer>
        <v-btn small outline color="deep-orange" @click="addItem()">+ Add Rule</v-btn>
      </v-layout>
    </v-card-title>
    <v-data-table
      :headers="headers"
      :items="unit.rules"
      hide-actions
      class="elevation-1"
    >
      <template slot="items" slot-scope="props">
        <td>{{ props.item.applyPlan ? '과금자 대상' : '비 과금자 대상' }}</td>
        <td>
              <span v-if="props.item.countingMethod == 'SUM'">
                합산 과금
              </span>
          <span v-else>
                {{props.item.periodSplitting == 'HOUR' ? '매 시간' : '매일'}}
                {{props.item.countingMethod == 'AVG' ? '평균치' : '최대치'}} 합산 과금
              </span>
        </td>
        <td>
              <span v-if="props.item.countingMethod == 'SUM'">
                {{props.item.limitRefreshInterval == 'HOUR' ? '시간 당' : '하루 당'}}
              </span>
          {{props.item.limitAmount ? props.item.limitAmount : 0 }} 한도
        </td>
        <td>
          {{props.item.freePeriod == 'HOUR' ? '시간 당' : '하루 당'}}
          {{props.item.freeAmount ? props.item.freeAmount : 0 }} 무료
        </td>
        <td>{{props.item.basePlan}}</td>
        <td>{{props.item.addonPlan}}</td>
        <td class="justify-center layout px-0">
          <v-icon
            small
            class="mr-2"
            @click="editItem(props.index)"
          >
            edit
          </v-icon>
          <v-icon
            small
            @click="deleteItem(props.index)"
          >
            delete
          </v-icon>
        </td>
      </template>
    </v-data-table>

    <rule-edit :unit="unit" :unitId="unitId" v-on:updated="load" ref="rule-edit"></rule-edit>
    <confirm
      :title="'Rule 삭제 확인'"
      :content="'Rule 을 삭제합니다. 집계된 데이터는 사라지지 않습니다.'"
      :ok-text="'삭제'"
      :cancel-text="'취소'"
      ref="deleteDialog">
    </confirm>
  </v-card>
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';
  import RuleEdit from '@/components/RuleEdit'
  import Confirm from '@/components/Confirm'

  @Component({
    components: {
      'rule-edit': RuleEdit,
      'confirm': Confirm
    }
  })
  export default class RuleTable extends Vue {
    @Prop(Number) unitId;

    data() {
      return {
        unit: null,
        headers: [
          {text: '과금', value: 'applyPlan'},
          {text: 'Counting', value: 'countingMethod'},
          {text: 'Limit', value: 'limitAmount'},
          {text: 'Free', value: 'freeAmount'},
          {text: 'BasePlan', value: 'basePlan'},
          {text: 'AddonPlan', value: 'addonPlan'},
          {text: 'Action', value: 'action'}
        ]
      }
    }

    addItem() {
      this.$refs['rule-edit'].open();
    }

    editItem(ruleIndex) {
      this.$refs['rule-edit'].open(ruleIndex);
    }

    deleteItem() {

    }

    mounted() {
      this.load();
    }

    async load() {
      var me = this;
      var res = await axios.get('meter/units/' + me.unitId);
      this.unit = res.data;
    }

    //
    // deleteConfirm(unit) {
    //   var me = this;
    //   this.$refs['deleteDialog'].open(function (confirm) {
    //     if (confirm) {
    //       me.deleteItem(unit);
    //     }
    //   })
    // }
    //
    // async deleteItem(unit) {
    //   try {
    //     await axios.delete('meter/units/' + unit.id);
    //     this.$root.$children[0].info('deleted.');
    //     this.load();
    //   } catch (e) {
    //     console.log(e.response);
    //     this.$root.$children[0].error('failed.');
    //   }
    // }
  }
</script>
