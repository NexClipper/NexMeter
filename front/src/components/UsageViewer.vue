<template>
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
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';
  import moment from 'moment';

  var YAML = require('js-yaml');

  @Component
  export default class UsageViewer extends Vue {
    @Prop(Object) usage;

    data() {
      return {
        dump: ''
      }
    }

    mounted() {
      this.make();
    }

    make() {
      var me = this;
      this.dump = YAML.dump(this.usage, null, 2);
      $(this.$el).find('.CodeMirror').height(500).css('font-size', '11px');
    }
  }
</script>
