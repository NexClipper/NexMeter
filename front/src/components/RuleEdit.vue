<template>

  <v-layout row justify-center>
    <v-dialog v-model="dialog" fullscreen hide-overlay transition="dialog-bottom-transition">
      <v-card>
        <v-toolbar dark color="primary">
          <v-btn icon dark @click.native="dialog = false">
            <v-icon>close</v-icon>
          </v-btn>
          <v-toolbar-title>Rule Setting</v-toolbar-title>
          <v-spacer></v-spacer>
          <v-toolbar-items>
            <v-btn dark flat @click.native="save">Save</v-btn>
          </v-toolbar-items>
        </v-toolbar>

        <v-layout>
          <v-flex xs6>
            <v-form ref="form" v-model="valid" style="padding: 0px 32px 32px 32px">
              <v-checkbox
                value
                v-model="rule.applyPlan"
                label="과금자 대상 사용량 규칙"
                :rules="[rules.applyPlanCheck]"
              >
              </v-checkbox>

              <div v-if="rule.applyPlan">
                <div class="caption">과금 플랜</div>
                <v-text-field
                  v-model="rule.basePlan"
                  label="기본 플랜"
                  clearable
                  required
                  :rules="[rules.required]"
                ></v-text-field>
                <v-text-field
                  v-model="rule.addonPlan"
                  label="부가서비스 플랜"
                  clearable
                  :rules="[rules.addonPlan]"
                ></v-text-field>
              </div>


              <div v-if="rule.countingMethod != 'SUM'">매 {{rule.periodSplitting == 'HOUR' ? '시간' : '일'}}
                {{rule.countingMethod == 'AVG' ? '평균' : '최대치'}}
                사용량을 합산하여 청구
              </div>
              <v-layout row>
                <v-flex>
                  <v-text-field
                    v-model="rule.countingMethod"
                    label="집계 방식"
                    readonly
                  ></v-text-field>
                </v-flex>
                <v-flex v-if="rule.countingMethod != 'SUM'">
                  <v-select
                    :items="['HOUR','DAY']"
                    label="집계 주기"
                    v-model="rule.periodSplitting"
                    required
                  ></v-select>
                </v-flex>
                <v-flex v-if="rule.countingMethod != 'SUM'">
                  <v-checkbox
                    value
                    v-model="rule.putEmptyPeriod"
                    label="누락된 집계 구간 자동 채움"
                  >
                  </v-checkbox>
                </v-flex>
              </v-layout>

              <v-layout row>
                <v-flex>
                  <v-text-field
                    v-model.number="rule.limitAmount"
                    type="number"
                    label="사용량 한도"
                  ></v-text-field>
                </v-flex>
                <v-flex v-if="rule.countingMethod == 'SUM' && rule.limitAmount != null">
                  <v-select
                    :items="['HOUR','DAY']"
                    label="사용량 한도 초기화 주기"
                    v-model="rule.limitRefreshInterval"
                    :rules="[rules.limitRefreshInterval]"
                  ></v-select>
                </v-flex>
              </v-layout>

              <v-layout row>
                <v-flex>
                  <v-text-field
                    v-model.number="rule.freeAmount"
                    type="number"
                    label="무료 사용량"
                  ></v-text-field>
                </v-flex>
                <v-flex v-if="rule.freeAmount != null">
                  <v-select
                    :items="rule.countingMethod == 'SUM' ? ['HOUR','DAY'] : [rule.periodSplitting]"
                    label="무료 사용량 적용 주기"
                    v-model="rule.freePeriod"
                    :rules="[rules.freePeriod]"
                  ></v-select>
                </v-flex>
              </v-layout>

            </v-form>
          </v-flex>
          <v-flex xs6>
            <br>
            <catalog-viewer v-if="dialog"></catalog-viewer>
          </v-flex>
        </v-layout>
      </v-card>
    </v-dialog>
  </v-layout>
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';
  import CatalogViewer from '@/components/CatalogViewer'

  @Component({
    components: {
      'catalog-viewer': CatalogViewer
    }
  })
  export default class RuleEdit extends Vue {

    @Prop(Object) unit;
    @Prop(Number) unitId;

    data() {
      var me = this;
      return {
        dialog: false,
        notifications: false,
        sound: true,
        widgets: false,

        //rule index
        ruleIndex: -1,

        //init rule
        rule: this.emptyRule(),

        //validator
        valid: false,
        rules: {
          applyPlanCheck: function (value) {
            //false 일 경우, 자신을 제외한 다른 룰 중 디폴트가 있으면 안됨.
            if (!value) {
              var exist = false;
              me.unit.rules.forEach(function (rule, i) {
                if (me.ruleIndex != i && !rule.applyPlan) {
                  exist = true;
                }
              });
              return exist ? '비 과금자 사용량 규칙은 unit 당 하나만 지정 할 수 있습니다.' : true;
            } else {
              return true;
            }
          },
          addonPlan: function (value) {
            //부가서비스 플랜을 설정할 경우 기본플랜이 필요
            console.log('addonPlan', value, me.rule.basePlan);
            if (value && !me.rule.basePlan) {
              return '부가서비스 플랜은 기본 플랜이 필요합니다.'
            }
            return true;
          },
          limitRefreshInterval: function (value) {
            //limitRefreshInterval 이 지정된 경우 limitAmount 가 필요
            if (me.rule.limitAmount >= 0 && !value) {
              return '주기 설정이 필요합니다.'
            }
            return true;
          },
          freePeriod: function (value) {
            //freePeriod 이 지정된 경우 freeAmount 가 필요
            if (me.rule.freeAmount >= 0 && !value) {
              return '주기 설정이 필요합니다.'
            }
            return true;
          },
          required: value => !!value || 'Required.',
          counter: value => value && value.length <= 20 || 'Max 20 characters'
        }
      }
    }

    @Watch('rule', {deep: true})
    hideField() {
      if (typeof this.rule.limitAmount != 'number') {
        this.rule.limitAmount = null;
      }
      if (this.rule.limitAmount == null) {
        this.rule.limitRefreshInterval = null;
      }

      if (typeof this.rule.freeAmount != 'number') {
        this.rule.freeAmount = null;
      }
      if (this.rule.freeAmount == null) {
        this.rule.freePeriod = null;
      }
      this.$refs['form'].validate();
    }

    emptyRule() {
      return {
        applyPlan: true,
        basePlan: null,
        addonPlan: null,
        countingMethod: this.unit.countingMethod,
        periodSplitting: this.unit.countingMethod != 'SUM' ? 'HOUR' : null,
        limitAmount: null,
        limitRefreshInterval: null,
        freeAmount: null,
        freePeriod: null,
        putEmptyPeriod: false
      }
    }

    async open(ruleIndex) {
      if (ruleIndex == null) {
        this.ruleIndex = -1;
        this.rule = this.emptyRule();
      } else {
        this.ruleIndex = ruleIndex;
        this.rule = JSON.parse(JSON.stringify(this.unit.rules[ruleIndex]));
      }
      this.dialog = true;
    }

    async save() {
      this.$refs['form'].validate();
      if (!this.valid) {
        return;
      }
      var me = this;
      try {
        var unit = JSON.parse(JSON.stringify(this.unit));
        //put rule
        if (this.ruleIndex > -1) {
          unit.rules[this.ruleIndex] = this.rule;
          console.log('put rule', unit);
          await axios.put('meter/units/' + this.unitId, unit);
        }
        //add new rule
        else {
          unit.rules.push(this.rule);
          console.log('add new rule', unit);
          await axios.put('meter/units/' + this.unitId, unit);
        }
        me.$root.$children[0].info('saved.');
        this.dialog = false;
        this.$emit('updated');
      } catch (e) {
        console.log(e.response);
        me.$root.$children[0].error('failed.');
      }
    }

    mounted() {

    }
  }
</script>
