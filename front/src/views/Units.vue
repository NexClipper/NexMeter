<template>
  <div>
    <v-layout row wrap>
      <v-flex xs12>
        <unit-list></unit-list>
      </v-flex>
    </v-layout>
  </div>
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';
  import UnitList from '@/components/UnitList'

  @Component({
    components: {
      'unit-list': UnitList
    }
  })
  export default class Units extends Vue {
    data() {
      return {}
    }

    mounted() {

    }
  }
</script>
