<template>
  <v-card>
    <v-data-table
      :headers="headers"
      :items="list"
      class="elevation-1"
    >
      <template slot="items" slot-scope="props">
        <tr @click="props.expanded = !props.expanded">
          <td>{{ !props.item.applyPlan ? '비 과금' : props.item.plan }}</td>
          <td>{{ props.item.subscriptionId }}</td>
          <td>{{ props.item.days }}</td>
          <td>{{ props.item.usage }}</td>
          <td>{{ props.item.free }}</td>
          <td>{{ props.item.total }}</td>
        </tr>
      </template>
      <template slot="expand" slot-scope="props">
        <v-card flat>
          <v-card-text>
            <usage-viewer :usage="props.item"></usage-viewer>
          </v-card-text>
        </v-card>
      </template>
      <template slot="no-data">
        <v-alert :value="true" color="error" icon="warning">
          Sorry, nothing to display here :(
        </v-alert>
      </template>
    </v-data-table>
  </v-card>
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';
  import Confirm from '@/components/Confirm'
  import UsageViewer from '@/components/UsageViewer'

  @Component({
    components: {
      'confirm': Confirm,
      'usage-viewer': UsageViewer
    }
  })
  export default class UsageTable extends Vue {
    @Prop(Array) usages;

    data() {
      return {
        list: [],
        headers: [
          {text: 'Plan', value: 'plan'},
          {text: 'subscriptionId', value: 'subscriptionId'},
          {text: '사용 일수', value: 'days'},
          {text: '총 사용량', value: 'usage'},
          {text: '무료 사용량', value: 'free'},
          {text: '청구량', value: 'total'}
        ]
      }
    }

    mounted() {
      this.make();
    }

    make() {
      //사용량 증가, 감소 일단 하지 말자. 하게 되면 필요한 것
      //subscriptionId
      //unit
      //user
      //amount
      //time [yyyy-MM-dd HH,yyyy-MM-dd HH]

      var me = this;
      me.list = [];
      me.usages.forEach(function (data, i) {
        if (data.amountTotal == null) {
          return;
        }
        var item = {
          amountTotal: data.amountTotal,
          amountPerDay: data.amountPerDay,
          amountPerHour: data.amountPerHour,
          id: i,
          applyPlan: data.rule.applyPlan,
          subscriptionId: data.subscriptionId,
          plan: data.rule.addonPlan || data.rule.basePlan,
          days: data.amountTotal.days,
          usage: data.amountTotal.usage,
          free: data.amountTotal.free,
          total: data.amountTotal.total
        };
        me.list.push(item);
      });
    }
  }
</script>
