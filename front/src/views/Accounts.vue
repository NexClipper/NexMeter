<template>
  <div>
    <v-layout row wrap>
      <v-flex xs12>
        <account-list></account-list>
      </v-flex>
    </v-layout>
  </div>
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';
  import AccountList from '@/components/AccountList'

  @Component({
    components: {
      'account-list': AccountList
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
