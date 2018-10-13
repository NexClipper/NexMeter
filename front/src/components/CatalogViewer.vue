<template>
  <div>
    <v-layout v-if="catalog" row>
      <v-flex>
        <p class="subheading">Found Units</p>
        <div class="caption" v-for="unit in  catalog.units">
          {{unit.name}}
        </div>
      </v-flex>
      <v-flex>
        <p class="subheading">Found Plans</p>
        <div v-for="product in  catalog.products">
          <div class="caption" v-for="plan in  product.plans">
            {{plan.name}}
          </div>
        </div>
      </v-flex>
    </v-layout>

    <v-divider></v-divider>
    <br>
    <codemirror
      ref="info"
      :options="{
              theme: 'dracula',
              mode: 'yml',
              extraKeys: {'Ctrl-Space': 'autocomplete'},
              lineNumbers: true,
              lineWrapping: true,
              readOnly: true
            }"
      :value="dump">
    </codemirror>
  </div>
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';
  import moment from 'moment';

  var YAML = require('js-yaml');

  @Component
  export default class ContainerInspect extends Vue {
    @Prop(String) id;

    data() {
      return {
        loaded: false,
        catalog: null,
        dump: ''
      }
    }

    mounted() {
      this.load();
    }

    @Watch('id')
    async load() {
      var me = this;
      var date = new Date();
      var res = await axios.get('1.0/kb/catalog?requestedDate=' + moment(date).format('YYYY-MM-DD'));

      this.catalog = res.data[0];
      this.dump = YAML.dump(this.catalog, null, 2);
      this.$emit('load', this.catalog);

      $(this.$el).find('.CodeMirror').height(500).css('font-size', '11px');
      this.loaded = true;
    }
  }
</script>
